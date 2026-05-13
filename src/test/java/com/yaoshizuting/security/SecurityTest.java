package com.yaoshizuting.security;

import com.yaoshizuting.dto.LoginRequest;
import com.yaoshizuting.dto.LoginResponse;
import com.yaoshizuting.service.UserService;
import com.yaoshizuting.testing.TestDistributedLockConfig;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private UserService userService;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }

        LoginResponse userResponse = new LoginResponse();
        userResponse.setToken(jwtUtils.generateToken(1L, "13800138000", 1));
        userResponse.setRole(1);

        LoginResponse adminResponse = new LoginResponse();
        adminResponse.setToken(jwtUtils.generateToken(2L, "13900139000", 9));
        adminResponse.setRole(9);

        userToken = userResponse.getToken();
        adminToken = adminResponse.getToken();

        when(userService.login(any(LoginRequest.class))).thenReturn(userResponse);
    }

    @Test
    void testAdminEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/policy/STORE_JOIN_FEE").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAdminEndpoint_WithUserRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/policy/STORE_JOIN_FEE")
                .contextPath("/api")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("权限不足"));
    }

    @Test
    void testWithdrawalApprove_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(put("/api/withdrawal/approve")
                .contextPath("/api")
                .contentType("application/json")
                .content("{\"withdrawalId\":1,\"approved\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testWithdrawalComplete_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(put("/api/withdrawal/complete")
                .contextPath("/api")
                .contentType("application/json")
                .content("{\"withdrawalId\":1,\"transactionId\":\"TX123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPublicEndpoint_AuthController_Accessible() throws Exception {
        mockMvc.perform(post("/api/auth/sendCode/13800138000")
                .contextPath("/api")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.11");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthEndpoint_WithoutToken_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
