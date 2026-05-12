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
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.DistributedLockService;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.testing.TestMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitServiceImpl implements ProfitService {

    private final UserMapper userMapper;
    private final ProfitLogMapper profitLogMapper;
    private final OrderService orderService;
    private final PolicyConfigService policyConfigService;
    private final DistributedLockService lockService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ConcurrentHashMap<String, Object> LOG_LOCKS = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processJoinStoreProfit(Order order) {
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            log.warn("订单未支付，跳过分润: orderSn={}", order.getOrderSn());
            return;
        }

        User newUser = userMapper.selectById(order.getUserId());
        if (newUser == null) {
            throw new BusinessException("用户不存在");
        }

        newUser.setRole(UserRole.STORE.getCode());
        userMapper.updateById(newUser);

        if (newUser.getParentId() != null && newUser.getParentId() > 0) {
            distributeStoreJoinProfit(order, newUser);
        }

        log.info("处理店铺加盟分润完成: orderSn={}, userId={}", order.getOrderSn(), order.getUserId());
    }

    private void distributeStoreJoinProfit(Order order, User newUser) {
        User parent = userMapper.selectById(newUser.getParentId());
        if (parent == null) {
            return;
        }

        BigDecimal directReward = getDirectStoreReward(parent);
        
        if (directReward.compareTo(BigDecimal.ZERO) > 0) {
            createProfitLog(order.getOrderSn(), parent.getId(), newUser.getId(), directReward, 
                    ProfitType.DIRECT_STORE.getCode(), "直推店铺奖励");
            addBalance(parent.getId(), directReward);
        }

        distributeIndirectReward(order.getOrderSn(), newUser);
        distributeTeamManagementFee(order.getOrderSn(), newUser);
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
            createProfitLog(orderSn, indirectParent.getId(), newUser.getId(), indirectReward, 
                    ProfitType.INDIRECT_STORE.getCode(), "间推店铺奖励");
            addBalance(indirectParent.getId(), indirectReward);
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
                    
                    if (storeCount >= 1 && storeCount <= 100) {
                        BigDecimal managementFee = policyConfigService.getConfigValue("PARTNER_TEAM_MANAGEMENT");
                        createProfitLog(orderSn, partnerId, newUser.getId(), managementFee,
                                ProfitType.TEAM_MANAGEMENT.getCode(), "团队管理津贴");
                        addBalance(partnerId, managementFee);
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

        User newAgent = userMapper.selectById(order.getUserId());
        if (newAgent == null) {
            throw new BusinessException("用户不存在");
        }

        newAgent.setRole(UserRole.AGENT.getCode());
        userMapper.updateById(newAgent);

        if (newAgent.getParentId() != null && newAgent.getParentId() > 0) {
            distributeAgentJoinProfit(order, newAgent);
        }

        log.info("处理代理加盟分润完成: orderSn={}, userId={}", order.getOrderSn(), order.getUserId());
    }

    private void distributeAgentJoinProfit(Order order, User newAgent) {
        User parent = userMapper.selectById(newAgent.getParentId());
        if (parent == null) {
            return;
        }

        BigDecimal directReward = BigDecimal.ZERO;
        String description = "直推代理奖励";

        if (parent.getRole() == UserRole.PARTNER.getCode()) {
            directReward = policyConfigService.getConfigValue("PARTNER_REWARD_DIRECT_AGENT");
            int currentCount = parent.getAgentCount() != null ? parent.getAgentCount() : 0;
            parent.setAgentCount(currentCount + 1);
            userMapper.updateById(parent);
        } else if (parent.getRole() == UserRole.AGENT.getCode() || parent.getRole() == UserRole.STORE.getCode()) {
            directReward = policyConfigService.getConfigValue("AGENT_REWARD_DIRECT_AGENT");
        }

        if (directReward.compareTo(BigDecimal.ZERO) > 0) {
            createProfitLog(order.getOrderSn(), parent.getId(), newAgent.getId(), directReward, 
                    ProfitType.AGENT_MANAGE.getCode(), description);
            addBalance(parent.getId(), directReward);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processJoinPartnerProfit(Order order) {
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            log.warn("订单未支付，跳过分润: orderSn={}", order.getOrderSn());
            return;
        }

        User newPartner = userMapper.selectById(order.getUserId());
        if (newPartner == null) {
            throw new BusinessException("用户不存在");
        }

        newPartner.setRole(UserRole.PARTNER.getCode());
        userMapper.updateById(newPartner);

        if (newPartner.getParentId() != null && newPartner.getParentId() > 0) {
            distributePartnerJoinProfit(order, newPartner);
        }

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
            createProfitLog(order.getOrderSn(), parent.getId(), newPartner.getId(), directReward, 
                    ProfitType.PARTNER_DIRECT.getCode(), description);
            addBalance(parent.getId(), directReward);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processPartnerRecruitAgentProfit(User partner, User newAgent) {
        int currentCount = partner.getAgentCount() != null ? partner.getAgentCount() : 0;

        BigDecimal manageFee = policyConfigService.getConfigValue("PARTNER_MANAGE_FEE");
        String orderSn = "AGENT-" + newAgent.getId();
        createProfitLog(orderSn, partner.getId(), newAgent.getId(), manageFee,
                ProfitType.AGENT_MANAGE.getCode(), "代理商管理培训费");
        addBalance(partner.getId(), manageFee);

        if (currentCount >= 10) {
            BigDecimal supportFee = policyConfigService.getConfigValue("HEADQUARTER_SUPPORT_FEE");
            createProfitLog("SUP-" + newAgent.getId(), partner.getId(), newAgent.getId(), supportFee.negate(),
                    ProfitType.HEADQUARTER_SUPPORT_FEE.getCode(), "总部培训支持费");
            addBalance(partner.getId(), supportFee.negate());
        }

        partner.setAgentCount(currentCount + 1);
        userMapper.updateById(partner);

        log.info("处理合伙人招募代理分润: partnerId={}, agentId={}, count={}", partner.getId(), newAgent.getId(), currentCount + 1);
    }

    private void createProfitLog(String orderSnKey, Long receiverId, Long contributorId, BigDecimal amount, 
                                  String type, String description) {
        String lockKey = "profit:lock:" + orderSnKey + ":" + type + ":" + receiverId;
        
        try {
            if (!lockService.tryLock(lockKey, 10, 30)) {
                log.warn("获取分布式锁失败: orderSn={}, type={}", orderSnKey, type);
                return;
            }
            
            ProfitLog existingLog = profitLogMapper.selectByUniqueKey(orderSnKey, type, receiverId);
            if (existingLog != null) {
                log.warn("分润记录已存在，跳过: orderSn={}, type={}", orderSnKey, type);
                return;
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

    @Override
    public WalletResponse getWalletInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        WalletResponse response = new WalletResponse();
        response.setBalance(user.getBalance());
        response.setTotalEarnings(user.getTotalEarnings());

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
}
