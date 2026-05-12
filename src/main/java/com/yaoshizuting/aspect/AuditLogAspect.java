package com.yaoshizuting.aspect;

import com.yaoshizuting.entity.AuditLog;
import com.yaoshizuting.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterReturning(value = "@annotation(auditLog)", argNames = "joinPoint,auditLog")
    public void logAfterReturning(org.aspectj.lang.JoinPoint joinPoint, com.yaoshizuting.annotation.AuditLog auditLog) {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return;
            }

            AuditLog auditRecord = new AuditLog();
            auditRecord.setModule(auditLog.module());
            auditRecord.setOperation(auditLog.operation());
            auditRecord.setRequestMethod(request.getMethod());
            auditRecord.setRequestUrl(request.getRequestURI());
            auditRecord.setClientIp(getClientIp(request));
            auditRecord.setUserAgent(request.getHeader("User-Agent"));
            auditRecord.setCreateTime(LocalDateTime.now());

            Object userId = request.getAttribute("userId");
            if (userId != null) {
                auditRecord.setOperatorId(Long.parseLong(userId.toString()));
            }

            Object mobile = request.getAttribute("mobile");
            if (mobile != null) {
                auditRecord.setOperatorName(mobile.toString());
            }

            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String params = objectMapper.writeValueAsString(sanitizeArgs(args));
                auditRecord.setRequestParams(params);
            }

            saveLogAsync(auditRecord);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    private Object sanitizeArgs(Object[] args) {
        if (args.length == 1) {
            return sanitizeArg(args[0]);
        }
        return Arrays.stream(args).map(this::sanitizeArg).toList();
    }

    private Object sanitizeArg(Object arg) {
        if (arg instanceof MultipartFile file) {
            Map<String, Object> safeFile = new LinkedHashMap<>();
            safeFile.put("filename", file.getOriginalFilename());
            safeFile.put("contentType", file.getContentType());
            safeFile.put("size", file.getSize());
            return safeFile;
        }
        return arg;
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Async
    protected void saveLogAsync(AuditLog auditRecord) {
        try {
            auditLogMapper.insert(auditRecord);
        } catch (Exception e) {
            log.error("保存审计日志失败: {}", e.getMessage());
        }
    }
}
