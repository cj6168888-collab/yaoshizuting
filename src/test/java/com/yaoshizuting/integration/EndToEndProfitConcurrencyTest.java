package com.yaoshizuting.integration;

import com.yaoshizuting.entity.Order;
import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.enums.ProfitType;
import com.yaoshizuting.enums.UserRole;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.testing.TestDistributedLockConfig;
import com.yaoshizuting.testing.TestMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
public class EndToEndProfitConcurrencyTest {

    @Autowired
    private ProfitService profitService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProfitLogMapper profitLogMapper;

    @Autowired
    private PolicyConfigService policyConfigService;

    @AfterEach
    void cleanup() {
        TestMode.setMode(TestMode.Mode.STABLE);
    }

    @Test
    void endToEnd_Concurrency_SameOrderIdempotent() throws InterruptedException {
        TestMode.setMode(TestMode.Mode.CONCURRENT);

        policyConfigService.updateConfig("PARTNER_REWARD_DIRECT", new BigDecimal("9000"), "partner direct store reward");
        policyConfigService.updateConfig("REWARD_INDIRECT", new BigDecimal("6000"), "indirect store reward");
        policyConfigService.updateConfig("PARTNER_TEAM_MANAGEMENT", new BigDecimal("998"), "team management fee");

        User partner = new User();
        partner.setRole(UserRole.PARTNER.getCode());
        partner.setMobile("13900000111");
        partner.setStoreCount(1);
        userMapper.insert(partner);
        Long partnerId = partner.getId();

        User store = new User();
        store.setRole(UserRole.STORE.getCode());
        store.setMobile("13900000112");
        store.setParentId(partnerId);
        store.setTreePath("/0/" + partnerId + "/");
        userMapper.insert(store);

        Order order = new Order();
        order.setOrderSn("ORD-CONC-001");
        order.setUserId(store.getId());
        order.setOrderType(1);
        order.setAmount(new BigDecimal("13960"));
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayMethod("WECHAT");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    profitService.processJoinStoreProfit(order);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        List<ProfitLog> logs = profitLogMapper.selectByReceiverId(partnerId);
        long directStoreCount = logs.stream()
                .filter(log -> ProfitType.DIRECT_STORE.getCode().equals(log.getType()))
                .count();

        assertEquals(1, directStoreCount, "Same order should create only one direct profit log");
    }

    @Test
    void endToEnd_CrossOrder_SameReceiverIdempotent() throws InterruptedException {
        policyConfigService.updateConfig("PARTNER_REWARD_DIRECT", new BigDecimal("9000"), "partner direct store reward");
        policyConfigService.updateConfig("REWARD_INDIRECT", new BigDecimal("6000"), "indirect store reward");

        User partner = new User();
        partner.setRole(UserRole.PARTNER.getCode());
        partner.setMobile("13900000121");
        userMapper.insert(partner);
        Long partnerId = partner.getId();

        User storeA = new User();
        storeA.setRole(UserRole.STORE.getCode());
        storeA.setMobile("13900000122");
        storeA.setParentId(partnerId);
        storeA.setTreePath("/0/" + partnerId + "/");
        userMapper.insert(storeA);

        User storeB = new User();
        storeB.setRole(UserRole.STORE.getCode());
        storeB.setMobile("13900000123");
        storeB.setParentId(partnerId);
        storeB.setTreePath("/0/" + partnerId + "/");
        userMapper.insert(storeB);

        Order orderA = new Order();
        orderA.setOrderSn("ORD-CROSS-A-001");
        orderA.setUserId(storeA.getId());
        orderA.setOrderType(1);
        orderA.setAmount(new BigDecimal("13960"));
        orderA.setStatus(OrderStatus.PAID.getCode());
        orderA.setPayMethod("WECHAT");
        orderA.setCreateTime(LocalDateTime.now());
        orderA.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(orderA);

        Order orderB = new Order();
        orderB.setOrderSn("ORD-CROSS-B-001");
        orderB.setUserId(storeB.getId());
        orderB.setOrderType(1);
        orderB.setAmount(new BigDecimal("13960"));
        orderB.setStatus(OrderStatus.PAID.getCode());
        orderB.setPayMethod("WECHAT");
        orderB.setCreateTime(LocalDateTime.now());
        orderB.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(orderB);

        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                profitService.processJoinStoreProfit(orderA);
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                profitService.processJoinStoreProfit(orderB);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        List<ProfitLog> partnerLogs = profitLogMapper.selectByReceiverId(partnerId);
        long directStoreCount = partnerLogs.stream()
                .filter(log -> ProfitType.DIRECT_STORE.getCode().equals(log.getType()))
                .count();

        assertEquals(2, directStoreCount, "Different orders should each create a direct profit log");

        BigDecimal totalAmount = partnerLogs.stream()
                .filter(log -> ProfitType.DIRECT_STORE.getCode().equals(log.getType()))
                .map(ProfitLog::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("18000.00"), totalAmount.setScale(2), "Total direct reward should be 18000");
    }
}
