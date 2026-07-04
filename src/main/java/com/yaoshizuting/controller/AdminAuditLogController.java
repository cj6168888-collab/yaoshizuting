package com.yaoshizuting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.entity.AuditLog;
import com.yaoshizuting.mapper.AuditLogMapper;
import com.yaoshizuting.utils.CsvExportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Page<AuditLog> result = auditLogMapper.selectPage(
                new Page<>(page, Math.min(size, 100)),
                buildWrapper(module, operatorId, keyword, startDate, endDate)
        );

        return ApiResponse.success(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<AuditLog> records = auditLogMapper.selectList(
                buildWrapper(module, operatorId, keyword, startDate, endDate).last("LIMIT 5000")
        );
        byte[] csv = CsvExportUtils.toCsv(
                List.of("日志ID", "模块", "操作", "操作人ID", "操作人", "请求方式", "请求地址", "客户端IP", "创建时间", "请求参数"),
                records.stream()
                        .map(item -> List.of(
                                value(item.getId()),
                                value(item.getModule()),
                                value(item.getOperation()),
                                value(item.getOperatorId()),
                                value(item.getOperatorName()),
                                value(item.getRequestMethod()),
                                value(item.getRequestUrl()),
                                value(item.getClientIp()),
                                value(item.getCreateTime()),
                                value(item.getRequestParams())
                        ))
                        .toList()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }

    private LambdaQueryWrapper<AuditLog> buildWrapper(String module, Long operatorId, String keyword,
                                                      LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .eq(module != null && !module.isBlank(), AuditLog::getModule, module)
                .eq(operatorId != null, AuditLog::getOperatorId, operatorId)
                .ge(startDate != null, AuditLog::getCreateTime, startDate != null ? startDate.atStartOfDay() : null)
                .lt(endDate != null, AuditLog::getCreateTime, endDate != null ? endDate.plusDays(1).atStartOfDay() : null);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AuditLog::getOperation, keyword)
                    .or().like(AuditLog::getOperatorName, keyword)
                    .or().like(AuditLog::getRequestUrl, keyword)
                    .or().like(AuditLog::getRequestParams, keyword));
        }

        return wrapper.orderByDesc(AuditLog::getCreateTime).orderByDesc(AuditLog::getId);
    }

    private Object value(Object value) {
        return value == null ? "" : value;
    }
}
