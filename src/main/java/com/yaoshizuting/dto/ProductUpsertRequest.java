package com.yaoshizuting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "后台商品创建或更新请求")
public class ProductUpsertRequest {

    @NotBlank(message = "商品名称不能为空")
    @Schema(description = "商品名称", example = "药师祖庭店铺加盟权益包", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productName;

    @Schema(description = "商品编码，建议唯一", example = "STORE_JOIN_001")
    private String productCode;

    @NotNull(message = "商品类型不能为空")
    @Schema(description = "商品类型：1仪器，2套盒，3单品", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer productType;

    @NotNull(message = "市场价不能为空")
    @DecimalMin(value = "0.00", message = "市场价不能为负数")
    @Schema(description = "市场价", example = "3980.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal marketPrice;

    @NotNull(message = "加盟价不能为空")
    @DecimalMin(value = "0.00", message = "加盟价不能为负数")
    @Schema(description = "加盟价", example = "2980.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal joinPrice;

    @DecimalMin(value = "0.00", message = "代理价不能为负数")
    @Schema(description = "代理价", example = "1980.00")
    private BigDecimal agentPrice;

    @DecimalMin(value = "0.00", message = "合伙人价不能为负数")
    @Schema(description = "合伙人价", example = "980.00")
    private BigDecimal partnerPrice;

    @Min(value = 0, message = "库存不能为负数")
    @Schema(description = "库存数量", example = "100")
    private Integer stock;

    @Schema(description = "计量单位", example = "套")
    private String unit;

    @Schema(description = "商品图片地址", example = "/uploads/products/store-join.png")
    private String image;

    @Schema(description = "商品说明", example = "包含店铺会员权益、培训资料和基础推广工具。")
    private String description;

    @Schema(description = "状态：0下架，1上架", example = "1")
    private Integer status;
}
