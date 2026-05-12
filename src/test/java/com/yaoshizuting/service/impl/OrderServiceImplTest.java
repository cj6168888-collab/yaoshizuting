package com.yaoshizuting.service.impl;

import com.yaoshizuting.dto.JoinStoreRequest;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.enums.UserRole;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.service.PolicyConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PolicyConfigService policyConfigService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createStoreJoinOrderCreatesPendingOrderWithConfiguredFee() {
        User user = buildUser(10L, UserRole.MEMBER.getCode());
        JoinStoreRequest request = new JoinStoreRequest();
        request.setPayMethod(2);

        when(userMapper.selectById(10L)).thenReturn(user);
        when(policyConfigService.getConfigValue("STORE_JOIN_FEE")).thenReturn(new BigDecimal("398.00"));

        Order order = orderService.createStoreJoinOrder(10L, request);

        assertEquals(10L, order.getUserId());
        assertEquals(1, order.getOrderType());
        assertEquals(new BigDecimal("398.00"), order.getAmount());
        assertEquals(OrderStatus.PENDING.getCode(), order.getStatus());
        assertEquals("2", order.getPayMethod());
        assertNotNull(order.getOrderSn());
        verify(orderMapper).insert(order);
    }

    @Test
    void createStoreJoinOrderRejectsExistingStore() {
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L, UserRole.STORE.getCode()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.createStoreJoinOrder(10L, new JoinStoreRequest()));

        assertEquals("您已经是店铺，无需重复加盟", exception.getMessage());
        verify(policyConfigService, never()).getConfigValue(any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void createAgentJoinOrderRequiresStoreRole() {
        when(userMapper.selectById(20L)).thenReturn(buildUser(20L, UserRole.MEMBER.getCode()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.createAgentJoinOrder(20L));

        assertEquals("您需要先成为店铺才能申请代理", exception.getMessage());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void createAgentJoinOrderCreatesPendingWechatOrder() {
        when(userMapper.selectById(20L)).thenReturn(buildUser(20L, UserRole.STORE.getCode()));
        when(policyConfigService.getConfigValue("AGENT_JOIN_FEE")).thenReturn(new BigDecimal("1980.00"));

        Order order = orderService.createAgentJoinOrder(20L);

        assertEquals(20L, order.getUserId());
        assertEquals(2, order.getOrderType());
        assertEquals(new BigDecimal("1980.00"), order.getAmount());
        assertEquals(OrderStatus.PENDING.getCode(), order.getStatus());
        assertEquals("WECHAT", order.getPayMethod());
        verify(orderMapper).insert(order);
    }

    @Test
    void createPartnerJoinOrderRejectsExistingPartner() {
        when(userMapper.selectById(30L)).thenReturn(buildUser(30L, UserRole.PARTNER.getCode()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.createPartnerJoinOrder(30L));

        assertEquals("您已经是合伙人，无需重复加盟", exception.getMessage());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void updateOrderStatusMarksPaidWithTransactionIdAndPayTime() {
        Order order = new Order();
        order.setOrderSn("ORD-TEST-001");
        order.setStatus(OrderStatus.PENDING.getCode());
        when(orderMapper.selectByOrderSn("ORD-TEST-001")).thenReturn(order);

        orderService.updateOrderStatus("ORD-TEST-001", OrderStatus.PAID.getCode(), "TX-001");

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateById(captor.capture());
        Order updated = captor.getValue();
        assertEquals(OrderStatus.PAID.getCode(), updated.getStatus());
        assertEquals("TX-001", updated.getTransactionId());
        assertNotNull(updated.getPayTime());
        assertNotNull(updated.getUpdateTime());
    }

    @Test
    void updateOrderStatusRejectsNonPendingOrder() {
        Order order = new Order();
        order.setOrderSn("ORD-PAID-001");
        order.setStatus(OrderStatus.PAID.getCode());
        when(orderMapper.selectByOrderSn("ORD-PAID-001")).thenReturn(order);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.updateOrderStatus("ORD-PAID-001", OrderStatus.COMPLETED.getCode(), null));

        assertEquals("订单状态不可修改", exception.getMessage());
        verify(orderMapper, never()).updateById(any());
    }

    private User buildUser(Long id, Integer role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
