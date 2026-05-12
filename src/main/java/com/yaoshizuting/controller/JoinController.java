package com.yaoshizuting.controller;

import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.JoinStoreRequest;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/join")
@RequiredArgsConstructor
public class JoinController {

    private final OrderService orderService;
    private final JwtUtils jwtUtils;

    @PostMapping("/store")
    public ApiResponse<Order> joinStore(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody JoinStoreRequest request) {
        
        Long userId = getUserIdFromToken(authHeader);
        Order order = orderService.createStoreJoinOrder(userId, request);
        return ApiResponse.success(order);
    }

    @PostMapping("/agent")
    public ApiResponse<Order> joinAgent(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Long userId = getUserIdFromToken(authHeader);
        Order order = orderService.createAgentJoinOrder(userId);
        return ApiResponse.success(order);
    }

    @PostMapping("/partner")
    public ApiResponse<Order> joinPartner(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Long userId = getUserIdFromToken(authHeader);
        Order order = orderService.createPartnerJoinOrder(userId);
        return ApiResponse.success(order);
    }

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.yaoshizuting.exception.BusinessException(401, "请先登录");
        }
        String token = authHeader.substring(7);
        return jwtUtils.getUserIdFromToken(token);
    }
}
