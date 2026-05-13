package com.yaoshizuting.service.impl;

import cn.hutool.core.util.StrUtil;
import com.yaoshizuting.dto.WalletResponse;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.enums.ProfitType;
import com.yaoshizuting.enums.UserRole;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.mapper.WithdrawalMapper;
import com.yaoshizuting.service.DistributedLockService;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.service.TeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitServiceImpl implements ProfitService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final ProfitLogMapper profitLogMapper;
    private final PolicyConfigService policyConfigService;
    private final DistributedLockService lockService;
    private final WithdrawalMapper withdrawalMapper;
    private final TeamService teamService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processJoinStoreProfit(Order order) {
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            log.warn("订单未支付，跳过分润: orderSn={}", order.getOrderSn());
            return;
        }

        if (!claimPaidOrder(order)) {
            return;
        }

        User newUser = userMapper.selectById(order.getUserId());
        if (newUser == null) {
            throw new BusinessException("用户不存在");
        }

        newUser.setRole(UserRole.STORE.getCode());
        userMapper.updateById(newUser);
        teamService.evictTeamTreeCaches(newUser);

        if (newUser.getParentId() != null && newUser.getParentId() > 0) {
            incrementStoreCounts(newUser);
            distributeStoreJoinProfit(order, newUser);
        }

        completeOrder(order);
        log.info("处理店铺加盟分润完成: orderSn={}, userId={}", order.getOrderSn(), order.getUserId());
    }

    private void distributeStoreJoinProfit(Order order, User newUser) {
        User parent = userMapper.selectById(newUser.getParentId());
        if (parent == null) {
            return;
        }

        BigDecimal directReward = isDirectStoreRewardEligible(parent) ? getDirectStoreReward(parent) : BigDecimal.ZERO;
        
        if (directReward.compareTo(BigDecimal.ZERO) > 0) {
            if (createProfitLog(order.getOrderSn(), parent.getId(), newUser.getId(), directReward,
                    ProfitType.DIRECT_STORE.getCode(), "直推店铺奖励")) {
                addBalance(parent.getId(), directReward);
            }
        }

        if (isIndirectStoreRewardEnabled()) {
            distributeIndirectReward(order.getOrderSn(), newUser);
        }
        distributeTeamManagementFee(order.getOrderSn(), newUser);
    }

    private boolean isDirectStoreRewardEligible(User parent) {
        int storeCount = parent.getStoreCount() != null ? parent.getStoreCount() : 0;
        int startCount = getConfigInt("STORE_DIRECT_REWARD_START_COUNT", 2);
        return storeCount >= startCount;
    }

    private boolean isIndirectStoreRewardEnabled() {
        return getConfigInt("STORE_INDIRECT_REWARD_ENABLED", 0) > 0;
    }

    private BigDecimal getDirectStoreReward(User parent) {
        if (parent.getRole() == UserRole.PARTNER.getCode()) {
            return policyConfigService.getConfigValue("PARTNER_REWARD_DIRECT");
        } else if (parent.getRole() == UserRole.AGENT.getCode()) {
            return policyConfigService.getConfigValue("AGENT_REWARD_DIRECT");
        } else {
            return policyConfigService.getConfigValue("STORE_REWARD_DIRECT");
        }
    }

    private void distributeIndirectReward(String orderSn, User newUser) {
        if (StrUtil.isBlank(newUser.getTreePath())) {
            return;
        }

        String[] pathIds = newUser.getTreePath().split("/");
        if (pathIds.length < 3) {
            return;
        }

        List<Long> ancestorIds = new ArrayList<>();
        for (int i = 1; i < pathIds.length - 1; i++) {
            try {
                ancestorIds.add(Long.parseLong(pathIds[i]));
            } catch (NumberFormatException ignored) {
            }
        }

        if (ancestorIds.isEmpty()) {
            return;
        }

        Long indirectParentId = ancestorIds.get(ancestorIds.size() - 1);
        User indirectParent = userMapper.selectById(indirectParentId);
        
        if (indirectParent != null && indirectParent.getId().longValue() != newUser.getParentId().longValue()) {
            BigDecimal indirectReward = policyConfigService.getConfigValue("REWARD_INDIRECT");
            if (createProfitLog(orderSn, indirectParent.getId(), newUser.getId(), indirectReward,
                    ProfitType.INDIRECT_STORE.getCode(), "间推店铺奖励")) {
                addBalance(indirectParent.getId(), indirectReward);
            }
        }
    }

    private void distributeTeamManagementFee(String orderSn, User newUser) {
        if (StrUtil.isBlank(newUser.getTreePath())) {
            return;
        }

        String[] pathIds = newUser.getTreePath().split("/");
        
        for (int i = 1; i < pathIds.length - 1; i++) {
            try {
                Long partnerId = Long.parseLong(pathIds[i]);
                User partner = userMapper.selectById(partnerId);
                
                if (partner != null && partner.getRole() == UserRole.PARTNER.getCode()) {
                    Integer storeCount = partner.getStoreCount() != null ? partner.getStoreCount() : 0;
                    int startCount = getConfigInt("PARTNER_TEAM_MANAGEMENT_START_COUNT", 2);
                    int endCount = getConfigInt("PARTNER_TEAM_MANAGEMENT_END_COUNT", 100);
                    
                    if (storeCount >= startCount && storeCount <= endCount) {
                        BigDecimal managementFee = policyConfigService.getConfigValue("PARTNER_TEAM_MANAGEMENT");
                        if (createProfitLog(orderSn, partnerId, newUser.getId(), managementFee,
                                ProfitType.TEAM_MANAGEMENT.getCode(), "团队管理津贴")) {
                            addBalance(partnerId, managementFee);
                        }
                    }
                    break;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processJoinAgentProfit(Order order) {
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            log.warn("订单未支付，跳过分润: orderSn={}", order.getOrderSn());
            return;
        }

        if (!claimPaidOrder(order)) {
            return;
        }

        User newAgent = userMapper.selectById(order.getUserId());
        if (newAgent == null) {
            throw new BusinessException("用户不存在");
        }

        newAgent.setRole(UserRole.AGENT.getCode());
        userMapper.updateById(newAgent);
        teamService.evictTeamTreeCaches(newAgent);

        if (newAgent.getParentId() != null && newAgent.getParentId() > 0) {
            distributeAgentJoinProfit(order, newAgent);
        }

        completeOrder(order);
        log.info("处理代理加盟分润完成: orderSn={}, userId={}", order.getOrderSn(), order.getUserId());
    }

    private void distributeAgentJoinProfit(Order order, User newAgent) {
        User parent = userMapper.selectById(newAgent.getParentId());
        if (parent == null) {
            return;
        }

        if (parent.getRole() == UserRole.PARTNER.getCode()) {
            processPartnerRecruitAgentProfit(parent, newAgent, order.getOrderSn());
            return;
        } else if (parent.getRole() == UserRole.AGENT.getCode() || parent.getRole() == UserRole.STORE.getCode()) {
            BigDecimal directReward = policyConfigService.getConfigValue("AGENT_REWARD_DIRECT_AGENT");
            if (directReward.compareTo(BigDecimal.ZERO) > 0
                    && createProfitLog(order.getOrderSn(), parent.getId(), newAgent.getId(), directReward,
                    ProfitType.AGENT_MANAGE.getCode(), "直推代理奖励")) {
                addBalance(parent.getId(), directReward);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processJoinPartnerProfit(Order order) {
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            log.warn("订单未支付，跳过分润: orderSn={}", order.getOrderSn());
            return;
        }

        if (!claimPaidOrder(order)) {
            return;
        }

        User newPartner = userMapper.selectById(order.getUserId());
        if (newPartner == null) {
            throw new BusinessException("用户不存在");
        }

        newPartner.setRole(UserRole.PARTNER.getCode());
        userMapper.updateById(newPartner);
        teamService.evictTeamTreeCaches(newPartner);

        if (newPartner.getParentId() != null && newPartner.getParentId() > 0) {
            distributePartnerJoinProfit(order, newPartner);
        }

        completeOrder(order);
        log.info("处理合伙人加盟分润完成: orderSn={}, userId={}", order.getOrderSn(), order.getUserId());
    }

    private void distributePartnerJoinProfit(Order order, User newPartner) {
        User parent = userMapper.selectById(newPartner.getParentId());
        if (parent == null) {
            return;
        }

        BigDecimal directReward = BigDecimal.ZERO;
        String description = "直推合伙人奖励";

        if (parent.getRole() == UserRole.PARTNER.getCode()) {
            directReward = policyConfigService.getConfigValue("PARTNER_REWARD_DIRECT_PARTNER");
        }

        if (directReward.compareTo(BigDecimal.ZERO) > 0) {
            if (createProfitLog(order.getOrderSn(), parent.getId(), newPartner.getId(), directReward,
                    ProfitType.PARTNER_DIRECT.getCode(), description)) {
                addBalance(parent.getId(), directReward);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processPartnerRecruitAgentProfit(User partner, User newAgent) {
        processPartnerRecruitAgentProfit(partner, newAgent, "AGENT-" + newAgent.getId());
    }

    private void processPartnerRecruitAgentProfit(User partner, User newAgent, String orderSn) {
        int currentCount = partner.getAgentCount() != null ? partner.getAgentCount() : 0;

        BigDecimal manageFee = policyConfigService.getConfigValue("PARTNER_MANAGE_FEE");
        if (createProfitLog(orderSn, partner.getId(), newAgent.getId(), manageFee,
                ProfitType.AGENT_MANAGE.getCode(), "代理商管理培训费")) {
            addBalance(partner.getId(), manageFee);
        }

        if (currentCount >= 10) {
            BigDecimal supportFee = policyConfigService.getConfigValue("HEADQUARTER_SUPPORT_FEE");
            if (createProfitLog(orderSn, partner.getId(), newAgent.getId(), supportFee.negate(),
                    ProfitType.HEADQUARTER_SUPPORT_FEE.getCode(), "总部培训支持费")) {
                addBalance(partner.getId(), supportFee.negate());
            }
        }

        incrementAgentCount(partner, currentCount);

        log.info("处理合伙人招募代理分润: partnerId={}, agentId={}, count={}", partner.getId(), newAgent.getId(), currentCount + 1);
    }

    private void incrementAgentCount(User partner, int currentCount) {
        int updated = userMapper.update(null, new UpdateWrapper<User>()
                .setSql("agent_count = COALESCE(agent_count, 0) + 1")
                .set("update_time", LocalDateTime.now())
                .eq("id", partner.getId())
                .eq("deleted", 0));

        if (updated > 0) {
            partner.setAgentCount(currentCount + 1);
            return;
        }

        log.warn("合伙人代理数更新失败: partnerId={}", partner.getId());
    }

    private boolean createProfitLog(String orderSnKey, Long receiverId, Long contributorId, BigDecimal amount,
                                  String type, String description) {
        String lockKey = "profit:lock:" + orderSnKey + ":" + type + ":" + receiverId;
        
        try {
            if (!lockService.tryLock(lockKey, 10, 30)) {
                log.warn("获取分布式锁失败: orderSn={}, type={}", orderSnKey, type);
                return false;
            }
            
            ProfitLog existingLog = profitLogMapper.selectByUniqueKey(orderSnKey, type, receiverId);
            if (existingLog != null) {
                log.warn("分润记录已存在，跳过: orderSn={}, type={}", orderSnKey, type);
                return false;
            }

            ProfitLog profitLog = new ProfitLog();
            profitLog.setOrderSn(orderSnKey);
            profitLog.setReceiverId(receiverId);
            profitLog.setContributorId(contributorId);
            profitLog.setAmount(amount);
            profitLog.setType(type);
            profitLog.setStatus(1);
            profitLog.setRemark(description);
            profitLog.setCreateTime(LocalDateTime.now());
            profitLog.setUpdateTime(LocalDateTime.now());
            
            profitLogMapper.insert(profitLog);
            
            log.info("创建分润记录成功: orderSn={}, receiverId={}, amount={}", 
                    orderSnKey, receiverId, amount);
            return true;
        } finally {
            lockService.unlock(lockKey);
        }
    }

    private void addBalance(Long userId, BigDecimal amount) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return;
            }
            
            BigDecimal newBalance = user.getBalance().add(amount);
            BigDecimal newEarnings = amount.compareTo(BigDecimal.ZERO) > 0 
                ? user.getTotalEarnings().add(amount) 
                : user.getTotalEarnings();
            
            user.setBalance(newBalance);
            user.setTotalEarnings(newEarnings);
            
            int updated = userMapper.updateById(user);
            if (updated > 0) {
                log.info("余额更新成功: userId={}, balance={}, earnings={}", userId, newBalance, newEarnings);
                return;
            }
            
            log.warn("乐观锁冲突，重试: userId={}, attempt={}", userId, i + 1);
        }
        
        log.error("余额更新失败，超过最大重试次数: userId={}", userId);
    }

    private boolean claimPaidOrder(Order order) {
        if (order.getId() == null) {
            return true;
        }

        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getStatus, OrderStatus.PROCESSING.getCode())
                .set(Order::getUpdateTime, LocalDateTime.now())
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.PAID.getCode()));
        if (updated <= 0) {
            log.warn("订单已被处理或正在处理，跳过分润: orderSn={}", order.getOrderSn());
            return false;
        }
        order.setStatus(OrderStatus.PROCESSING.getCode());
        return true;
    }

    private void completeOrder(Order order) {
        if (order.getId() == null) {
            order.setStatus(OrderStatus.COMPLETED.getCode());
            return;
        }

        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getStatus, OrderStatus.COMPLETED.getCode())
                .set(Order::getUpdateTime, LocalDateTime.now())
                .eq(Order::getId, order.getId()));
        order.setStatus(OrderStatus.COMPLETED.getCode());
    }

    private int getConfigInt(String key, int defaultValue) {
        BigDecimal value = policyConfigService.getConfigValue(key);
        return value == null ? defaultValue : value.intValue();
    }

    @Override
    public WalletResponse getWalletInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        WalletResponse response = new WalletResponse();
        response.setBalance(user.getBalance());
        response.setTotalEarnings(user.getTotalEarnings());
        response.setTotalWithdrawn(withdrawalMapper.sumByUserId(userId));
        response.setPendingAmount(withdrawalMapper.sumAmountByUserIdAndStatus(userId, 0));

        List<ProfitLog> logs = profitLogMapper.selectByReceiverId(userId);
        List<WalletResponse.ProfitLogDTO> recentLogs = new ArrayList<>();
        
        for (ProfitLog log : logs) {
            WalletResponse.ProfitLogDTO dto = new WalletResponse.ProfitLogDTO();
            dto.setOrderSn(log.getOrderSn());
            dto.setType(log.getType());
            dto.setTypeDesc(ProfitType.fromCode(log.getType()).getDesc());
            dto.setAmount(log.getAmount());
            dto.setCreateTime(log.getCreateTime().format(DATE_TIME_FORMATTER));
            dto.setRemark(log.getRemark());
            recentLogs.add(dto);
        }
        
        response.setRecentLogs(recentLogs);
        
        return response;
    }

    private void incrementStoreCounts(User newStore) {
        List<Long> userIds = new ArrayList<>();
        if (newStore.getParentId() != null && newStore.getParentId() > 0) {
            userIds.add(newStore.getParentId());
        }
        if (StrUtil.isNotBlank(newStore.getTreePath())) {
            String[] pathIds = newStore.getTreePath().split("/");
            for (String pathId : pathIds) {
                try {
                    Long id = Long.parseLong(pathId);
                    if (id > 0 && !id.equals(newStore.getId()) && !userIds.contains(id)) {
                        userIds.add(id);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (Long userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                continue;
            }
            int currentCount = user.getStoreCount() != null ? user.getStoreCount() : 0;
            user.setStoreCount(currentCount + 1);
            userMapper.updateById(user);
        }
    }
}
