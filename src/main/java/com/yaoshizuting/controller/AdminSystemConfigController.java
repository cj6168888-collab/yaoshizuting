package com.yaoshizuting.controller;

import com.yaoshizuting.annotation.AuditLog;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/system-config")
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;

    @PutMapping
    @AuditLog(module = "系统配置", operation = "更新系统配置")
    public ApiResponse<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> params) {
        String key = requiredString(params, "configKey", "配置键");
        String value = optionalString(params.get("configValue"));
        String description = optionalString(params.get("description"));

        systemConfigService.updateConfig(key, value, description);
        return ApiResponse.success(Map.of("message", "配置已保存"));
    }

    @GetMapping("/{key}")
    public ApiResponse<Map<String, Object>> getConfig(@PathVariable String key) {
        return ApiResponse.success(Map.of(
                "configKey", key,
                "configValue", systemConfigService.getConfigValueMasked(key)
        ));
    }

    private String requiredString(Map<String, Object> params, String key, String label) {
        if (params == null || params.get(key) == null || params.get(key).toString().isBlank()) {
            throw new BusinessException(400, label + "不能为空");
        }
        return params.get(key).toString().trim();
    }

    private String optionalString(Object value) {
        return value == null ? null : value.toString();
    }
}
