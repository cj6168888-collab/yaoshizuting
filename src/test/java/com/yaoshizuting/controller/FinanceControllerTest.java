package com.yaoshizuting.controller;

import com.yaoshizuting.dto.WalletResponse;
import com.yaoshizuting.exception.GlobalExceptionHandler;
import com.yaoshizuting.service.ProfitService;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FinanceControllerTest {

    @Mock
    private ProfitService profitService;

    @Mock
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FinanceController controller = new FinanceController(profitService, jwtUtils);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getWalletInfo_WithBearerToken_ReturnsWallet() throws Exception {
        WalletResponse wallet = new WalletResponse();
        wallet.setBalance(new BigDecimal("1000.00"));
        wallet.setTotalEarnings(new BigDecimal("500.00"));
        wallet.setTotalWithdrawn(new BigDecimal("200.00"));
        wallet.setPendingAmount(new BigDecimal("50.00"));
        wallet.setRecentLogs(List.of());

        when(jwtUtils.getUserIdFromToken("wallet-token")).thenReturn(10L);
        when(profitService.getWalletInfo(10L)).thenReturn(wallet);

        mockMvc.perform(get("/finance/wallet")
                .header("Authorization", "Bearer wallet-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.balance").value(1000.00))
                .andExpect(jsonPath("$.data.totalEarnings").value(500.00))
                .andExpect(jsonPath("$.data.totalWithdrawn").value(200.00))
                .andExpect(jsonPath("$.data.pendingAmount").value(50.00));

        verify(profitService).getWalletInfo(10L);
    }

    @Test
    void getWalletInfo_WithoutBearerToken_ReturnsBusinessUnauthorized() throws Exception {
        mockMvc.perform(get("/finance/wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));

        verifyNoInteractions(jwtUtils, profitService);
    }
}
