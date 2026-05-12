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
public class ProfitServiceImplIdempotentConcurrentTestIndirect {

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
    void testIndirectJoinProfit_Concurrent_Idempotent() throws InterruptedException {
        // Arrange: create a chain so that INDIRECT_STORE will be paid to an indirect recipient
        User partner = new User();
        partner.setId(1L);
        partner.setRole(UserRole.PARTNER.getCode());

        User intermediate = new User();
        intermediate.setId(2L);
        intermediate.setRole(UserRole.AGENT.getCode());
        intermediate.setParentId(partner.getId());
        intermediate.setTreePath("/0/1/2/");

        User newUser = new User();
        newUser.setId(3L);
        newUser.setParentId(intermediate.getId());
        newUser.setTreePath("/0/1/2/3/");
        newUser.setRole(UserRole.STORE.getCode());

        com.yaoshizuting.entity.Order order = new com.yaoshizuting.entity.Order();
        order.setOrderSn("ORD-CNT-IDE-IND" );
        order.setUserId(newUser.getId());
        order.setStatus(1);
        order.setOrderType(1);

        when(userMapper.selectById(newUser.getId())).thenReturn(newUser);
        when(userMapper.selectById(intermediate.getId())).thenReturn(intermediate);
        when(userMapper.selectById(partner.getId())).thenReturn(partner);
        when(policyConfigService.getConfigValue("REWARD_INDIRECT")).thenReturn(BigDecimal.valueOf(6000));

        AtomicBoolean inserted = new AtomicBoolean(false);
        String indirectKey = order.getOrderSn() + ":" + ProfitType.INDIRECT_STORE.getCode() + ":" + intermediate.getId();
        when(profitLogMapper.selectByUniqueKey(order.getOrderSn(), ProfitType.INDIRECT_STORE.getCode(), intermediate.getId()))
                .thenAnswer(invocation -> inserted.get() ? new ProfitLog() : null);
        when(profitLogMapper.insert(Mockito.any(ProfitLog.class))).thenAnswer(invocation -> {
            inserted.set(true);
            return 1;
        });

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        Runnable r = () -> {
            try {
                start.await();
                profitService.processJoinStoreProfit(order);
            } catch (Exception ignored) {}
            finally { done.countDown(); }
        };
        new Thread(r).start();
        new Thread(r).start();
        start.countDown();
        done.await(5, TimeUnit.SECONDS);

        verify(profitLogMapper, times(1)).insert(Mockito.any());
    }
}
