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
    void createStoreJoinOrderUsesWechatWhenPayMethodMissing() {
        when(userMapper.selectById(10L)).thenReturn(buildUser(10L, UserRole.MEMBER.getCode()));
        when(policyConfigService.getConfigValue("STORE_JOIN_FEE")).thenReturn(new BigDecimal("398.00"));

        Order order = orderService.createStoreJoinOrder(10L, new JoinStoreRequest());

        assertEquals("WECHAT", order.getPayMethod());
        verify(orderMapper).insert(order);
    }

    @Test
    void createStoreJoinOrderRejectsMissingUser() {
        when(userMapper.selectById(10L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> orderService.createStoreJoinOrder(10L, new JoinStoreRequest()));

        verify(policyConfigService, never()).getConfigValue(any());
        verify(orderMapper, never()).insert(any());
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
    void createAgentJoinOrderRejectsMissingUser() {
        when(userMapper.selectById(20L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> orderService.createAgentJoinOrder(20L));

        verify(policyConfigService, never()).getConfigValue(any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void createAgentJoinOrderRejectsExistingAgent() {
        when(userMapper.selectById(20L)).thenReturn(buildUser(20L, UserRole.AGENT.getCode()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.createAgentJoinOrder(20L));

        assertEquals("您已经是代理，无需重复加盟", exception.getMessage());
        verify(policyConfigService, never()).getConfigValue(any());
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
    void createPartnerJoinOrderRejectsMissingUser() {
        when(userMapper.selectById(30L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> orderService.createPartnerJoinOrder(30L));

        verify(policyConfigService, never()).getConfigValue(any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void createPartnerJoinOrderRequiresStoreRole() {
        when(userMapper.selectById(30L)).thenReturn(buildUser(30L, UserRole.MEMBER.getCode()));

        assertThrows(
                BusinessException.class,
                () -> orderService.createPartnerJoinOrder(30L));

        verify(orderMapper, never()).insert(any());
    }

    @Test
    void createPartnerJoinOrderCreatesPendingWechatOrder() {
        when(userMapper.selectById(30L)).thenReturn(buildUser(30L, UserRole.AGENT.getCode()));
        when(policyConfigService.getConfigValue("PARTNER_JOIN_FEE")).thenReturn(new BigDecimal("9800.00"));

        Order order = orderService.createPartnerJoinOrder(30L);

        assertEquals(30L, order.getUserId());
        assertEquals(3, order.getOrderType());
        assertEquals(new BigDecimal("9800.00"), order.getAmount());
        assertEquals(OrderStatus.PENDING.getCode(), order.getStatus());
        assertEquals("WECHAT", order.getPayMethod());
        verify(orderMapper).insert(order);
    }

    @Test
    void getOrderByIdDelegatesToMapper() {
        Order order = new Order();
        order.setId(99L);
        when(orderMapper.selectById(99L)).thenReturn(order);

        assertEquals(order, orderService.getOrderById(99L));
    }

    @Test
    void getOrderByOrderSnDelegatesToMapper() {
        Order order = new Order();
        order.setOrderSn("ORD-QUERY-001");
        when(orderMapper.selectByOrderSn("ORD-QUERY-001")).thenReturn(order);

        assertEquals(order, orderService.getOrderByOrderSn("ORD-QUERY-001"));
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

    @Test
    void updateOrderStatusRejectsMissingOrder() {
        when(orderMapper.selectByOrderSn("ORD-MISSING")).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> orderService.updateOrderStatus("ORD-MISSING", OrderStatus.PAID.getCode(), null));

        verify(orderMapper, never()).updateById(any());
    }

    @Test
    void updateOrderStatusCanCancelWithoutTransactionOrPayTime() {
        Order order = new Order();
        order.setOrderSn("ORD-CANCEL-001");
        order.setStatus(OrderStatus.PENDING.getCode());
        when(orderMapper.selectByOrderSn("ORD-CANCEL-001")).thenReturn(order);

        orderService.updateOrderStatus("ORD-CANCEL-001", OrderStatus.CANCELLED.getCode(), null);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateById(captor.capture());
        Order updated = captor.getValue();
        assertEquals(OrderStatus.CANCELLED.getCode(), updated.getStatus());
        assertEquals(null, updated.getTransactionId());
        assertEquals(null, updated.getPayTime());
        assertNotNull(updated.getUpdateTime());
    }

    private User buildUser(Long id, Integer role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
