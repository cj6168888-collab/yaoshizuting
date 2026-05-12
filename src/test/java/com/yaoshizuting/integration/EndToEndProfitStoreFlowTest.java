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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
public class EndToEndProfitStoreFlowTest {

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

    @Test
    void endToEnd_StoreJoinFlow_DirectReward() {
        policyConfigService.updateConfig("STORE_REWARD_DIRECT", new BigDecimal("9000"), "direct reward for store join");
        policyConfigService.updateConfig("REWARD_INDIRECT", new BigDecimal("6000"), "indirect reward for store join");

        User partner = new User();
        partner.setRole(UserRole.PARTNER.getCode());
        partner.setMobile("13900000991");
        userMapper.insert(partner);

        User a = new User();
        a.setRole(UserRole.STORE.getCode());
        a.setMobile("13900000992");
        a.setParentId(partner.getId());
        a.setTreePath("/0/" + partner.getId() + "/");
        userMapper.insert(a);

        User b = new User();
        b.setRole(UserRole.STORE.getCode());
        b.setMobile("13900000993");
        b.setParentId(a.getId());
        b.setTreePath("/0/" + partner.getId() + "/" + a.getId() + "/");
        userMapper.insert(b);

        Order order = new Order();
        order.setOrderSn("ORD-ET-END-STORE-001");
        order.setUserId(b.getId());
        order.setOrderType(1);
        order.setAmount(BigDecimal.valueOf(13960));
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayMethod("WECHAT");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        profitService.processJoinStoreProfit(order);

        ProfitLog log = profitLogMapper.selectByReceiverId(a.getId()).stream()
                .filter(p -> ProfitType.DIRECT_STORE.getCode().equals(p.getType()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(log, "Direct store profit log should exist for Store A");
        assertEquals(BigDecimal.valueOf(9000).setScale(2), log.getAmount().setScale(2));
    }
}
