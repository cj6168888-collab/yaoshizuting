package com.yaoshizuting.integration;

import com.yaoshizuting.entity.Order;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.mapper.GytUserHierarchyMapper;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.dto.WalletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class EndToEndTestSeeder {
    @Autowired
    UserMapper userMapper;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    com.yaoshizuting.mapper.GytUserHierarchyMapper gytUserHierarchyMapper;

    @Autowired
    PolicyConfigService policyConfigService;

    @Autowired
    ProfitService profitService;

    @Transactional
    public User createPartner(String mobile) {
        User p = new User();
        p.setRole(3); // PARTNER
        p.setMobile(mobile);
        p.setPassword("pwd");
        userMapper.insert(p);
        return p;
    }

    @Transactional
    public User createStoreUnderParent(User parent, String mobile) {
        User s = new User();
        s.setRole(1); // STORE
        s.setMobile(mobile);
        s.setParentId(parent.getId());
        s.setTreePath("/0/" + parent.getId() + "/");
        userMapper.insert(s);
        return s;
    }

    @Transactional
    public User createAgentUnderParent(User parent, String mobile) {
        User a = new User();
        a.setRole(2); // AGENT
        a.setMobile(mobile);
        a.setParentId(parent.getId());
        a.setTreePath("/0/" + parent.getId() + "/" + a.getId() + "/");
        userMapper.insert(a);
        return a;
    }

    @Transactional
    public Order createPaidOrderForUser(User user, String orderSn, BigDecimal amount) {
        Order o = new Order();
        o.setOrderSn(orderSn);
        o.setUserId(user.getId());
        o.setOrderType(1);
        o.setAmount(amount);
        o.setStatus(1); // PAID
        o.setPayMethod("WECHAT");
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(o);
        return o;
    }
}
