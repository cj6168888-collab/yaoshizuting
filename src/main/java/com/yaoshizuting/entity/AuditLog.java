package com.yaoshizuting.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gyt_audit_log")
public class AuditLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;

    private String operation;

    private Long operatorId;

    private String operatorName;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String responseData;

    private Integer responseStatus;

    private Long executionTime;

    private String clientIp;

    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
