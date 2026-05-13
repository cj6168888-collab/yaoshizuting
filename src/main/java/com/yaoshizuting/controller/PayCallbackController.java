package com.yaoshizuting.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.service.PaymentSignatureService;
import com.yaoshizuting.service.ProfitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/pay")
@RequiredArgsConstructor
public class PayCallbackController {

    private final OrderService orderService;
    private final ProfitService profitService;
    private final PaymentSignatureService signatureService;
    private final ObjectMapper objectMapper;

    @PostMapping("/wechat/notify")
    public ApiResponse<Void> wechatNotify(HttpServletRequest request) {
        String clientIP = getClientIP(request);

        if (!signatureService.isAllowedIP(clientIP)) {
            log.warn("Wechat callback IP not allowed: {}", clientIP);
            return ApiResponse.error(403, "Forbidden");
        }

        String body = readRequestBody(request);
        String signature = request.getHeader("Wechatpay-Signature");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");

        if (!signatureService.verifyWechatSignature(signature, body, timestamp, nonce)) {
            log.error("Wechat signature verification failed");
            return ApiResponse.error(400, "Signature verification failed");
        }

        Map<String, Object> params = parseWechatNotify(body);
        String orderSn = stringValue(params.get("out_trade_no"));
        String transactionId = stringValue(params.get("transaction_id"));
        String resultCode = stringValue(params.get("result_code"));

        if (!"SUCCESS".equals(resultCode)) {
            log.warn("Wechat payment not successful: orderSn={}", orderSn);
            return ApiResponse.success();
        }

        return handlePaySuccess(orderSn, transactionId);
    }

    @PostMapping("/alipay/notify")
    public ApiResponse<Void> alipayNotify(HttpServletRequest request) {
        String clientIP = getClientIP(request);

        if (!signatureService.isAllowedIP(clientIP)) {
            log.warn("Alipay callback IP not allowed: {}", clientIP);
            return ApiResponse.error(403, "Forbidden");
        }

        Map<String, String> params = parseAlipayParams(request);

        if (!signatureService.verifyAlipaySignature(params)) {
            log.error("Alipay signature verification failed");
            return ApiResponse.error(400, "Signature verification failed");
        }

        String orderSn = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");

        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.warn("Alipay payment not successful: orderSn={}", orderSn);
            return ApiResponse.success();
        }

        return handlePaySuccess(orderSn, tradeNo);
    }

    private ApiResponse<Void> handlePaySuccess(String orderSn, String transactionId) {
        Order order = orderService.getOrderByOrderSn(orderSn);
        if (order == null) {
            log.error("Order not found: orderSn={}", orderSn);
            return ApiResponse.error(404, "Order not found");
        }

        if (order.getStatus() == OrderStatus.PAID.getCode()
                || order.getStatus() == OrderStatus.PROCESSING.getCode()
                || order.getStatus() == OrderStatus.COMPLETED.getCode()) {
            log.info("Order already paid or settled, skipping callback replay: orderSn={}, status={}", orderSn, order.getStatus());
            return ApiResponse.success();
        }

        orderService.updateOrderStatus(orderSn, OrderStatus.PAID.getCode(), transactionId);
        order.setStatus(OrderStatus.PAID.getCode());
        triggerProfitDistribution(order);

        log.info("Payment callback handled successfully: orderSn={}, transactionId={}", orderSn, transactionId);
        return ApiResponse.success();
    }

    private void triggerProfitDistribution(Order order) {
        try {
            switch (order.getOrderType()) {
                case 1 -> profitService.processJoinStoreProfit(order);
                case 2 -> profitService.processJoinAgentProfit(order);
                case 3 -> profitService.processJoinPartnerProfit(order);
                default -> log.warn("Unknown order type, skipping profit distribution: {}", order.getOrderType());
            }
        } catch (Exception e) {
            log.error("Profit distribution failed: orderSn={}", order.getOrderSn(), e);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String readRequestBody(HttpServletRequest request) {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } catch (IOException e) {
            log.error("Failed to read callback request body", e);
        }
        return body.toString();
    }

    private Map<String, Object> parseWechatNotify(String body) {
        if (body == null || body.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(body, new TypeReference<HashMap<String, Object>>() { });
        } catch (Exception e) {
            log.error("Failed to parse Wechat callback payload", e);
            return Collections.emptyMap();
        }
    }

    private Map<String, String> parseAlipayParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
