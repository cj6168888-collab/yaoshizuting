package com.yaoshizuting.controller;

import com.yaoshizuting.annotation.AuditLog;
import com.yaoshizuting.dto.ApiResponse;
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
    public ApiResponse<String> updatePolicy(@RequestBody Map<String, Object> params) {
        String key = (String) params.get("configKey");
        BigDecimal value = new BigDecimal(params.get("configValue").toString());
        String description = (String) params.get("description");
        
        policyConfigService.updateConfig(key, value, description);
        return ApiResponse.success("配置更新成功");
    }

    @GetMapping("/{key}")
    public ApiResponse<Map<String, Object>> getPolicy(@PathVariable String key) {
        BigDecimal value = policyConfigService.getConfigValue(key);
        return ApiResponse.success(Map.of("configKey", key, "configValue", value));
    }
}
