package com.yaoshizuting.entity;

import com.baomidou.mybatisplus.annotation.*;
import org.apache.ibatis.type.JdbcType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gyt_product")
public class Product implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private String productCode;

    private Integer productType;

    private BigDecimal marketPrice;

    private BigDecimal joinPrice;

    private BigDecimal agentPrice;

    private BigDecimal partnerPrice;

    private Integer stock;

    private String unit;

    private String image;

    @TableField(jdbcType = JdbcType.VARCHAR)
    private String description;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
