package com.yaoshizuting.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yaoshizuting.enums.AccountStatus;
import com.yaoshizuting.enums.UserRole;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gyt_user")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String mobile;

    private String nickname;

    private String avatar;

    private Integer role;

    private Long parentId;

    private String treePath;

    private Integer agentCount;

    private Integer storeCount;

    private BigDecimal balance;

    private BigDecimal totalEarnings;

    private Integer status;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
