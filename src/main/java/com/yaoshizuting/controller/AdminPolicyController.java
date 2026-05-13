package com.yaoshizuting.controller;

import com.yaoshizuting.annotation.AuditLog;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.service.PolicyConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/admin/policy")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final PolicyConfigService policyConfigService;

    @PutMapping
    @AuditLog(module = "系统配置", operation = "更新政策配置")
    public ApiResponse<Map<String, Object>> updatePolicy(@RequestBody Map<String, Object> params) {
        String key = requiredString(params, "configKey", "配置键");
        BigDecimal value = requiredBigDecimal(params, "configValue", "配置值");
        String description = optionalString(params.get("description"));
        
        policyConfigService.updateConfig(key, value, description);
        return ApiResponse.success(Map.of(
                "message", "配置更新成功",
                "warnings", policyConfigService.getPolicyWarnings()));
    }

    @GetMapping("/{key}")
    public ApiResponse<Map<String, Object>> getPolicy(@PathVariable String key) {
        BigDecimal value = policyConfigService.getConfigValue(key);
        return ApiResponse.success(Map.of(
                "configKey", key,
                "configValue", value,
                "warnings", policyConfigService.getPolicyWarnings()));
    }

    private String requiredString(Map<String, Object> params, String key, String label) {
        Object value = requiredValue(params, key, label);
        return value.toString().trim();
    }

    private BigDecimal requiredBigDecimal(Map<String, Object> params, String key, String label) {
        Object value = requiredValue(params, key, label);
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, label + "格式无效");
        }
    }

    private Object requiredValue(Map<String, Object> params, String key, String label) {
        if (params == null || params.get(key) == null || params.get(key).toString().isBlank()) {
            throw new BusinessException(400, label + "不能为空");
        }
        return params.get(key);
    }

    private String optionalString(Object value) {
        return value == null ? null : value.toString();
    }
}
