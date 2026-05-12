package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
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
public class ProfitServiceImplIdempotentPartnerTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProfitLogMapper profitLogMapper;

    @Mock
    private PolicyConfigService policyConfigService;

    @Mock
    private com.yaoshizuting.service.OrderService orderService;

    @InjectMocks
    private ProfitService profitService = new com.yaoshizuting.service.impl.ProfitServiceImpl();

    @Test
    void testProcessPartnerRecruitAgentProfit_Idempotent() {
        // arrange partner and newAgent
        User partner = new User();
        partner.setId(100L);
        partner.setRole(UserRole.PARTNER.getCode());
        partner.setAgentCount(10);

        User newAgent = new User();
        newAgent.setId(101L);
        newAgent.setParentId(partner.getId());

        // order details (simple mock trigger)
        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-PARTNER-001");
        order.setUserId(newAgent.getId());
        order.setStatus(1); // PAID
        order.setOrderType(3);

        when(userMapper.selectById(partner.getId())).thenReturn(partner);
        when(userMapper.selectById(newAgent.getId())).thenReturn(newAgent);
        when(policyConfigService.getConfigValue("PARTNER_MANAGE_FEE")).thenReturn(BigDecimal.valueOf(39800));
        when(policyConfigService.getConfigValue("HEADQUARTER_SUPPORT_FEE")).thenReturn(BigDecimal.valueOf(9800));
        when(profitLogMapper.selectByUniqueKey(order.getOrderSn(), ProfitType.PARTNER_DIRECT.getCode(), partner.getId()))
                .thenReturn(null).thenReturn(new ProfitLog());

        // act twice to test idempotency
        profitService.processPartnerRecruitAgentProfit(partner, newAgent);
        profitService.processPartnerRecruitAgentProfit(partner, newAgent);

        verify(profitLogMapper, times(1)).insert(org.mockito.Matchers.any());
    }
}
