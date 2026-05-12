package com.yaoshizuting.dto;

import com.yaoshizuting.entity.ProfitLog;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminProfitLogView {

    private Long id;
    private String orderSn;
    private Long receiverId;
    private String receiverMobile;
    private String receiverNickname;
    private Long contributorId;
    private String contributorMobile;
    private String contributorNickname;
    private BigDecimal amount;
    private String type;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AdminProfitLogView from(ProfitLog profitLog) {
        AdminProfitLogView view = new AdminProfitLogView();
        BeanUtils.copyProperties(profitLog, view);
        return view;
    }
}
