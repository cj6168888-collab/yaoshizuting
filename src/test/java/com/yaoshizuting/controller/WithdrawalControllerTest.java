package com.yaoshizuting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaoshizuting.entity.Withdrawal;
import com.yaoshizuting.exception.GlobalExceptionHandler;
import com.yaoshizuting.service.WithdrawalService;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WithdrawalControllerTest {

    @Mock
    private WithdrawalService withdrawalService;

    @Mock
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        WithdrawalController controller = new WithdrawalController(withdrawalService, jwtUtils);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void apply_WithBearerToken_ReturnsCreatedWithdrawal() throws Exception {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setWithdrawSn("WD-TEST-001");
        withdrawal.setAmount(new BigDecimal("200.00"));
        withdrawal.setFee(new BigDecimal("2.00"));
        withdrawal.setActualAmount(new BigDecimal("198.00"));
        withdrawal.setStatus(0);

        when(jwtUtils.getUserIdFromToken("withdraw-token")).thenReturn(10L);
        when(withdrawalService.createWithdrawal(
                10L,
                new BigDecimal("200.00"),
                1,
                "account-no",
                "account-name",
                "bank-name"))
                .thenReturn(withdrawal);

        mockMvc.perform(post("/withdrawal/apply")
                .header("Authorization", "Bearer withdraw-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "amount", "200.00",
                        "withdrawType", 1,
                        "accountNo", "account-no",
                        "accountName", "account-name",
                        "bankName", "bank-name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.withdrawSn").value("WD-TEST-001"))
                .andExpect(jsonPath("$.data.actualAmount").value(198.00));

        verify(withdrawalService).createWithdrawal(
                10L,
                new BigDecimal("200.00"),
                1,
                "account-no",
                "account-name",
                "bank-name");
    }

    @Test
    void apply_WithNumericFieldsAsStrings_ParsesAndDelegates() throws Exception {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setWithdrawSn("WD-TEST-002");
        withdrawal.setAmount(new BigDecimal("300.00"));

        when(jwtUtils.getUserIdFromToken("withdraw-token")).thenReturn(10L);
        when(withdrawalService.createWithdrawal(
                10L,
                new BigDecimal("300.00"),
                2,
                "account-no",
                "account-name",
                null))
                .thenReturn(withdrawal);

        mockMvc.perform(post("/withdrawal/apply")
                .header("Authorization", "Bearer withdraw-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "amount", "300.00",
                        "withdrawType", "2",
                        "accountNo", "account-no",
                        "accountName", "account-name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.withdrawSn").value("WD-TEST-002"));

        verify(withdrawalService).createWithdrawal(
                10L,
                new BigDecimal("300.00"),
                2,
                "account-no",
                "account-name",
                null);
    }

    @Test
    void apply_WithoutBearerToken_ReturnsBusinessUnauthorized() throws Exception {
        mockMvc.perform(post("/withdrawal/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("amount", "200.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));

        verifyNoInteractions(jwtUtils, withdrawalService);
    }

    @Test
    void approve_DelegatesToWithdrawalService() throws Exception {
        mockMvc.perform(put("/withdrawal/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "withdrawalId", 9,
                        "approved", true,
                        "remark", "通过"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("审核完成"));

        verify(withdrawalService).approveWithdrawal(9L, true, "通过");
    }

    @Test
    void approve_WithoutApproved_ReturnsBusinessBadRequest() throws Exception {
        mockMvc.perform(put("/withdrawal/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("withdrawalId", 9))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("审核结果不能为空"));

        verifyNoInteractions(withdrawalService);
    }

    @Test
    void complete_DelegatesToWithdrawalService() throws Exception {
        mockMvc.perform(put("/withdrawal/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "withdrawalId", 9,
                        "transactionId", "TX-001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("打款完成"));

        verify(withdrawalService).completeWithdrawal(9L, "TX-001");
    }
}
