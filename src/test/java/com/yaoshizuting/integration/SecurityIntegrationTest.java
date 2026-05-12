package com.yaoshizuting.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.yaoshizuting.testing.TestDistributedLockConfig;
import com.yaoshizuting.utils.JwtUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private JwtUtils jwtUtils;

    @org.junit.jupiter.api.BeforeEach
    void clearRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void testFullSecurityFlow() throws Exception {
        mockMvc.perform(get("/api/admin/policy/STORE_JOIN_FEE").contextPath("/api"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/sendCode/13800138000")
                .contextPath("/api")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.21");
                    return request;
                }))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contextPath("/api")
                .contentType("application/json")
                .content("{\"mobile\":\"13800138000\",\"code\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        mockMvc.perform(post("/api/auth/sendCode/13800138000")
                .contextPath("/api")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.21");
                    return request;
                }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testPaymentCallback_WithoutSignature_ReturnsError() throws Exception {
        mockMvc.perform(post("/api/v1/pay/wechat/notify")
                .contextPath("/api")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                })
                .contentType("application/json")
                .content("{\"out_trade_no\":\"ORDER123\",\"result_code\":\"SUCCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void testAdminEndpoint_WithInvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/policy/STORE_JOIN_FEE")
                .contextPath("/api")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testWithdrawalApproval_WithoutAdminRole_Returns403() throws Exception {
        String userToken = "Bearer " + jwtUtils.generateToken(1L, "13800138000", 1);

        mockMvc.perform(post("/api/withdrawal/approve")
                .contextPath("/api")
                .header("Authorization", userToken)
                .contentType("application/json")
                .content("{\"withdrawalId\":1,\"approved\":true}"))
                .andExpect(status().isForbidden());
    }
}
