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
    void endToEnd_StoreJoinFlow_FirstDirectNoRewardSecondDirectRewardAndNoIndirectReward() {
        policyConfigService.updateConfig("STORE_REWARD_DIRECT", new BigDecimal("9000"), "direct reward for store join");
        policyConfigService.updateConfig("STORE_DIRECT_REWARD_START_COUNT", new BigDecimal("2"), "direct reward starts from second store");
        policyConfigService.updateConfig("STORE_INDIRECT_REWARD_ENABLED", BigDecimal.ZERO, "disable indirect store reward");
        policyConfigService.updateConfig("REWARD_INDIRECT", BigDecimal.ZERO, "indirect reward for store join");
        policyConfigService.updateConfig("PARTNER_TEAM_MANAGEMENT", new BigDecimal("998"), "team management fee");
        policyConfigService.updateConfig("PARTNER_TEAM_MANAGEMENT_START_COUNT", new BigDecimal("2"), "team management starts from second store");
        policyConfigService.updateConfig("PARTNER_TEAM_MANAGEMENT_END_COUNT", new BigDecimal("100"), "team management ends at 100 stores");

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

        User b = newStoreCandidate("13900000993", a, partner);
        Order firstOrder = paidStoreOrder("ORD-ET-END-STORE-001", b.getId());
        profitService.processJoinStoreProfit(firstOrder);

        long firstDirectRewardCount = profitLogMapper.selectByReceiverId(a.getId()).stream()
                .filter(p -> ProfitType.DIRECT_STORE.getCode().equals(p.getType()))
                .count();
        assertEquals(0L, firstDirectRewardCount);

        User c = newStoreCandidate("13900000994", a, partner);
        Order secondOrder = paidStoreOrder("ORD-ET-END-STORE-002", c.getId());
        profitService.processJoinStoreProfit(secondOrder);

        ProfitLog log = profitLogMapper.selectByReceiverId(a.getId()).stream()
                .filter(p -> ProfitType.DIRECT_STORE.getCode().equals(p.getType()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(log, "Direct store profit log should exist for Store A");
        assertEquals(BigDecimal.valueOf(9000).setScale(2), log.getAmount().setScale(2));

        long indirectRewardCount = profitLogMapper.selectByReceiverId(partner.getId()).stream()
                .filter(p -> ProfitType.INDIRECT_STORE.getCode().equals(p.getType()))
                .count();
        assertEquals(0L, indirectRewardCount);

        ProfitLog managementFee = profitLogMapper.selectByReceiverId(partner.getId()).stream()
                .filter(p -> ProfitType.TEAM_MANAGEMENT.getCode().equals(p.getType()))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(managementFee, "Partner team management fee should exist");
        assertEquals(new BigDecimal("998.00"), managementFee.getAmount().setScale(2));
    }

    private User newStoreCandidate(String mobile, User parentStore, User partner) {
        User user = new User();
        user.setRole(UserRole.MEMBER.getCode());
        user.setMobile(mobile);
        user.setParentId(parentStore.getId());
        user.setTreePath("/0/" + partner.getId() + "/" + parentStore.getId() + "/");
        userMapper.insert(user);
        return user;
    }

    private Order paidStoreOrder(String orderSn, Long userId) {
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setUserId(userId);
        order.setOrderType(1);
        order.setAmount(BigDecimal.valueOf(13960));
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayMethod("WECHAT");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }
}
