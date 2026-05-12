package com.yaoshizuting.task;

import com.yaoshizuting.entity.Order;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.service.ProfitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderScheduleTaskTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderService orderService;

    @Mock
    private ProfitService profitService;

    @Test
    void processPendingOrdersCancelsTimedOutOrders() {
        Order first = order("ORD-001", OrderStatus.PENDING.getCode(), 1);
        Order second = order("ORD-002", OrderStatus.PENDING.getCode(), 1);
        when(orderMapper.selectList(any())).thenReturn(List.of(first, second));

        newTask().processPendingOrders();

        assertEquals(OrderStatus.CANCELLED.getCode(), first.getStatus());
        assertEquals(OrderStatus.CANCELLED.getCode(), second.getStatus());
        assertNotNull(first.getUpdateTime());
        assertNotNull(second.getUpdateTime());
        verify(orderMapper).updateById(first);
        verify(orderMapper).updateById(second);
    }

    @Test
    void processPendingOrdersContinuesWhenSingleCancelFails() {
        Order failing = order("ORD-FAIL", OrderStatus.PENDING.getCode(), 1);
        Order succeeding = order("ORD-OK", OrderStatus.PENDING.getCode(), 1);
        when(orderMapper.selectList(any())).thenReturn(List.of(failing, succeeding));
        doThrow(new RuntimeException("update failed")).when(orderMapper).updateById(failing);

        newTask().processPendingOrders();

        assertEquals(OrderStatus.CANCELLED.getCode(), failing.getStatus());
        assertEquals(OrderStatus.CANCELLED.getCode(), succeeding.getStatus());
        verify(orderMapper).updateById(failing);
        verify(orderMapper).updateById(succeeding);
    }

    @Test
    void processPendingOrdersDoesNothingWhenNoOrders() {
        when(orderMapper.selectList(any())).thenReturn(List.of());

        newTask().processPendingOrders();

        verify(orderMapper, never()).updateById(any());
    }

    @Test
    void dailySettlementProcessesPaidStoreJoinOrders() {
        Order first = order("ORD-PAID-1", OrderStatus.PAID.getCode(), 1);
        Order second = order("ORD-PAID-2", OrderStatus.PAID.getCode(), 1);
        when(orderMapper.selectList(any())).thenReturn(List.of(first, second));

        newTask().dailySettlement();

        verify(profitService).processJoinStoreProfit(first);
        verify(profitService).processJoinStoreProfit(second);
    }

    @Test
    void dailySettlementContinuesWhenSingleProfitProcessingFails() {
        Order failing = order("ORD-FAIL", OrderStatus.PAID.getCode(), 1);
        Order succeeding = order("ORD-OK", OrderStatus.PAID.getCode(), 1);
        when(orderMapper.selectList(any())).thenReturn(List.of(failing, succeeding));
        doThrow(new RuntimeException("profit failed")).when(profitService).processJoinStoreProfit(failing);

        newTask().dailySettlement();

        verify(profitService).processJoinStoreProfit(failing);
        verify(profitService).processJoinStoreProfit(succeeding);
    }

    private OrderScheduleTask newTask() {
        return new OrderScheduleTask(orderMapper, orderService, profitService);
    }

    private Order order(String orderSn, Integer status, Integer orderType) {
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setStatus(status);
        order.setOrderType(orderType);
        order.setCreateTime(LocalDateTime.now().minusHours(1));
        return order;
    }
}
