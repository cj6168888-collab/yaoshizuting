package com.yaoshizuting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaoshizuting.annotation.AuditLog;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.ProductUpsertRequest;
import com.yaoshizuting.entity.Product;
import com.yaoshizuting.mapper.ProductMapper;
import com.yaoshizuting.service.FileStorageService;
import com.yaoshizuting.service.ProductService;
import com.yaoshizuting.utils.CsvExportUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/admin/product")
@RequiredArgsConstructor
@Tag(name = "后台商品管理", description = "商品分页查询、创建更新、上下架、图片上传与 CSV 导出")
public class AdminProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final ProductMapper productMapper;

    @GetMapping("/list")
    @Operation(summary = "分页查询商品", description = "支持按商品类型、上下架状态、商品名或编码筛选，分页大小最大 100。")
    public ApiResponse<Map<String, Object>> list(
            @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页数量，最大 100", example = "20") @RequestParam(defaultValue = "20") long size,
            @Parameter(description = "商品类型：1仪器，2套盒，3单品", example = "1") @RequestParam(required = false) Integer productType,
            @Parameter(description = "状态：0下架，1上架", example = "1") @RequestParam(required = false) Integer status,
            @Parameter(description = "商品名或编码关键字", example = "店铺") @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Product> wrapper = buildListWrapper(productType, status, keyword);
        Page<Product> result = productMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        return ApiResponse.success(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/export")
    @Operation(summary = "导出商品 CSV", description = "按当前筛选条件导出最多 5000 条商品数据。")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "商品类型：1仪器，2套盒，3单品", example = "1") @RequestParam(required = false) Integer productType,
            @Parameter(description = "状态：0下架，1上架", example = "1") @RequestParam(required = false) Integer status,
            @Parameter(description = "商品名或编码关键字", example = "店铺") @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Product> wrapper = buildListWrapper(productType, status, keyword)
                .last("LIMIT 5000");
        byte[] csv = CsvExportUtils.toCsv(
                List.of("商品ID", "商品名称", "商品编码", "类型", "市场价", "加盟价", "代理价", "合伙价", "库存", "单位", "状态", "更新时间"),
                productMapper.selectList(wrapper).stream()
                        .map(product -> List.of(
                                product.getId(),
                                product.getProductName(),
                                value(product.getProductCode()),
                                product.getProductType(),
                                product.getMarketPrice(),
                                product.getJoinPrice(),
                                value(product.getAgentPrice()),
                                value(product.getPartnerPrice()),
                                value(product.getStock()),
                                value(product.getUnit()),
                                product.getStatus(),
                                value(product.getUpdateTime())
                        ))
                        .toList());

        return csvResponse("products.csv", csv);
    }

    @PostMapping("/upload")
    @AuditLog(module = "商品管理", operation = "上传商品图片")
    @Operation(summary = "上传商品图片", description = "上传商品图片并返回可访问的图片 URL。")
    public ApiResponse<Map<String, Object>> uploadImage(
            @Parameter(description = "商品图片文件") @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeProductImage(file);
        return ApiResponse.success(Map.of("url", url));
    }

    @PostMapping
    @AuditLog(module = "商品管理", operation = "创建商品")
    @Operation(summary = "创建商品", description = "创建后台商品，商品类型决定其加盟、代理或合伙权益。")
    public ApiResponse<Product> create(@Valid @RequestBody ProductUpsertRequest request) {
        return ApiResponse.success(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @AuditLog(module = "商品管理", operation = "更新商品")
    @Operation(summary = "更新商品", description = "按商品 ID 更新商品基础信息、价格、库存、图片和上下架状态。")
    public ApiResponse<Product> update(
            @Parameter(description = "商品 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ProductUpsertRequest request) {
        return ApiResponse.success(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/status")
    @AuditLog(module = "商品管理", operation = "更新商品状态")
    @Operation(summary = "更新商品上下架状态", description = "请求体示例：{\"status\":1}，0 表示下架，1 表示上架。")
    public ApiResponse<Product> updateStatus(
            @Parameter(description = "商品 ID", example = "1") @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        return ApiResponse.success(productService.updateStatus(id, body.get("status")));
    }

    private LambdaQueryWrapper<Product> buildListWrapper(Integer productType, Integer status, String keyword) {
        return new LambdaQueryWrapper<Product>()
                .eq(productType != null, Product::getProductType, productType)
                .eq(status != null, Product::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Product::getProductName, keyword)
                        .or()
                        .like(Product::getProductCode, keyword))
                .orderByDesc(Product::getUpdateTime)
                .orderByDesc(Product::getId);
    }

    private ResponseEntity<byte[]> csvResponse(String filename, byte[] csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}
