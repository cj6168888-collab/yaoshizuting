package com.yaoshizuting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.OrderMapper;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.mapper.WithdrawalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final ProfitLogMapper profitLogMapper;
    private final WithdrawalMapper withdrawalMapper;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        BigDecimal totalRevenue = orderMapper.sumTotalRevenue();
        BigDecimal todayRevenue = orderMapper.sumTodayRevenue();
        BigDecimal pendingWithdrawals = withdrawalMapper.sumByStatus(0);
        BigDecimal todayProfit = profitLogMapper.sumTodayProfit();

        return ApiResponse.success(Map.of(
                "totalUsers", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)),
                "totalRevenue", totalRevenue == null ? BigDecimal.ZERO : totalRevenue,
                "todayRevenue", todayRevenue == null ? BigDecimal.ZERO : todayRevenue,
                "pendingWithdrawals", pendingWithdrawals == null ? BigDecimal.ZERO : pendingWithdrawals,
                "todayProfit", todayProfit == null ? BigDecimal.ZERO : todayProfit,
                "pendingWithdrawalCount", withdrawalMapper.countByStatus(0),
                "roleDistribution", userMapper.countByRole()
        ));
    }

    @GetMapping("/user-growth")
    public ApiResponse<List<Map<String, Object>>> userGrowth(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.success(userMapper.dailyNewUsers(normalizeDays(days)));
    }

    @GetMapping("/revenue-trend")
    public ApiResponse<List<Map<String, Object>>> revenueTrend(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.success(orderMapper.dailyRevenue(normalizeDays(days)));
    }

    @GetMapping("/profit-distribution")
    public ApiResponse<List<Map<String, Object>>> profitDistribution() {
        return ApiResponse.success(profitLogMapper.groupByType());
    }

    @GetMapping("/recent-orders")
    public ApiResponse<List<Map<String, Object>>> recentOrders(@RequestParam(defaultValue = "8") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return ApiResponse.success(orderMapper.recentOrders(safeLimit));
    }

    private int normalizeDays(int days) {
        return Math.max(1, Math.min(days, 365));
    }
}
