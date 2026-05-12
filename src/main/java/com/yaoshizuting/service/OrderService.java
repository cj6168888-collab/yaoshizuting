package com.yaoshizuting.service;

import com.yaoshizuting.dto.JoinStoreRequest;
import com.yaoshizuting.entity.Order;

public interface OrderService {

    Order createStoreJoinOrder(Long userId, JoinStoreRequest request);

    Order createAgentJoinOrder(Long userId);

    Order createPartnerJoinOrder(Long userId);

    Order getOrderById(Long orderId);

    Order getOrderByOrderSn(String orderSn);

    void updateOrderStatus(String orderSn, Integer status, String transactionId);
}
