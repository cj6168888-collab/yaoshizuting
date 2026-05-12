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
public class ProfitServiceImplIdempotentStoreTest2 {

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
    void testStoreJoinProfit_Idempotent_DuplicateOrder() {
        User parent = new User();
        parent.setId(1L);
        parent.setRole(UserRole.STORE.getCode());

        User newUser = new User();
        newUser.setId(2L);
        newUser.setParentId(parent.getId());
        newUser.setTreePath("/0/1/");
        newUser.setRole(UserRole.STORE.getCode());

        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-Idem-STORE-002");
        order.setUserId(newUser.getId());
        order.setStatus(1); // PAID
        order.setOrderType(1);

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parent.getId())).thenReturn(parent);
        when(policyConfigService.getConfigValue("STORE_REWARD_DIRECT")).thenReturn(BigDecimal.valueOf(9000));
        when(profitLogMapper.selectByUniqueKey(order.getOrderSn(), ProfitType.DIRECT_STORE.getCode(), parent.getId()))
                .thenReturn(null).thenReturn(new ProfitLog());

        profitService.processJoinStoreProfit(order);
        profitService.processJoinStoreProfit(order);

        verify(profitLogMapper, times(1)).insert(org.mockito.Matchers.any());
    }
}
