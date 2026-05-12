package com.yaoshizuting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gyt_withdrawal")
public class Withdrawal implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
