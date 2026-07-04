package com.yaoshizuting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaoshizuting.dto.AdminOrderView;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.utils.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer orderType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        validateListFilters(status, orderType);
        List<Long> userIds = findUserIds(keyword);
        LambdaQueryWrapper<Order> wrapper = buildListWrapper(keyword, status, orderType, startDate, endDate, userIds);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);

        return ApiResponse.success(Map.of(
                "records", enrichUsers(result.getRecords()),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer orderType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        validateListFilters(status, orderType);
        List<Long> userIds = findUserIds(keyword);
        List<AdminOrderView> records = enrichUsers(
                orderMapper.selectList(buildListWrapper(keyword, status, orderType, startDate, endDate, userIds).last("LIMIT 5000")));
        byte[] csv = CsvExportUtils.toCsv(
                List.of("订单ID", "订单号", "会员ID", "手机号", "昵称", "订单类型", "金额", "状态", "支付方式", "支付时间", "交易号", "备注", "创建时间"),
                records.stream()
                        .map(item -> List.of(
                                value(item.getId()),
                                value(item.getOrderSn()),
                                value(item.getUserId()),
                                value(item.getUserMobile()),
                                value(item.getUserNickname()),
                                value(item.getOrderType()),
                                value(item.getAmount()),
                                value(item.getStatus()),
                                value(item.getPayMethod()),
                                value(item.getPayTime()),
                                value(item.getTransactionId()),
                                value(item.getRemark()),
                                value(item.getCreateTime())
                        ))
                        .toList());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    private LambdaQueryWrapper<Order> buildListWrapper(String keyword, Integer status, Integer orderType,
                                                       LocalDate startDate, LocalDate endDate, List<Long> userIds) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(status != null, Order::getStatus, status)
                .eq(orderType != null, Order::getOrderType, orderType)
                .ge(startDate != null, Order::getCreateTime, startDate != null ? startDate.atStartOfDay() : null)
                .lt(endDate != null, Order::getCreateTime, endDate != null ? endDate.plusDays(1).atStartOfDay() : null);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> {
                w.like(Order::getOrderSn, keyword);
                if (!userIds.isEmpty()) {
                    w.or().in(Order::getUserId, userIds);
                }
            });
        }

        return wrapper.orderByDesc(Order::getCreateTime).orderByDesc(Order::getId);
    }

    private List<Long> findUserIds(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .and(w -> w.like(User::getMobile, keyword).or().like(User::getNickname, keyword)))
                .stream()
                .map(User::getId)
                .toList();
    }

    private List<AdminOrderView> enrichUsers(List<Order> orders) {
        Set<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        return orders.stream()
                .map(order -> {
                    AdminOrderView view = AdminOrderView.from(order);
                    User user = users.get(order.getUserId());
                    if (user != null) {
                        view.setUserMobile(user.getMobile());
                        view.setUserNickname(user.getNickname());
                    }
                    return view;
                })
                .toList();
    }

    private void validateListFilters(Integer status, Integer orderType) {
        if (status != null && (status < 0 || status > 5)) {
            throw new BusinessException(400, "订单状态无效");
        }
        if (orderType != null && (orderType < 1 || orderType > 5)) {
            throw new BusinessException(400, "订单类型无效");
        }
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}
