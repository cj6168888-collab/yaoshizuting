package com.yaoshizuting.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class WalletResponse {
    private BigDecimal balance;
    private BigDecimal totalEarnings;
    private BigDecimal totalWithdrawn;
    private BigDecimal pendingAmount;
    private List<ProfitLogDTO> recentLogs;

    @Data
    public static class ProfitLogDTO {
        private String orderSn;
        private String type;
        private String typeDesc;
        private BigDecimal amount;
        private String createTime;
        private String remark;
    }
}
