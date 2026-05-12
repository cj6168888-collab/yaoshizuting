package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.enums.ProfitType;
import com.yaoshizuting.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private ProfitService profitService = new com.yaoshizuting.service.impl.ProfitServiceImpl();

    @Test
    void testProcessJoinAgentProfit_Idempotent() {
        // partner is a PARTNER, newAgent under partner
        User partner = new User();
        partner.setId(10L);
        partner.setRole(UserRole.PARTNER.getCode());

        User newAgent = new User();
        newAgent.setId(11L);
        newAgent.setParentId(partner.getId());

        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-Idem-AGN-001");
        order.setUserId(newAgent.getId());
        order.setStatus(1); // PAID
        order.setOrderType(2);

        // Mocks
        when(userMapper.selectById(newAgent.getId())).thenReturn(newAgent);
        when(userMapper.selectById(partner.getId())).thenReturn(partner);
        when(policyConfigService.getConfigValue("PARTNER_REWARD_DIRECT_AGENT")).thenReturn(BigDecimal.valueOf(16000));
        // First call returns null (no existing log), second call returns a non-null (existing log)
        when(profitLogMapper.selectByUniqueKey(order.getOrderSn(), ProfitType.AGENT_MANAGE.getCode(), partner.getId()))
                .thenReturn(null).thenReturn(new ProfitLog());

        // Execute twice to simulate idempotent behavior
        profitService.processJoinAgentProfit(order);
        profitService.processJoinAgentProfit(order);

        // Verify insert called only once due to idempotence guard
        verify(profitLogMapper, times(1)).insert(org.mockito.Matchers.any());
    }
}
