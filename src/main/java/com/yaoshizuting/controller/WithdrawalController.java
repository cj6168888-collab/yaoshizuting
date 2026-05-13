package com.yaoshizuting.controller;

import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.Withdrawal;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.service.WithdrawalService;
import com.yaoshizuting.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/withdrawal")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final JwtUtils jwtUtils;

    @PostMapping("/apply")
    public ApiResponse<Withdrawal> apply(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> params) {
        
        Long userId = getUserIdFromToken(authHeader);
        BigDecimal amount = requiredBigDecimal(params, "amount", "提现金额");
        Integer withdrawType = requiredInteger(params, "withdrawType", "提现方式");
        String accountNo = optionalString(params.get("accountNo"));
        String accountName = optionalString(params.get("accountName"));
        String bankName = optionalString(params.get("bankName"));
        
        Withdrawal withdrawal = withdrawalService.createWithdrawal(
                userId, amount, withdrawType, accountNo, accountName, bankName);
        
        return ApiResponse.success(withdrawal);
    }

    @PutMapping("/approve")
    public ApiResponse<String> approve(@RequestBody Map<String, Object> params) {
        Long withdrawalId = requiredLong(params, "withdrawalId", "提现记录");
        Boolean approved = requiredBoolean(params, "approved", "审核结果");
        String remark = optionalString(params.get("remark"));
        
        withdrawalService.approveWithdrawal(withdrawalId, approved, remark);
        return ApiResponse.success("审核完成");
    }

    @PutMapping("/complete")
    public ApiResponse<String> complete(@RequestBody Map<String, Object> params) {
        Long withdrawalId = requiredLong(params, "withdrawalId", "提现记录");
        String transactionId = optionalString(params.get("transactionId"));
        
        withdrawalService.completeWithdrawal(withdrawalId, transactionId);
        return ApiResponse.success("打款完成");
    }

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }
        String token = authHeader.substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }

    private BigDecimal requiredBigDecimal(Map<String, Object> params, String key, String label) {
        Object value = requiredValue(params, key, label);
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, label + "格式无效");
        }
    }

    private Integer requiredInteger(Map<String, Object> params, String key, String label) {
        Object value = requiredValue(params, key, label);
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, label + "格式无效");
        }
    }

    private Long requiredLong(Map<String, Object> params, String key, String label) {
        Object value = requiredValue(params, key, label);
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, label + "格式无效");
        }
    }

    private Boolean requiredBoolean(Map<String, Object> params, String key, String label) {
        Object value = requiredValue(params, key, label);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if ("true".equalsIgnoreCase(value.toString())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.toString())) {
            return false;
        }
        throw new BusinessException(400, label + "格式无效");
    }

    private Object requiredValue(Map<String, Object> params, String key, String label) {
        if (params == null || params.get(key) == null || params.get(key).toString().isBlank()) {
            throw new BusinessException(400, label + "不能为空");
        }
        return params.get(key);
    }

    private String optionalString(Object value) {
        return value == null ? null : value.toString();
    }
}
