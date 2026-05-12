package com.yaoshizuting.controller;

import com.yaoshizuting.entity.User;
import com.yaoshizuting.mapper.UserMapper;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
class AdminUserControllerTest {

    private static final AtomicInteger MOBILE_SEQ = new AtomicInteger(1000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;

    private String adminToken;
    private String userToken;
    private User user;
    private User parentUser;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtUtils.generateToken(9L, "13800000000", 9);
        userToken = "Bearer " + jwtUtils.generateToken(1L, "13800138000", 1);

        parentUser = new User();
        parentUser.setMobile("139100" + MOBILE_SEQ.getAndIncrement());
        parentUser.setNickname("后台测试上级");
        parentUser.setRole(2);
        parentUser.setStatus(1);
        parentUser.setBalance(BigDecimal.ZERO);
        parentUser.setTotalEarnings(BigDecimal.ZERO);
        userMapper.insert(parentUser);

        user = new User();
        user.setMobile("139000" + MOBILE_SEQ.getAndIncrement());
        user.setNickname("后台测试会员");
        user.setRole(0);
        user.setParentId(parentUser.getId());
        user.setStatus(1);
        user.setBalance(BigDecimal.ZERO);
        user.setTotalEarnings(BigDecimal.ZERO);
        userMapper.insert(user);
    }

    @Test
    void listUsers_WithAdminToken_ReturnsUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .contextPath("/api")
                .param("keyword", user.getMobile())
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].parentMobile").value(parentUser.getMobile()))
                .andExpect(jsonPath("$.data.records[0].parentNickname").value("后台测试上级"));
    }

    @Test
    void listUsers_WithRoleAndStatusFilters_ReturnsMatchingUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .contextPath("/api")
                .param("keyword", user.getMobile())
                .param("role", "0")
                .param("status", "1")
                .param("size", "200")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].mobile").value(user.getMobile()))
                .andExpect(jsonPath("$.data.records[0].role").value(0))
                .andExpect(jsonPath("$.data.records[0].status").value(1));
    }

    @Test
    void exportUsers_WithAdminToken_ReturnsCsv() throws Exception {
        mockMvc.perform(get("/api/admin/users/export")
                .contextPath("/api")
                .param("keyword", user.getMobile())
                .param("role", "0")
                .param("status", "1")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getHeader("Content-Disposition").contains("users.csv")))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsString().contains(user.getMobile())))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsString().contains(parentUser.getMobile())));
    }

    @Test
    void updateUser_WithAdminToken_UpdatesRoleAndStatus() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}", user.getId())
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":2,\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.role").value(2))
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void updateUser_WithInvalidRole_ReturnsBusinessError() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}", user.getId())
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户角色无效"));
    }

    @Test
    void updateUser_WithInvalidStatus_ReturnsBusinessError() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}", user.getId())
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("账号状态无效"));
    }

    @Test
    void updateUser_WithMissingUser_ReturnsBusinessError() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}", 99999999L)
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }

    @Test
    void listUsers_WithUserToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .contextPath("/api")
                .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }
}
