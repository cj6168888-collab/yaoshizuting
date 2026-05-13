package com.yaoshizuting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaoshizuting.testing.TestDistributedLockConfig;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
class AdminPolicyControllerTest {

    private static final AtomicInteger KEY_SEQUENCE = new AtomicInteger(1000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtUtils.generateToken(9L, "13800000000", 9);
        userToken = "Bearer " + jwtUtils.generateToken(1L, "13800138000", 1);
    }

    @Test
    void updatePolicy_WithAdminToken_CreatesConfig() throws Exception {
        String key = nextPolicyKey();

        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "configKey", key,
                        "configValue", new BigDecimal("299.00"),
                        "description", "后台政策测试配置"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("配置更新成功"))
                .andExpect(jsonPath("$.data.warnings").isArray());
    }

    @Test
    void getPolicy_WithAdminToken_ReturnsConfigValue() throws Exception {
        String key = nextPolicyKey();
        createPolicy(key, "88.50");

        mockMvc.perform(get("/api/admin/policy/{key}", key)
                .contextPath("/api")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.configKey").value(key))
                .andExpect(jsonPath("$.data.configValue").value(88.50))
                .andExpect(jsonPath("$.data.warnings").isArray());
    }

    @Test
    void updatePolicy_WhenStoreRewardExceedsJoinFee_ReturnsWarning() throws Exception {
        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "configKey", "STORE_REWARD_DIRECT",
                        "configValue", new BigDecimal("20000.00"),
                        "description", "后台政策测试配置"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.warnings[0]").exists());
    }

    @Test
    void updatePolicy_WithWhitespaceWrappedConfigValue_CreatesConfig() throws Exception {
        String key = nextPolicyKey();

        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "configKey", key,
                        "configValue", " 299.00 ",
                        "description", "后台政策测试配置"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.message").value("配置更新成功"));

        mockMvc.perform(get("/api/admin/policy/{key}", key)
                .contextPath("/api")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value(299.00));
    }

    @Test
    void updatePolicy_WithoutConfigKey_ReturnsBusinessError() throws Exception {
        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("configValue", new BigDecimal("199.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("配置键不能为空"));
    }

    @Test
    void updatePolicy_WithInvalidConfigValue_ReturnsBusinessError() throws Exception {
        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "configKey", nextPolicyKey(),
                        "configValue", "abc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("配置值格式无效"));
    }

    @Test
    void updatePolicy_WithMalformedJson_ReturnsBusinessError() throws Exception {
        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"configKey\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求体格式无效"));
    }

    @Test
    void updatePolicy_WithNegativeConfigValue_ReturnsBusinessError() throws Exception {
        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "configKey", nextPolicyKey(),
                        "configValue", new BigDecimal("-1.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("配置值不能小于0"));
    }

    @Test
    void getPolicy_WithMissingConfig_ReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/admin/policy/{key}", nextPolicyKey())
                .contextPath("/api")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("配置键不存在: ")));
    }

    @Test
    void updatePolicy_WithUserToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "configKey", nextPolicyKey(),
                        "configValue", new BigDecimal("199.00")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPolicy_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/policy/{key}", nextPolicyKey())
                .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    private void createPolicy(String key, String value) throws Exception {
        mockMvc.perform(put("/api/admin/policy")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "configKey", key,
                        "configValue", new BigDecimal(value),
                        "description", "后台政策测试配置"))))
                .andExpect(status().isOk());
    }

    private String nextPolicyKey() {
        return "ADMIN_POLICY_TEST_" + KEY_SEQUENCE.getAndIncrement();
    }
}
