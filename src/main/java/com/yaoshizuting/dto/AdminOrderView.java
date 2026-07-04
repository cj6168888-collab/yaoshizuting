package com.yaoshizuting.dto;

import com.yaoshizuting.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminOrderView {
    private Long id;
    private String orderSn;
    private Long userId;
    private String userMobile;
    private String userNickname;
    private Integer orderType;
    private BigDecimal amount;
    private Integer status;
    private String payMethod;
    private String payTime;
    private String transactionId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminOrderView from(Order order) {
        AdminOrderView view = new AdminOrderView();
        view.setId(order.getId());
        view.setOrderSn(order.getOrderSn());
        view.setUserId(order.getUserId());
        view.setOrderType(order.getOrderType());
        view.setAmount(order.getAmount());
        view.setStatus(order.getStatus());
        view.setPayMethod(order.getPayMethod());
        view.setPayTime(order.getPayTime());
        view.setTransactionId(order.getTransactionId());
        view.setRemark(order.getRemark());
        view.setCreateTime(order.getCreateTime());
        view.setUpdateTime(order.getUpdateTime());
        return view;
    }
}
