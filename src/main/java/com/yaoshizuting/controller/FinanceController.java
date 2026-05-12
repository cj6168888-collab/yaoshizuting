package com.yaoshizuting.controller;

import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.WalletResponse;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final ProfitService profitService;
    private final JwtUtils jwtUtils;

    @GetMapping("/wallet")
    public ApiResponse<WalletResponse> getWalletInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Long userId = getUserIdFromToken(authHeader);
        WalletResponse wallet = profitService.getWalletInfo(userId);
        return ApiResponse.success(wallet);
    }

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.yaoshizuting.exception.BusinessException(401, "请先登录");
        }
        String token = authHeader.substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }
}
