package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.ProfitType;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.DistributedLockService;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.service.OrderService;
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
public class ProfitServiceImplIdempotentTest {

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
    void testStoreJoinProfit_Idempotent() {
        // Setup: a new user under a STORE parent, with a valid paid order
        User parent = new User();
        parent.setId(1L);
        parent.setRole(1); // STORE
        parent.setBalance(BigDecimal.ZERO);
        parent.setTotalEarnings(BigDecimal.ZERO);

        User newUser = new User();
        newUser.setId(2L);
        newUser.setParentId(parent.getId());
        newUser.setTreePath("/0/1/");
        newUser.setRole(1);
        newUser.setBalance(BigDecimal.ZERO);
        newUser.setTotalEarnings(BigDecimal.ZERO);

        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-Idem-001");
        order.setUserId(newUser.getId());
        order.setStatus(1); // PAID
        order.setOrderType(1);

        // Mocks
        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parent.getId())).thenReturn(parent, parent, parent);
        when(policyConfigService.getConfigValue("STORE_REWARD_DIRECT")).thenReturn(BigDecimal.valueOf(9000));
        when(lockService.tryLock(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(true);
        // First call returns null (no existing log), second call returns a non-null (existing log)
        when(profitLogMapper.selectByUniqueKey(order.getOrderSn(), ProfitType.DIRECT_STORE.getCode(), parent.getId()))
                .thenReturn(null).thenReturn(new ProfitLog());

        // Execute twice to simulate idempotent behavior
        profitService.processJoinStoreProfit(order);
        profitService.processJoinStoreProfit(order);

        // Verify insert called only once due to idempotence guard
        verify(profitLogMapper, times(1)).insert(any());
    }
}
