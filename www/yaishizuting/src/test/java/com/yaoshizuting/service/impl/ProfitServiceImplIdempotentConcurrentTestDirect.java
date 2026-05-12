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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ProfitServiceImplIdempotentConcurrentTestDirect {

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
    void testTwoConcurrentExecutions_SameOrder_IdempotentDirect() throws InterruptedException {
        // Arrange: parent is STORE, direct recipient will be parent
        User parent = new User();
        parent.setId(1L);
        parent.setRole(UserRole.STORE.getCode());

        User newUser = new User();
        newUser.setId(2L);
        newUser.setParentId(parent.getId());
        newUser.setTreePath("/0/1/"); // direct path only
        newUser.setRole(UserRole.STORE.getCode());

        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-CNT-IDE-TEST1");
        order.setUserId(newUser.getId());
        order.setStatus(1);
        order.setOrderType(1);

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parent.getId())).thenReturn(parent);
        when(policyConfigService.getConfigValue("STORE_REWARD_DIRECT")).thenReturn(BigDecimal.valueOf(9000));

        // Mock idempotence: first call allowed, second call pretends inserted already
        // We implement a simple in-test flag per order+receiver
        AtomicBoolean firstInserted = new AtomicBoolean(false);
        when(profitLogMapper.selectByUniqueKey(order.getOrderSn(), ProfitType.DIRECT_STORE.getCode(), parent.getId()))
                .thenAnswer(invocation -> {
                    return firstInserted.get() ? new ProfitLog() : null;
                });
        when(profitLogMapper.insert(Mockito.any(ProfitLog.class))).thenAnswer(invocation -> {
            firstInserted.set(true);
            return 1;
        });

        // Concurrent execution
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        Runnable r = () -> {
            try {
                start.await(1, TimeUnit.SECONDS);
                profitService.processJoinStoreProfit(order);
            } catch (Exception ignored) {
            } finally {
                done.countDown();
            }
        };
        new Thread(r).start();
        new Thread(r).start();
        start.countDown();
        done.await(5, TimeUnit.SECONDS);

        verify(profitLogMapper, times(1)).insert(Mockito.any());
    }
}
