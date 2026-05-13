package com.yaoshizuting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaoshizuting.dto.AdminProfitLogView;
import com.yaoshizuting.dto.AdminWithdrawalView;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.User;
import com.yaoshizuting.entity.Withdrawal;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.ProfitLogMapper;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.mapper.WithdrawalMapper;
import com.yaoshizuting.utils.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceController {

    private final UserMapper userMapper;
    private final ProfitLogMapper profitLogMapper;
    private final WithdrawalMapper withdrawalMapper;

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        BigDecimal totalBalance = userMapper.sumBalance();
        BigDecimal totalEarnings = userMapper.sumTotalEarnings();
        BigDecimal pendingWithdrawals = withdrawalMapper.sumByStatus(0);
        BigDecimal completedWithdrawals = withdrawalMapper.sumByStatus(2);

        return ApiResponse.success(Map.of(
                "totalBalance", totalBalance,
                "totalEarnings", totalEarnings,
                "pendingWithdrawals", pendingWithdrawals,
                "completedWithdrawals", completedWithdrawals,
                "userCount", userMapper.selectCount(new LambdaQueryWrapper<User>()),
                "pendingWithdrawalCount", withdrawalMapper.selectCount(new LambdaQueryWrapper<Withdrawal>().eq(Withdrawal::getStatus, 0))
        ));
    }

    @GetMapping("/withdrawals")
    public ApiResponse<Map<String, Object>> withdrawals(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {

        validateWithdrawalStatus(status);
        LambdaQueryWrapper<Withdrawal> wrapper = buildWithdrawalWrapper(status, userId);

        Page<Withdrawal> result = withdrawalMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        List<AdminWithdrawalView> records = enrichWithdrawals(result.getRecords());
        return ApiResponse.success(Map.of(
                "records", records,
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/profit-logs")
    public ApiResponse<Map<String, Object>> profitLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) String type) {

        LambdaQueryWrapper<ProfitLog> wrapper = buildProfitLogWrapper(receiverId, type);

        Page<ProfitLog> result = profitLogMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        List<AdminProfitLogView> records = enrichProfitLogs(result.getRecords());
        return ApiResponse.success(Map.of(
                "records", records,
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/withdrawals/export")
    public ResponseEntity<byte[]> exportWithdrawals(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {

        validateWithdrawalStatus(status);
        List<AdminWithdrawalView> records = enrichWithdrawals(
                withdrawalMapper.selectList(buildWithdrawalWrapper(status, userId).last("LIMIT 5000")));
        byte[] csv = CsvExportUtils.toCsv(
                List.of("提现ID", "单号", "用户ID", "手机号", "昵称", "申请金额", "手续费", "到账金额", "账户名", "账号", "银行", "状态", "备注", "申请时间"),
                records.stream()
                        .map(item -> List.of(
                                item.getId(),
                                value(item.getWithdrawSn()),
                                value(item.getUserId()),
                                value(item.getUserMobile()),
                                value(item.getUserNickname()),
                                value(item.getAmount()),
                                value(item.getFee()),
                                value(item.getActualAmount()),
                                value(item.getAccountName()),
                                value(item.getAccountNo()),
                                value(item.getBankName()),
                                value(item.getStatus()),
                                value(item.getRemark()),
                                value(item.getCreateTime())
                        ))
                        .toList());

        return csvResponse("withdrawals.csv", csv);
    }

    @GetMapping("/profit-logs/export")
    public ResponseEntity<byte[]> exportProfitLogs(
            @RequestParam(required = false) Long receiverId,
            @RequestParam(required = false) String type) {

        List<AdminProfitLogView> records = enrichProfitLogs(
                profitLogMapper.selectList(buildProfitLogWrapper(receiverId, type).last("LIMIT 5000")));
        byte[] csv = CsvExportUtils.toCsv(
                List.of("流水ID", "订单号", "收款人ID", "收款人手机号", "收款人昵称", "贡献人ID", "贡献人手机号", "贡献人昵称", "金额", "类型", "状态", "备注", "时间"),
                records.stream()
                        .map(item -> List.of(
                                item.getId(),
                                value(item.getOrderSn()),
                                value(item.getReceiverId()),
                                value(item.getReceiverMobile()),
                                value(item.getReceiverNickname()),
                                value(item.getContributorId()),
                                value(item.getContributorMobile()),
                                value(item.getContributorNickname()),
                                value(item.getAmount()),
                                value(item.getType()),
                                value(item.getStatus()),
                                value(item.getRemark()),
                                value(item.getCreateTime())
                        ))
                        .toList());

        return csvResponse("profit-logs.csv", csv);
    }

    private LambdaQueryWrapper<Withdrawal> buildWithdrawalWrapper(Integer status, Long userId) {
        return new LambdaQueryWrapper<Withdrawal>()
                .eq(status != null, Withdrawal::getStatus, status)
                .eq(userId != null, Withdrawal::getUserId, userId)
                .orderByDesc(Withdrawal::getCreateTime);
    }

    private LambdaQueryWrapper<ProfitLog> buildProfitLogWrapper(Long receiverId, String type) {
        return new LambdaQueryWrapper<ProfitLog>()
                .eq(receiverId != null, ProfitLog::getReceiverId, receiverId)
                .eq(type != null && !type.isBlank(), ProfitLog::getType, type)
                .orderByDesc(ProfitLog::getCreateTime);
    }

    private void validateWithdrawalStatus(Integer status) {
        if (status != null && (status < 0 || status > 3)) {
            throw new BusinessException(400, "提现状态无效");
        }
    }

    private List<AdminWithdrawalView> enrichWithdrawals(List<Withdrawal> withdrawals) {
        Map<Long, User> users = loadUsers(withdrawals.stream()
                .map(Withdrawal::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        return withdrawals.stream()
                .map(withdrawal -> {
                    AdminWithdrawalView view = AdminWithdrawalView.from(withdrawal);
                    User user = users.get(withdrawal.getUserId());
                    if (user != null) {
                        view.setUserMobile(user.getMobile());
                        view.setUserNickname(user.getNickname());
                    }
                    return view;
                })
                .toList();
    }

    private List<AdminProfitLogView> enrichProfitLogs(List<ProfitLog> profitLogs) {
        Set<Long> userIds = profitLogs.stream()
                .flatMap(log -> java.util.stream.Stream.of(log.getReceiverId(), log.getContributorId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = loadUsers(userIds);

        return profitLogs.stream()
                .map(log -> {
                    AdminProfitLogView view = AdminProfitLogView.from(log);
                    User receiver = users.get(log.getReceiverId());
                    if (receiver != null) {
                        view.setReceiverMobile(receiver.getMobile());
                        view.setReceiverNickname(receiver.getNickname());
                    }
                    User contributor = users.get(log.getContributorId());
                    if (contributor != null) {
                        view.setContributorMobile(contributor.getMobile());
                        view.setContributorNickname(contributor.getNickname());
                    }
                    return view;
                })
                .toList();
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ResponseEntity<byte[]> csvResponse(String filename, byte[] csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}
