package com.yaoshizuting.dto;

import com.yaoshizuting.entity.Withdrawal;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminWithdrawalView {

    private Long id;
    private Long userId;
    private String userMobile;
    private String userNickname;
    private String withdrawSn;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal actualAmount;
    private Integer withdrawType;
    private String accountNo;
    private String accountName;
    private String bankName;
    private Integer status;
    private String remark;
    private String auditTime;
    private String completeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminWithdrawalView from(Withdrawal withdrawal) {
        AdminWithdrawalView view = new AdminWithdrawalView();
        BeanUtils.copyProperties(withdrawal, view);
        return view;
    }
}
