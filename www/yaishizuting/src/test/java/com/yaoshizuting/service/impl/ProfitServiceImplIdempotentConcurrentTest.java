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
import org.junit.jupiter.api.BeforeEach;
import com.yaoshizuting.testing.TestMode;
import com.yaoshizuting.testing.TestMode.Mode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ProfitServiceImplIdempotentConcurrentTest {

    @BeforeEach
    void setupMode() {
        TestMode.setMode(Mode.CONCURRENT);
    }

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
    void testStoreJoinProfit_ConcurrentCalls_Idempotent() throws Exception {
        // Arrange: direct path with STORE as parent
        User parent = new User();
        parent.setId(1L);
        parent.setRole(UserRole.STORE.getCode());

        User newUser = new User();
        newUser.setId(2L);
        newUser.setParentId(parent.getId());
        newUser.setTreePath("/0/1/"); // no indirect path beyond parent
        newUser.setRole(UserRole.STORE.getCode());

        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-CNT-IDE-001");
        order.setUserId(newUser.getId());
        order.setStatus(1);
        order.setOrderType(1);

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(parent.getId())).thenReturn(parent);
        when(policyConfigService.getConfigValue("STORE_REWARD_DIRECT")).thenReturn(BigDecimal.valueOf(9000));

        // Shared state to simulate idempotence across concurrent invocations
        ConcurrentHashMap<String, AtomicBoolean> existsMap = new ConcurrentHashMap<>();
        String key = order.getOrderSn() + ":" + ProfitType.DIRECT_STORE.getCode() + ":" + parent.getId();

        when(profitLogMapper.selectByUniqueKey(order.getOrderSn(), ProfitType.DIRECT_STORE.getCode(), parent.getId()))
                .thenAnswer(invocation -> {
                    AtomicBoolean exists = existsMap.computeIfAbsent(key, k -> new AtomicBoolean(false));
                    return exists.get() ? new ProfitLog() : null;
                });
        when(profitLogMapper.insert(Mockito.any(ProfitLog.class))).thenAnswer(invocation -> {
            ProfitLog pl = invocation.getArgument(0);
            String k = pl.getOrderSn() + ":" + pl.getType() + ":" + pl.getReceiverId();
            existsMap.computeIfAbsent(k, kk -> new AtomicBoolean(false)).set(true);
            return 1;
        });

        // Act: run two threads concurrently
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(2);

        Runnable r = () -> {
            try {
                startGate.await();
                profitService.processJoinStoreProfit(order);
            } catch (Exception ignored) {
            } finally {
                doneGate.countDown();
            }
        };

        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r);
        t1.start();
        t2.start();
        startGate.countDown(); // release both threads
        doneGate.await();

        // Assert: only one insert should occur due to idempotent check
        verify(profitLogMapper, times(1)).insert(Mockito.any());
    }
}
