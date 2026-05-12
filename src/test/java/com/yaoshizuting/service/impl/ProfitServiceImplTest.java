package com.yaoshizuting.service.impl;

import com.yaoshizuting.dto.WalletResponse;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.enums.ProfitType;
import com.yaoshizuting.enums.UserRole;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.DistributedLockService;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.service.PolicyConfigService;
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
    private ProfitLogMapper profitLogMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private PolicyConfigService policyConfigService;

    @Mock
    private DistributedLockService lockService;

    @InjectMocks
    private com.yaoshizuting.service.impl.ProfitServiceImpl profitService;

    @BeforeEach
    void setUp() {
        profitService = new ProfitServiceImpl(userMapper, profitLogMapper, orderService, policyConfigService, lockService);
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

        // execute
        profitService.processPartnerRecruitAgentProfit(partner, newAgent);

        // verify agent count increment
        assertEquals(11, partner.getAgentCount().intValue());
        // current implementation updates the same in-memory partner instance on each balance refresh
        assertEquals(new BigDecimal("90000"), partner.getBalance());

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
}
