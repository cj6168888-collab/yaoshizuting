package com.yaoshizuting.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaoshizuting.dto.JoinStoreRequest;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.enums.UserRole;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.service.PolicyConfigService;
import com.yaoshizuting.utils.OrderNoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final PolicyConfigService policyConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createStoreJoinOrder(Long userId, JoinStoreRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getRole() >= UserRole.STORE.getCode()) {
            throw new BusinessException("您已经是店铺，无需重复加盟");
        }

        BigDecimal joinFee = policyConfigService.getConfigValue("STORE_JOIN_FEE");

        Order order = new Order();
        order.setOrderSn(OrderNoUtils.generateOrderSn());
        order.setUserId(userId);
        order.setOrderType(1);
        order.setAmount(joinFee);
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setPayMethod(request.getPayMethod() != null ? request.getPayMethod().toString() : "WECHAT");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);

        log.info("创建店铺加盟订单: orderSn={}, userId={}, amount={}", order.getOrderSn(), userId, joinFee);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createAgentJoinOrder(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getRole() >= UserRole.AGENT.getCode()) {
            throw new BusinessException("您已经是代理，无需重复加盟");
        }

        if (user.getRole() < UserRole.STORE.getCode()) {
            throw new BusinessException("您需要先成为店铺才能申请代理");
        }

        BigDecimal joinFee = policyConfigService.getConfigValue("AGENT_JOIN_FEE");

        Order order = new Order();
        order.setOrderSn(OrderNoUtils.generateOrderSn());
        order.setUserId(userId);
        order.setOrderType(2);
        order.setAmount(joinFee);
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setPayMethod("WECHAT");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);

        log.info("创建代理加盟订单: orderSn={}, userId={}, amount={}", order.getOrderSn(), userId, joinFee);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createPartnerJoinOrder(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getRole() >= UserRole.PARTNER.getCode()) {
            throw new BusinessException("您已经是合伙人，无需重复加盟");
        }

        if (user.getRole() < UserRole.STORE.getCode()) {
            throw new BusinessException("您需要先成为店铺才能申请合伙人");
        }

        BigDecimal joinFee = policyConfigService.getConfigValue("PARTNER_JOIN_FEE");

        Order order = new Order();
        order.setOrderSn(OrderNoUtils.generateOrderSn());
        order.setUserId(userId);
        order.setOrderType(3);
        order.setAmount(joinFee);
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setPayMethod("WECHAT");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);

        log.info("创建合伙人加盟订单: orderSn={}, userId={}, amount={}", order.getOrderSn(), userId, joinFee);
        return order;
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public Order getOrderByOrderSn(String orderSn) {
        return orderMapper.selectByOrderSn(orderSn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(String orderSn, Integer status, String transactionId) {
        Order order = orderMapper.selectByOrderSn(orderSn);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException("订单状态不可修改");
        }

        order.setStatus(status);
        if (transactionId != null) {
            order.setTransactionId(transactionId);
        }
        if (status == OrderStatus.PAID.getCode()) {
            order.setPayTime(LocalDateTime.now().toString());
        }
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.updateById(order);

        log.info("更新订单状态: orderSn={}, status={}", orderSn, status);
    }
}
