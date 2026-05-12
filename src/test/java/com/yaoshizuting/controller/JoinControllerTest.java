package com.yaoshizuting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaoshizuting.entity.Order;
import com.yaoshizuting.enums.OrderStatus;
import com.yaoshizuting.exception.GlobalExceptionHandler;
import com.yaoshizuting.service.OrderService;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JoinControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        JoinController controller = new JoinController(orderService, jwtUtils);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void joinStore_WithBearerToken_CreatesStoreOrder() throws Exception {
        Order order = buildOrder("ORD-STORE-001", 1);
        when(jwtUtils.getUserIdFromToken("token-store")).thenReturn(10L);
        when(orderService.createStoreJoinOrder(argThat(userId -> userId == 10L), argThat(req -> req.getPayMethod() == 1)))
                .thenReturn(order);

        mockMvc.perform(post("/join/store")
                .header("Authorization", "Bearer token-store")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("payMethod", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderSn").value("ORD-STORE-001"))
                .andExpect(jsonPath("$.data.orderType").value(1));

        verify(orderService).createStoreJoinOrder(argThat(userId -> userId == 10L), argThat(req -> req.getPayMethod() == 1));
    }

    @Test
    void joinStore_WithoutPayMethod_ReturnsValidationError() throws Exception {
        mockMvc.perform(post("/join/store")
                .header("Authorization", "Bearer token-store")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("支付方式不能为空"));

        verifyNoInteractions(jwtUtils, orderService);
    }

    @Test
    void joinAgent_WithBearerToken_CreatesAgentOrder() throws Exception {
        Order order = buildOrder("ORD-AGENT-001", 2);
        when(jwtUtils.getUserIdFromToken("token-agent")).thenReturn(20L);
        when(orderService.createAgentJoinOrder(20L)).thenReturn(order);

        mockMvc.perform(post("/join/agent")
                .header("Authorization", "Bearer token-agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderSn").value("ORD-AGENT-001"))
                .andExpect(jsonPath("$.data.orderType").value(2));

        verify(orderService).createAgentJoinOrder(20L);
    }

    @Test
    void joinPartner_WithBearerToken_CreatesPartnerOrder() throws Exception {
        Order order = buildOrder("ORD-PARTNER-001", 3);
        when(jwtUtils.getUserIdFromToken("token-partner")).thenReturn(30L);
        when(orderService.createPartnerJoinOrder(30L)).thenReturn(order);

        mockMvc.perform(post("/join/partner")
                .header("Authorization", "Bearer token-partner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderSn").value("ORD-PARTNER-001"))
                .andExpect(jsonPath("$.data.orderType").value(3));

        verify(orderService).createPartnerJoinOrder(30L);
    }

    @Test
    void joinAgent_WithoutBearerToken_ReturnsBusinessUnauthorized() throws Exception {
        mockMvc.perform(post("/join/agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));

        verifyNoInteractions(jwtUtils, orderService);
    }

    private Order buildOrder(String orderSn, Integer orderType) {
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setOrderType(orderType);
        order.setAmount(new BigDecimal("398.00"));
        order.setStatus(OrderStatus.PENDING.getCode());
        return order;
    }
}
