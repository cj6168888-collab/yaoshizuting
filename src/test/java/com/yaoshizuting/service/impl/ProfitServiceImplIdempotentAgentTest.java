package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.DistributedLockService;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.enums.ProfitType;
import com.yaoshizuting.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ProfitServiceImplIdempotentAgentTest {

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
    private ProfitServiceImpl profitService;

    @BeforeEach
    void setUp() {
        profitService = new ProfitServiceImpl(userMapper, profitLogMapper, orderService, policyConfigService, lockService);
    }

    @Test
    void testProcessJoinAgentProfit_Idempotent() {
        // partner is a PARTNER, newAgent under partner
        User partner = new User();
        partner.setId(10L);
        partner.setRole(UserRole.PARTNER.getCode());
        partner.setBalance(BigDecimal.ZERO);
        partner.setTotalEarnings(BigDecimal.ZERO);

        User newAgent = new User();
        newAgent.setId(11L);
        newAgent.setParentId(partner.getId());
        newAgent.setBalance(BigDecimal.ZERO);
        newAgent.setTotalEarnings(BigDecimal.ZERO);

        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-Idem-AGN-001");
        order.setUserId(newAgent.getId());
        order.setStatus(1); // PAID
        order.setOrderType(2);

        // Mocks
        when(userMapper.selectById(newAgent.getId())).thenReturn(newAgent);
        when(userMapper.selectById(partner.getId())).thenReturn(partner, partner, partner);
        when(policyConfigService.getConfigValue("PARTNER_REWARD_DIRECT_AGENT")).thenReturn(BigDecimal.valueOf(16000));
        when(lockService.tryLock(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
        // First call returns null (no existing log), second call returns a non-null (existing log)
        when(profitLogMapper.selectByUniqueKey(order.getOrderSn(), ProfitType.AGENT_MANAGE.getCode(), partner.getId()))
                .thenReturn(null).thenReturn(new ProfitLog());

        // Execute twice to simulate idempotent behavior
        profitService.processJoinAgentProfit(order);
        profitService.processJoinAgentProfit(order);

        // Verify insert called only once due to idempotence guard
        verify(profitLogMapper, times(1)).insert(any());
    }
}
