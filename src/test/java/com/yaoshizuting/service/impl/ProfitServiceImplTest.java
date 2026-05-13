package com.yaoshizuting.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import com.yaoshizuting.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProfitServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ProfitLogMapper profitLogMapper;

    @Mock
    private PolicyConfigService policyConfigService;

    @Mock
    private DistributedLockService lockService;

    @Mock
    private WithdrawalMapper withdrawalMapper;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private com.yaoshizuting.service.impl.ProfitServiceImpl profitService;

    @BeforeEach
    void setUp() {
        profitService = new ProfitServiceImpl(userMapper, orderMapper, profitLogMapper, policyConfigService, lockService, withdrawalMapper, teamService);
    }

    @Test
    void processJoinStoreProfitSkipsUnpaidOrder() {
        Order order = order("ORD-STORE-PENDING", 10L, OrderStatus.PENDING.getCode());

        profitService.processJoinStoreProfit(order);

        verify(userMapper, never()).selectById(anyLong());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinStoreProfitRejectsMissingUser() {
        Order order = order("ORD-STORE-MISSING", 10L, OrderStatus.PAID.getCode());
        when(userMapper.selectById(10L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> profitService.processJoinStoreProfit(order));

        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void processJoinStoreProfitPromotesUserWithoutParentAndSkipsProfit() {
        User newUser = user(10L, UserRole.MEMBER, "0.00", "0.00");
        newUser.setParentId(null);
        Order order = order("ORD-STORE-NO-PARENT", newUser.getId(), OrderStatus.PAID.getCode());
        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);

        profitService.processJoinStoreProfit(order);

        assertEquals(UserRole.STORE.getCode(), newUser.getRole());
        verify(userMapper).updateById(newUser);
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinStoreProfitRewardsSecondDirectStoreAndSkipsIndirectRewardByDefault() {
        User ancestorPartner = user(1L, UserRole.PARTNER, "0.00", "0.00");
        ancestorPartner.setStoreCount(1);
        User parentStore = user(3L, UserRole.STORE, "0.00", "0.00");
        parentStore.setStoreCount(1);
        User newUser = user(10L, UserRole.MEMBER, "0.00", "0.00");
        newUser.setParentId(parentStore.getId());
        newUser.setTreePath("/0/1/3/");
        Order order = order("ORD-STORE-REWARDS", newUser.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parentStore.getId())).thenReturn(parentStore, parentStore);
        when(userMapper.selectById(ancestorPartner.getId()))
                .thenReturn(ancestorPartner, ancestorPartner, ancestorPartner, ancestorPartner);
        when(policyConfigService.getConfigValue("STORE_DIRECT_REWARD_START_COUNT")).thenReturn(new BigDecimal("2"));
        when(policyConfigService.getConfigValue("STORE_REWARD_DIRECT")).thenReturn(new BigDecimal("9000.00"));
        when(policyConfigService.getConfigValue("STORE_INDIRECT_REWARD_ENABLED")).thenReturn(BigDecimal.ZERO);
        when(policyConfigService.getConfigValue("PARTNER_TEAM_MANAGEMENT_START_COUNT")).thenReturn(new BigDecimal("2"));
        when(policyConfigService.getConfigValue("PARTNER_TEAM_MANAGEMENT_END_COUNT")).thenReturn(new BigDecimal("100"));
        when(policyConfigService.getConfigValue("PARTNER_TEAM_MANAGEMENT")).thenReturn(new BigDecimal("1000.00"));
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        profitService.processJoinStoreProfit(order);

        assertEquals(UserRole.STORE.getCode(), newUser.getRole());
        assertEquals(new BigDecimal("9000.00"), parentStore.getBalance());
        assertEquals(new BigDecimal("9000.00"), parentStore.getTotalEarnings());
        assertEquals(new BigDecimal("1000.00"), ancestorPartner.getBalance());
        assertEquals(new BigDecimal("1000.00"), ancestorPartner.getTotalEarnings());

        ArgumentCaptor<ProfitLog> logCaptor = ArgumentCaptor.forClass(ProfitLog.class);
        verify(profitLogMapper, times(2)).insert(logCaptor.capture());
        List<ProfitLog> logs = logCaptor.getAllValues();
        assertEquals(ProfitType.DIRECT_STORE.getCode(), logs.get(0).getType());
        assertEquals(new BigDecimal("9000.00"), logs.get(0).getAmount());
        assertEquals(ProfitType.TEAM_MANAGEMENT.getCode(), logs.get(1).getType());
        assertEquals(new BigDecimal("1000.00"), logs.get(1).getAmount());
    }

    @Test
    void processJoinStoreProfitSkipsFirstDirectStoreReward() {
        User parentStore = user(3L, UserRole.STORE, "0.00", "0.00");
        parentStore.setStoreCount(0);
        User newUser = user(10L, UserRole.MEMBER, "0.00", "0.00");
        newUser.setParentId(parentStore.getId());
        newUser.setTreePath("/0/3/");
        Order order = order("ORD-STORE-FIRST-DIRECT", newUser.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parentStore.getId())).thenReturn(parentStore, parentStore);
        when(policyConfigService.getConfigValue("STORE_DIRECT_REWARD_START_COUNT")).thenReturn(new BigDecimal("2"));
        when(policyConfigService.getConfigValue("STORE_INDIRECT_REWARD_ENABLED")).thenReturn(BigDecimal.ZERO);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        profitService.processJoinStoreProfit(order);

        assertEquals(1, parentStore.getStoreCount());
        assertEquals(new BigDecimal("0.00"), parentStore.getBalance());
        verify(profitLogMapper, never()).insert(any());
    }

    @Test
    void processJoinStoreProfitSkipsRewardWhenParentMissing() {
        User newUser = user(10L, UserRole.MEMBER, "0.00", "0.00");
        newUser.setParentId(99L);
        Order order = order("ORD-STORE-MISSING-PARENT", newUser.getId(), OrderStatus.PAID.getCode());
        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(99L)).thenReturn(null);

        profitService.processJoinStoreProfit(order);

        assertEquals(UserRole.STORE.getCode(), newUser.getRole());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinStoreProfitRewardsAgentParentAndSkipsBlankTreePath() {
        User parentAgent = user(2L, UserRole.AGENT, "10.00", "20.00");
        parentAgent.setStoreCount(1);
        User newUser = user(10L, UserRole.MEMBER, "0.00", "0.00");
        newUser.setParentId(parentAgent.getId());
        newUser.setTreePath(" ");
        Order order = order("ORD-STORE-AGENT-PARENT", newUser.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parentAgent.getId())).thenReturn(parentAgent, parentAgent);
        when(policyConfigService.getConfigValue("STORE_DIRECT_REWARD_START_COUNT")).thenReturn(new BigDecimal("2"));
        when(policyConfigService.getConfigValue("AGENT_REWARD_DIRECT")).thenReturn(new BigDecimal("7000.00"));
        when(policyConfigService.getConfigValue("STORE_INDIRECT_REWARD_ENABLED")).thenReturn(BigDecimal.ZERO);
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        profitService.processJoinStoreProfit(order);

        assertEquals(new BigDecimal("7010.00"), parentAgent.getBalance());
        assertEquals(new BigDecimal("7020.00"), parentAgent.getTotalEarnings());
        verify(profitLogMapper).insert(any(ProfitLog.class));
    }

    @Test
    void processJoinStoreProfitSkipsShortTreePathAndZeroDirectReward() {
        User parentStore = user(3L, UserRole.STORE, "0.00", "0.00");
        parentStore.setStoreCount(1);
        User newUser = user(10L, UserRole.MEMBER, "0.00", "0.00");
        newUser.setParentId(parentStore.getId());
        newUser.setTreePath("/");
        Order order = order("ORD-STORE-SHORT-PATH", newUser.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parentStore.getId())).thenReturn(parentStore);
        when(policyConfigService.getConfigValue("STORE_DIRECT_REWARD_START_COUNT")).thenReturn(new BigDecimal("2"));
        when(policyConfigService.getConfigValue("STORE_REWARD_DIRECT")).thenReturn(BigDecimal.ZERO);
        when(policyConfigService.getConfigValue("STORE_INDIRECT_REWARD_ENABLED")).thenReturn(BigDecimal.ZERO);

        profitService.processJoinStoreProfit(order);

        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinStoreProfitIgnoresInvalidAncestorPath() {
        User parentStore = user(3L, UserRole.STORE, "0.00", "0.00");
        parentStore.setStoreCount(1);
        User newUser = user(10L, UserRole.MEMBER, "0.00", "0.00");
        newUser.setParentId(parentStore.getId());
        newUser.setTreePath("/abc/3/");
        Order order = order("ORD-STORE-BAD-PATH", newUser.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parentStore.getId())).thenReturn(parentStore);
        when(policyConfigService.getConfigValue("STORE_DIRECT_REWARD_START_COUNT")).thenReturn(new BigDecimal("2"));
        when(policyConfigService.getConfigValue("STORE_REWARD_DIRECT")).thenReturn(BigDecimal.ZERO);
        when(policyConfigService.getConfigValue("STORE_INDIRECT_REWARD_ENABLED")).thenReturn(BigDecimal.ZERO);

        profitService.processJoinStoreProfit(order);

        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinAgentProfitSkipsUnpaidOrder() {
        Order order = order("ORD-AGENT-PENDING", 20L, OrderStatus.PENDING.getCode());

        profitService.processJoinAgentProfit(order);

        verify(userMapper, never()).selectById(anyLong());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinAgentProfitRejectsMissingUser() {
        Order order = order("ORD-AGENT-MISSING", 20L, OrderStatus.PAID.getCode());
        when(userMapper.selectById(20L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> profitService.processJoinAgentProfit(order));

        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void processJoinAgentProfitRewardsStoreParent() {
        User parentStore = user(30L, UserRole.STORE, "100.00", "200.00");
        User newAgent = user(40L, UserRole.MEMBER, "0.00", "0.00");
        newAgent.setParentId(parentStore.getId());
        Order order = order("ORD-AGENT-STORE-PARENT", newAgent.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newAgent.getId())).thenReturn(newAgent);
        when(userMapper.selectById(parentStore.getId())).thenReturn(parentStore, parentStore);
        when(policyConfigService.getConfigValue("AGENT_REWARD_DIRECT_AGENT")).thenReturn(new BigDecimal("6000.00"));
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        profitService.processJoinAgentProfit(order);

        assertEquals(UserRole.AGENT.getCode(), newAgent.getRole());
        assertEquals(new BigDecimal("6100.00"), parentStore.getBalance());
        assertEquals(new BigDecimal("6200.00"), parentStore.getTotalEarnings());
        ArgumentCaptor<ProfitLog> logCaptor = ArgumentCaptor.forClass(ProfitLog.class);
        verify(profitLogMapper).insert(logCaptor.capture());
        assertEquals(ProfitType.AGENT_MANAGE.getCode(), logCaptor.getValue().getType());
        assertEquals(new BigDecimal("6000.00"), logCaptor.getValue().getAmount());
    }

    @Test
    void processJoinAgentProfitPromotesUserWithoutParentAndSkipsProfit() {
        User newAgent = user(40L, UserRole.MEMBER, "0.00", "0.00");
        newAgent.setParentId(null);
        Order order = order("ORD-AGENT-NO-PARENT", newAgent.getId(), OrderStatus.PAID.getCode());
        when(userMapper.selectById(newAgent.getId())).thenReturn(newAgent);

        profitService.processJoinAgentProfit(order);

        assertEquals(UserRole.AGENT.getCode(), newAgent.getRole());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinAgentProfitSkipsRewardWhenParentMissing() {
        User newAgent = user(40L, UserRole.MEMBER, "0.00", "0.00");
        newAgent.setParentId(99L);
        Order order = order("ORD-AGENT-MISSING-PARENT", newAgent.getId(), OrderStatus.PAID.getCode());
        when(userMapper.selectById(newAgent.getId())).thenReturn(newAgent);
        when(userMapper.selectById(99L)).thenReturn(null);

        profitService.processJoinAgentProfit(order);

        assertEquals(UserRole.AGENT.getCode(), newAgent.getRole());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinAgentProfitRewardsAgentParent() {
        User parentAgent = user(30L, UserRole.AGENT, "100.00", "200.00");
        User newAgent = user(40L, UserRole.MEMBER, "0.00", "0.00");
        newAgent.setParentId(parentAgent.getId());
        Order order = order("ORD-AGENT-AGENT-PARENT", newAgent.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newAgent.getId())).thenReturn(newAgent);
        when(userMapper.selectById(parentAgent.getId())).thenReturn(parentAgent, parentAgent);
        when(policyConfigService.getConfigValue("AGENT_REWARD_DIRECT_AGENT")).thenReturn(new BigDecimal("6000.00"));
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        profitService.processJoinAgentProfit(order);

        assertEquals(new BigDecimal("6100.00"), parentAgent.getBalance());
        assertEquals(new BigDecimal("6200.00"), parentAgent.getTotalEarnings());
        verify(profitLogMapper).insert(any(ProfitLog.class));
    }

    @Test
    void processJoinAgentProfitSkipsUnsupportedParentRole() {
        User parentMember = user(30L, UserRole.MEMBER, "100.00", "200.00");
        User newAgent = user(40L, UserRole.MEMBER, "0.00", "0.00");
        newAgent.setParentId(parentMember.getId());
        Order order = order("ORD-AGENT-UNSUPPORTED-PARENT", newAgent.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newAgent.getId())).thenReturn(newAgent);
        when(userMapper.selectById(parentMember.getId())).thenReturn(parentMember);

        profitService.processJoinAgentProfit(order);

        assertEquals(UserRole.AGENT.getCode(), newAgent.getRole());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinPartnerProfitRejectsMissingUser() {
        Order order = order("ORD-PARTNER-MISSING", 40L, OrderStatus.PAID.getCode());
        when(userMapper.selectById(40L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> profitService.processJoinPartnerProfit(order));

        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void processJoinPartnerProfitPromotesUserWithoutParentAndSkipsProfit() {
        User newPartner = user(40L, UserRole.MEMBER, "0.00", "0.00");
        newPartner.setParentId(null);
        Order order = order("ORD-PARTNER-NO-PARENT", newPartner.getId(), OrderStatus.PAID.getCode());
        when(userMapper.selectById(newPartner.getId())).thenReturn(newPartner);

        profitService.processJoinPartnerProfit(order);

        assertEquals(UserRole.PARTNER.getCode(), newPartner.getRole());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinPartnerProfitSkipsRewardWhenParentMissing() {
        User newPartner = user(40L, UserRole.MEMBER, "0.00", "0.00");
        newPartner.setParentId(99L);
        Order order = order("ORD-PARTNER-MISSING-PARENT", newPartner.getId(), OrderStatus.PAID.getCode());
        when(userMapper.selectById(newPartner.getId())).thenReturn(newPartner);
        when(userMapper.selectById(99L)).thenReturn(null);

        profitService.processJoinPartnerProfit(order);

        assertEquals(UserRole.PARTNER.getCode(), newPartner.getRole());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processJoinPartnerProfitSkipsUnsupportedParentRole() {
        User parentStore = user(30L, UserRole.STORE, "100.00", "200.00");
        User newPartner = user(40L, UserRole.MEMBER, "0.00", "0.00");
        newPartner.setParentId(parentStore.getId());
        Order order = order("ORD-PARTNER-UNSUPPORTED-PARENT", newPartner.getId(), OrderStatus.PAID.getCode());

        when(userMapper.selectById(newPartner.getId())).thenReturn(newPartner);
        when(userMapper.selectById(parentStore.getId())).thenReturn(parentStore);

        profitService.processJoinPartnerProfit(order);

        assertEquals(UserRole.PARTNER.getCode(), newPartner.getRole());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void processPartnerRecruitAgentProfitWithoutExistingCountSkipsSupportFee() {
        User partner = user(1L, UserRole.PARTNER, "0.00", "0.00");
        partner.setAgentCount(null);
        User newAgent = user(200L, UserRole.AGENT, "0.00", "0.00");
        when(policyConfigService.getConfigValue("PARTNER_MANAGE_FEE")).thenReturn(new BigDecimal("39800.00"));
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userMapper.selectById(partner.getId())).thenReturn(partner);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(userMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        profitService.processPartnerRecruitAgentProfit(partner, newAgent);

        assertEquals(1, partner.getAgentCount());
        assertEquals(new BigDecimal("39800.00"), partner.getBalance());
        verify(policyConfigService, never()).getConfigValue("HEADQUARTER_SUPPORT_FEE");
        verify(profitLogMapper).insert(any(ProfitLog.class));
        verify(userMapper).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void processPartnerRecruitAgentProfitSkipsLogWhenLockUnavailableAndBalanceUserMissing() {
        User partner = user(1L, UserRole.PARTNER, "0.00", "0.00");
        partner.setAgentCount(null);
        User newAgent = user(200L, UserRole.AGENT, "0.00", "0.00");

        when(policyConfigService.getConfigValue("PARTNER_MANAGE_FEE")).thenReturn(new BigDecimal("39800.00"));
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(false);
        when(userMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        profitService.processPartnerRecruitAgentProfit(partner, newAgent);

        assertEquals(1, partner.getAgentCount());
        assertEquals(new BigDecimal("0.00"), partner.getBalance());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService).unlock(anyString());
        verify(userMapper).update(any(), any(UpdateWrapper.class));
    }

    @Test
    void testProcessPartnerRecruitAgentProfit_Boundary11_FeeApplied() {
        // setup partner as PARTNER with agentCount 10
        User partner = new User();
        partner.setId(1L);
        partner.setRole(3); // PARTNER
        partner.setAgentCount(10);
        partner.setBalance(BigDecimal.ZERO);
        partner.setTotalEarnings(BigDecimal.ZERO);

        // new agent recruited by partner
        User newAgent = new User();
        newAgent.setId(200L);
        newAgent.setParentId(partner.getId());

        when(policyConfigService.getConfigValue("PARTNER_MANAGE_FEE")).thenReturn(BigDecimal.valueOf(39800));
        when(policyConfigService.getConfigValue("HEADQUARTER_SUPPORT_FEE")).thenReturn(BigDecimal.valueOf(9800));
        when(lockService.tryLock(any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
        // balance update flow uses selectById for fetch in addBalance
        when(userMapper.selectById(partner.getId())).thenReturn(partner, partner, partner);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(userMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        // execute
        profitService.processPartnerRecruitAgentProfit(partner, newAgent);

        // verify agent count increment
        assertEquals(11, partner.getAgentCount().intValue());
        assertEquals(new BigDecimal("30000"), partner.getBalance());

        // verify profit logs: two entries created
        ArgumentCaptor<ProfitLog> logCaptor = ArgumentCaptor.forClass(ProfitLog.class);
        verify(profitLogMapper, times(2)).insert(logCaptor.capture());
        List<ProfitLog> logs = logCaptor.getAllValues();
        ProfitLog first = logs.get(0);
        ProfitLog second = logs.get(1);

        // first log: 39,800 +  agent management fee
        assertEquals(partner.getId(), first.getReceiverId());
        assertEquals(newAgent.getId(), first.getContributorId());
        assertEquals(BigDecimal.valueOf(39800).setScale(2), first.getAmount().setScale(2));
        assertEquals(ProfitType.AGENT_MANAGE.getCode(), first.getType());
        // orderSn should be agent-specific
        // second log: -9800 HQ fee
        assertEquals(partner.getId(), second.getReceiverId());
        assertEquals(newAgent.getId(), second.getContributorId());
        assertEquals(BigDecimal.valueOf(-9800).setScale(2), second.getAmount().setScale(2));
        assertEquals(ProfitType.HEADQUARTER_SUPPORT_FEE.getCode(), second.getType());
    }

    @Test
    void processJoinPartnerProfitCreatesDirectPartnerReward() {
        User parent = new User();
        parent.setId(30L);
        parent.setRole(UserRole.PARTNER.getCode());
        parent.setBalance(new BigDecimal("500.00"));
        parent.setTotalEarnings(new BigDecimal("800.00"));

        User newPartner = new User();
        newPartner.setId(40L);
        newPartner.setParentId(parent.getId());
        newPartner.setBalance(BigDecimal.ZERO);
        newPartner.setTotalEarnings(BigDecimal.ZERO);

        Order order = new Order();
        order.setOrderSn("ORD-PARTNER-001");
        order.setUserId(newPartner.getId());
        order.setStatus(OrderStatus.PAID.getCode());

        when(userMapper.selectById(newPartner.getId())).thenReturn(newPartner);
        when(userMapper.selectById(parent.getId())).thenReturn(parent, parent);
        when(policyConfigService.getConfigValue("PARTNER_REWARD_DIRECT_PARTNER"))
                .thenReturn(new BigDecimal("12000.00"));
        when(lockService.tryLock(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        profitService.processJoinPartnerProfit(order);

        assertEquals(UserRole.PARTNER.getCode(), newPartner.getRole());
        assertEquals(new BigDecimal("12500.00"), parent.getBalance());
        assertEquals(new BigDecimal("12800.00"), parent.getTotalEarnings());

        ArgumentCaptor<ProfitLog> logCaptor = ArgumentCaptor.forClass(ProfitLog.class);
        verify(profitLogMapper).insert(logCaptor.capture());
        ProfitLog log = logCaptor.getValue();
        assertEquals("ORD-PARTNER-001", log.getOrderSn());
        assertEquals(parent.getId(), log.getReceiverId());
        assertEquals(newPartner.getId(), log.getContributorId());
        assertEquals(new BigDecimal("12000.00"), log.getAmount());
        assertEquals(ProfitType.PARTNER_DIRECT.getCode(), log.getType());
    }

    @Test
    void processJoinPartnerProfitSkipsUnpaidOrder() {
        Order order = new Order();
        order.setOrderSn("ORD-PARTNER-PENDING");
        order.setUserId(40L);
        order.setStatus(OrderStatus.PENDING.getCode());

        profitService.processJoinPartnerProfit(order);

        verify(userMapper, never()).selectById(anyLong());
        verify(profitLogMapper, never()).insert(any());
        verify(lockService, never()).tryLock(anyString(), anyLong(), anyLong());
    }

    @Test
    void getWalletInfoMapsRecentProfitLogs() {
        User user = new User();
        user.setId(50L);
        user.setBalance(new BigDecimal("66.00"));
        user.setTotalEarnings(new BigDecimal("99.00"));

        ProfitLog log = new ProfitLog();
        log.setOrderSn("ORD-WALLET-001");
        log.setType(ProfitType.DIRECT_STORE.getCode());
        log.setAmount(new BigDecimal("10.00"));
        log.setRemark("direct reward");
        log.setCreateTime(LocalDateTime.of(2026, 5, 12, 9, 30, 0));

        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(profitLogMapper.selectByReceiverId(user.getId())).thenReturn(List.of(log));

        WalletResponse response = profitService.getWalletInfo(user.getId());

        assertEquals(new BigDecimal("66.00"), response.getBalance());
        assertEquals(new BigDecimal("99.00"), response.getTotalEarnings());
        assertEquals(1, response.getRecentLogs().size());
        WalletResponse.ProfitLogDTO dto = response.getRecentLogs().get(0);
        assertEquals("ORD-WALLET-001", dto.getOrderSn());
        assertEquals(ProfitType.DIRECT_STORE.getCode(), dto.getType());
        assertNotNull(dto.getTypeDesc());
        assertEquals(new BigDecimal("10.00"), dto.getAmount());
        assertEquals("2026-05-12 09:30:00", dto.getCreateTime());
        assertEquals("direct reward", dto.getRemark());
    }

    @Test
    void getWalletInfoRejectsMissingUser() {
        when(userMapper.selectById(50L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> profitService.getWalletInfo(50L));

        assertEquals(404, exception.getCode());
        assertEquals("用户不存在", exception.getMessage());
        verify(profitLogMapper, never()).selectByReceiverId(anyLong());
    }

    private Order order(String orderSn, Long userId, Integer status) {
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setUserId(userId);
        order.setStatus(status);
        return order;
    }

    private User user(Long id, UserRole role, String balance, String totalEarnings) {
        User user = new User();
        user.setId(id);
        user.setRole(role.getCode());
        user.setBalance(new BigDecimal(balance));
        user.setTotalEarnings(new BigDecimal(totalEarnings));
        return user;
    }
}
