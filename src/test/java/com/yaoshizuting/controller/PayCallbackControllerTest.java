package com.yaoshizuting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.service.PaymentSignatureService;
import com.yaoshizuting.service.ProfitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PayCallbackControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ProfitService profitService;

    @Mock
    private PaymentSignatureService signatureService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PayCallbackController controller = new PayCallbackController(
                orderService,
                profitService,
                signatureService,
                new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void wechatNotify_WithValidPaidCallback_UpdatesOrderAndTriggersStoreProfit() throws Exception {
        String body = """
                {"out_trade_no":"ORD-PAID-001","transaction_id":"WX-TX-001","result_code":"SUCCESS"}
                """;
        Order order = buildOrder("ORD-PAID-001", 1, OrderStatus.PENDING.getCode());

        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyWechatSignature("sig", body.trim(), "ts", "nonce")).thenReturn(true);
        when(orderService.getOrderByOrderSn("ORD-PAID-001")).thenReturn(order);

        mockMvc.perform(post("/v1/pay/wechat/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Wechatpay-Signature", "sig")
                .header("Wechatpay-Timestamp", "ts")
                .header("Wechatpay-Nonce", "nonce")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).updateOrderStatus("ORD-PAID-001", OrderStatus.PAID.getCode(), "WX-TX-001");
        verify(profitService).processJoinStoreProfit(order);
    }

    @Test
    void wechatNotify_WithAlreadyPaidOrder_SkipsStatusUpdateAndProfit() throws Exception {
        String body = """
                {"out_trade_no":"ORD-REPLAY-001","transaction_id":"WX-TX-002","result_code":"SUCCESS"}
                """;
        Order order = buildOrder("ORD-REPLAY-001", 2, OrderStatus.PAID.getCode());

        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyWechatSignature("sig", body.trim(), "ts", "nonce")).thenReturn(true);
        when(orderService.getOrderByOrderSn("ORD-REPLAY-001")).thenReturn(order);

        mockMvc.perform(post("/v1/pay/wechat/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Wechatpay-Signature", "sig")
                .header("Wechatpay-Timestamp", "ts")
                .header("Wechatpay-Nonce", "nonce")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService, never()).updateOrderStatus("ORD-REPLAY-001", OrderStatus.PAID.getCode(), "WX-TX-002");
        verifyNoInteractions(profitService);
    }

    @Test
    void wechatNotify_WithUnsuccessfulResult_DoesNotLookupOrder() throws Exception {
        String body = """
                {"out_trade_no":"ORD-FAILED-001","transaction_id":"WX-TX-003","result_code":"FAIL"}
                """;

        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyWechatSignature("sig", body.trim(), "ts", "nonce")).thenReturn(true);

        mockMvc.perform(post("/v1/pay/wechat/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Wechatpay-Signature", "sig")
                .header("Wechatpay-Timestamp", "ts")
                .header("Wechatpay-Nonce", "nonce")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verifyNoInteractions(orderService, profitService);
    }

    @Test
    void wechatNotify_WithBlockedIp_ReturnsBusinessErrorWithoutSignatureCheck() throws Exception {
        when(signatureService.isAllowedIP("10.0.0.8")).thenReturn(false);

        mockMvc.perform(post("/v1/pay/wechat/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.8");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Forbidden"));

        verify(signatureService, never()).verifyWechatSignature(null, "{}", null, null);
        verifyNoInteractions(orderService, profitService);
    }

    @Test
    void alipayNotify_WithValidPaidCallback_UpdatesOrderAndTriggersAgentProfit() throws Exception {
        Order order = buildOrder("ORD-ALI-001", 2, OrderStatus.PENDING.getCode());

        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyAlipaySignature(anyMap())).thenReturn(true);
        when(orderService.getOrderByOrderSn("ORD-ALI-001")).thenReturn(order);

        mockMvc.perform(post("/v1/pay/alipay/notify")
                .param("out_trade_no", "ORD-ALI-001")
                .param("trade_no", "ALI-TX-001")
                .param("trade_status", "TRADE_SUCCESS")
                .param("sign", "sig")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).updateOrderStatus("ORD-ALI-001", OrderStatus.PAID.getCode(), "ALI-TX-001");
        verify(profitService).processJoinAgentProfit(order);
    }

    @Test
    void alipayNotify_WithBlockedIp_ReturnsBusinessErrorWithoutSignatureCheck() throws Exception {
        when(signatureService.isAllowedIP("10.0.0.9")).thenReturn(false);

        mockMvc.perform(post("/v1/pay/alipay/notify")
                .param("out_trade_no", "ORD-ALI-BLOCKED")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.9");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Forbidden"));

        verify(signatureService, never()).verifyAlipaySignature(anyMap());
        verifyNoInteractions(orderService, profitService);
    }

    @Test
    void alipayNotify_WithInvalidSignature_ReturnsBusinessError() throws Exception {
        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyAlipaySignature(anyMap())).thenReturn(false);

        mockMvc.perform(post("/v1/pay/alipay/notify")
                .param("out_trade_no", "ORD-ALI-SIG")
                .param("trade_status", "TRADE_SUCCESS")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Signature verification failed"));

        verifyNoInteractions(orderService, profitService);
    }

    @Test
    void alipayNotify_WithUnsuccessfulTradeStatus_DoesNotLookupOrder() throws Exception {
        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyAlipaySignature(anyMap())).thenReturn(true);

        mockMvc.perform(post("/v1/pay/alipay/notify")
                .param("out_trade_no", "ORD-ALI-WAIT")
                .param("trade_no", "ALI-TX-WAIT")
                .param("trade_status", "WAIT_BUYER_PAY")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verifyNoInteractions(orderService, profitService);
    }

    @Test
    void wechatNotify_WithInvalidPayload_DoesNotLookupOrder() throws Exception {
        String body = "not-json";

        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyWechatSignature("sig", body, "ts", "nonce")).thenReturn(true);

        mockMvc.perform(post("/v1/pay/wechat/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Wechatpay-Signature", "sig")
                .header("Wechatpay-Timestamp", "ts")
                .header("Wechatpay-Nonce", "nonce")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verifyNoInteractions(orderService, profitService);
    }

    @Test
    void wechatNotify_WithMissingOrder_ReturnsOrderNotFound() throws Exception {
        String body = """
                {"out_trade_no":"ORD-MISSING","transaction_id":"WX-TX-MISSING","result_code":"SUCCESS"}
                """;

        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyWechatSignature("sig", body.trim(), "ts", "nonce")).thenReturn(true);
        when(orderService.getOrderByOrderSn("ORD-MISSING")).thenReturn(null);

        mockMvc.perform(post("/v1/pay/wechat/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Wechatpay-Signature", "sig")
                .header("Wechatpay-Timestamp", "ts")
                .header("Wechatpay-Nonce", "nonce")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Order not found"));

        verify(orderService, never()).updateOrderStatus("ORD-MISSING", OrderStatus.PAID.getCode(), "WX-TX-MISSING");
        verifyNoInteractions(profitService);
    }

    @Test
    void wechatNotify_WithForwardedFor_UsesFirstClientIpAndTriggersPartnerProfit() throws Exception {
        String body = """
                {"out_trade_no":"ORD-WX-PARTNER","transaction_id":"WX-TX-PARTNER","result_code":"SUCCESS"}
                """;
        Order order = buildOrder("ORD-WX-PARTNER", 3, OrderStatus.PENDING.getCode());

        when(signatureService.isAllowedIP("203.0.113.10")).thenReturn(true);
        when(signatureService.verifyWechatSignature("sig", body.trim(), "ts", "nonce")).thenReturn(true);
        when(orderService.getOrderByOrderSn("ORD-WX-PARTNER")).thenReturn(order);

        mockMvc.perform(post("/v1/pay/wechat/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("X-Forwarded-For", "203.0.113.10, 10.0.0.2")
                .header("Wechatpay-Signature", "sig")
                .header("Wechatpay-Timestamp", "ts")
                .header("Wechatpay-Nonce", "nonce"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).updateOrderStatus("ORD-WX-PARTNER", OrderStatus.PAID.getCode(), "WX-TX-PARTNER");
        verify(profitService).processJoinPartnerProfit(order);
    }

    @Test
    void alipayNotify_WithRealIpHeader_UsesRealIpAndSkipsUnknownOrderType() throws Exception {
        Order order = buildOrder("ORD-ALI-UNKNOWN", 99, OrderStatus.PENDING.getCode());

        when(signatureService.isAllowedIP("198.51.100.8")).thenReturn(true);
        when(signatureService.verifyAlipaySignature(anyMap())).thenReturn(true);
        when(orderService.getOrderByOrderSn("ORD-ALI-UNKNOWN")).thenReturn(order);

        mockMvc.perform(post("/v1/pay/alipay/notify")
                .header("X-Real-IP", "198.51.100.8")
                .param("out_trade_no", "ORD-ALI-UNKNOWN")
                .param("trade_no", "ALI-TX-UNKNOWN")
                .param("trade_status", "TRADE_FINISHED")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).updateOrderStatus("ORD-ALI-UNKNOWN", OrderStatus.PAID.getCode(), "ALI-TX-UNKNOWN");
        verifyNoInteractions(profitService);
    }

    @Test
    void alipayNotify_WhenProfitDistributionFails_StillReturnsSuccess() throws Exception {
        Order order = buildOrder("ORD-ALI-PROFIT-FAIL", 3, OrderStatus.PENDING.getCode());

        when(signatureService.isAllowedIP("127.0.0.1")).thenReturn(true);
        when(signatureService.verifyAlipaySignature(anyMap())).thenReturn(true);
        when(orderService.getOrderByOrderSn("ORD-ALI-PROFIT-FAIL")).thenReturn(order);
        doThrow(new RuntimeException("profit failed")).when(profitService).processJoinPartnerProfit(order);

        mockMvc.perform(post("/v1/pay/alipay/notify")
                .param("out_trade_no", "ORD-ALI-PROFIT-FAIL")
                .param("trade_no", "ALI-TX-PROFIT-FAIL")
                .param("trade_status", "TRADE_SUCCESS")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService).updateOrderStatus("ORD-ALI-PROFIT-FAIL", OrderStatus.PAID.getCode(), "ALI-TX-PROFIT-FAIL");
        verify(profitService).processJoinPartnerProfit(order);
    }

    private Order buildOrder(String orderSn, Integer orderType, Integer status) {
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setOrderType(orderType);
        order.setStatus(status);
        return order;
    }
}
