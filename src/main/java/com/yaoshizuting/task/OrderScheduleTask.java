package com.yaoshizuting.task;

import com.yaoshizuting.entity.Order;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.service.ProfitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduleTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final ProfitService profitService;

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void processPendingOrders() {
        log.info("开始处理超时未支付订单...");
        
        List<Order> pendingOrders = orderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING.getCode())
                .le(Order::getCreateTime, LocalDateTime.now().minusMinutes(30))
        );
        
        for (Order order : pendingOrders) {
            try {
                order.setStatus(OrderStatus.CANCELLED.getCode());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("订单已自动取消: {}", order.getOrderSn());
            } catch (Exception e) {
                log.error("处理超时订单失败: {}", order.getOrderSn(), e);
            }
        }
        
        log.info("处理完成，共取消 {} 个订单", pendingOrders.size());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void dailySettlement() {
        log.info("开始执行每日财务结算...");
        
        List<Order> paidOrders = orderMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PAID.getCode())
                .eq(Order::getOrderType, 1)
        );
        
        for (Order order : paidOrders) {
            try {
                profitService.processJoinStoreProfit(order);
            } catch (Exception e) {
                log.error("处理订单分润失败: {}", order.getOrderSn(), e);
            }
        }
        
        log.info("每日财务结算完成，共处理 {} 个订单", paidOrders.size());
    }
}
