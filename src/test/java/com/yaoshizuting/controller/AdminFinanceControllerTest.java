package com.yaoshizuting.controller;

import com.yaoshizuting.entity.User;
import com.yaoshizuting.entity.Withdrawal;
import com.yaoshizuting.mapper.UserMapper;
import com.yaoshizuting.mapper.WithdrawalMapper;
import com.yaoshizuting.testing.TestDistributedLockConfig;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
class AdminFinanceControllerTest {

    private static final AtomicInteger MOBILE_SEQ = new AtomicInteger(2000);
    private static final AtomicInteger WITHDRAWAL_SEQ = new AtomicInteger(2000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WithdrawalMapper withdrawalMapper;

    private String adminToken;
    private String userToken;
    private Long testUserId;
    private String testUserMobile;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtUtils.generateToken(9L, "13800000000", 9);
        userToken = "Bearer " + jwtUtils.generateToken(1L, "13800138000", 1);

        User user = new User();
        user.setMobile("139000" + MOBILE_SEQ.getAndIncrement());
        user.setNickname("财务测试会员");
        user.setRole(1);
        user.setStatus(1);
        user.setBalance(new BigDecimal("1000.00"));
        user.setTotalEarnings(new BigDecimal("2000.00"));
        userMapper.insert(user);
        testUserId = user.getId();
        testUserMobile = user.getMobile();

        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(user.getId());
        withdrawal.setWithdrawSn("WD-TEST-" + WITHDRAWAL_SEQ.getAndIncrement());
        withdrawal.setAmount(new BigDecimal("200.00"));
        withdrawal.setFee(new BigDecimal("1.00"));
        withdrawal.setActualAmount(new BigDecimal("199.00"));
        withdrawal.setWithdrawType(1);
        withdrawal.setAccountNo("test-account");
        withdrawal.setAccountName("测试用户");
        withdrawal.setStatus(0);
        withdrawalMapper.insert(withdrawal);
    }

    @Test
    void summary_WithAdminToken_ReturnsFinanceSummary() throws Exception {
        mockMvc.perform(get("/api/admin/finance/summary")
                .contextPath("/api")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalBalance").exists())
                .andExpect(jsonPath("$.data.pendingWithdrawalCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void withdrawals_WithAdminToken_ReturnsPendingWithdrawals() throws Exception {
        mockMvc.perform(get("/api/admin/finance/withdrawals")
                .contextPath("/api")
                .param("status", "0")
                .param("userId", testUserId.toString())
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].userMobile").value(testUserMobile))
                .andExpect(jsonPath("$.data.records[0].userNickname").value("财务测试会员"));
    }

    @Test
    void summary_WithUserToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/finance/summary")
                .contextPath("/api")
                .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }
}
