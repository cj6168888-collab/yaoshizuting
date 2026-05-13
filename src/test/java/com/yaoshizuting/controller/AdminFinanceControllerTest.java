package com.yaoshizuting.controller;

import com.yaoshizuting.entity.User;
import com.yaoshizuting.entity.ProfitLog;
import com.yaoshizuting.entity.Withdrawal;
import com.yaoshizuting.mapper.ProfitLogMapper;
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
    private static final AtomicInteger PROFIT_SEQ = new AtomicInteger(2000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WithdrawalMapper withdrawalMapper;

    @Autowired
    private ProfitLogMapper profitLogMapper;

    private String adminToken;
    private String userToken;
    private Long testUserId;
    private String testUserMobile;
    private String contributorMobile;
    private String withdrawalSn;
    private String orderSn;

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

        User contributor = new User();
        contributor.setMobile("139100" + MOBILE_SEQ.getAndIncrement());
        contributor.setNickname("财务贡献会员");
        contributor.setRole(1);
        contributor.setStatus(1);
        contributor.setBalance(new BigDecimal("300.00"));
        contributor.setTotalEarnings(new BigDecimal("500.00"));
        userMapper.insert(contributor);
        contributorMobile = contributor.getMobile();

        withdrawalSn = "WD-TEST-" + WITHDRAWAL_SEQ.getAndIncrement();
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(user.getId());
        withdrawal.setWithdrawSn(withdrawalSn);
        withdrawal.setAmount(new BigDecimal("200.00"));
        withdrawal.setFee(new BigDecimal("1.00"));
        withdrawal.setActualAmount(new BigDecimal("199.00"));
        withdrawal.setWithdrawType(1);
        withdrawal.setAccountNo("test-account");
        withdrawal.setAccountName("测试用户");
        withdrawal.setBankName("测试银行");
        withdrawal.setStatus(0);
        withdrawalMapper.insert(withdrawal);

        orderSn = "PROFIT-TEST-" + PROFIT_SEQ.getAndIncrement();
        ProfitLog profitLog = new ProfitLog();
        profitLog.setOrderSn(orderSn);
        profitLog.setReceiverId(user.getId());
        profitLog.setContributorId(contributor.getId());
        profitLog.setAmount(new BigDecimal("88.00"));
        profitLog.setType("DIRECT_STORE");
        profitLog.setStatus(1);
        profitLog.setRemark("测试分润");
        profitLogMapper.insert(profitLog);
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
    void withdrawals_WithInvalidStatus_ReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/admin/finance/withdrawals")
                .contextPath("/api")
                .param("status", "99")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("提现状态无效"));
    }

    @Test
    void profitLogs_WithAdminToken_ReturnsEnrichedProfitLogs() throws Exception {
        mockMvc.perform(get("/api/admin/finance/profit-logs")
                .contextPath("/api")
                .param("receiverId", testUserId.toString())
                .param("type", "DIRECT_STORE")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].orderSn").value(orderSn))
                .andExpect(jsonPath("$.data.records[0].receiverMobile").value(testUserMobile))
                .andExpect(jsonPath("$.data.records[0].receiverNickname").value("财务测试会员"))
                .andExpect(jsonPath("$.data.records[0].contributorMobile").value(contributorMobile))
                .andExpect(jsonPath("$.data.records[0].contributorNickname").value("财务贡献会员"));
    }

    @Test
    void exportWithdrawals_WithAdminToken_ReturnsCsv() throws Exception {
        mockMvc.perform(get("/api/admin/finance/withdrawals/export")
                .contextPath("/api")
                .param("status", "0")
                .param("userId", testUserId.toString())
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getHeader("Content-Disposition").contains("withdrawals.csv")))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsString().contains(withdrawalSn)))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsString().contains(testUserMobile)));
    }

    @Test
    void exportWithdrawals_WithInvalidStatus_ReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/admin/finance/withdrawals/export")
                .contextPath("/api")
                .param("status", "99")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("提现状态无效"));
    }

    @Test
    void exportProfitLogs_WithAdminToken_ReturnsCsv() throws Exception {
        mockMvc.perform(get("/api/admin/finance/profit-logs/export")
                .contextPath("/api")
                .param("receiverId", testUserId.toString())
                .param("type", "DIRECT_STORE")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getHeader("Content-Disposition").contains("profit-logs.csv")))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsString().contains(orderSn)))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsString().contains(contributorMobile)));
    }

    @Test
    void summary_WithUserToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/finance/summary")
                .contextPath("/api")
                .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }
}
