package com.yaoshizuting.controller;

import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.Withdrawal;
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
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        Integer withdrawType = (Integer) params.get("withdrawType");
        String accountNo = (String) params.get("accountNo");
        String accountName = (String) params.get("accountName");
        String bankName = (String) params.get("bankName");
        
        Withdrawal withdrawal = withdrawalService.createWithdrawal(
                userId, amount, withdrawType, accountNo, accountName, bankName);
        
        return ApiResponse.success(withdrawal);
    }

    @PutMapping("/approve")
    public ApiResponse<String> approve(@RequestBody Map<String, Object> params) {
        Long withdrawalId = Long.parseLong(params.get("withdrawalId").toString());
        Boolean approved = (Boolean) params.get("approved");
        String remark = (String) params.get("remark");
        
        withdrawalService.approveWithdrawal(withdrawalId, approved, remark);
        return ApiResponse.success("审核完成");
    }

    @PutMapping("/complete")
    public ApiResponse<String> complete(@RequestBody Map<String, Object> params) {
        Long withdrawalId = Long.parseLong(params.get("withdrawalId").toString());
        String transactionId = (String) params.get("transactionId");
        
        withdrawalService.completeWithdrawal(withdrawalId, transactionId);
        return ApiResponse.success("打款完成");
    }

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.yaoshizuting.exception.BusinessException(401, "请先登录");
        }
        String token = authHeader.substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }
}
