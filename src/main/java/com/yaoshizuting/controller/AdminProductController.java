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
public class AdminProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final ProductMapper productMapper;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Integer productType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

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
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Integer productType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

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
    public ApiResponse<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeProductImage(file);
        return ApiResponse.success(Map.of("url", url));
    }

    @PostMapping
    @AuditLog(module = "商品管理", operation = "创建商品")
    public ApiResponse<Product> create(@Valid @RequestBody ProductUpsertRequest request) {
        return ApiResponse.success(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @AuditLog(module = "商品管理", operation = "更新商品")
    public ApiResponse<Product> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpsertRequest request) {
        return ApiResponse.success(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/status")
    @AuditLog(module = "商品管理", operation = "更新商品状态")
    public ApiResponse<Product> updateStatus(
            @PathVariable Long id,
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
