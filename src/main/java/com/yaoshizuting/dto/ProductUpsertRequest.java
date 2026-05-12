package com.yaoshizuting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpsertRequest {

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    private String productCode;

    @NotNull(message = "商品类型不能为空")
    private Integer productType;

    @NotNull(message = "市场价不能为空")
    @DecimalMin(value = "0.00", message = "市场价不能为负数")
    private BigDecimal marketPrice;

    @NotNull(message = "加盟价不能为空")
    @DecimalMin(value = "0.00", message = "加盟价不能为负数")
    private BigDecimal joinPrice;

    @DecimalMin(value = "0.00", message = "代理价不能为负数")
    private BigDecimal agentPrice;

    @DecimalMin(value = "0.00", message = "合伙人价不能为负数")
    private BigDecimal partnerPrice;

    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    private String unit;

    private String image;

    private String description;

    private Integer status;
}
