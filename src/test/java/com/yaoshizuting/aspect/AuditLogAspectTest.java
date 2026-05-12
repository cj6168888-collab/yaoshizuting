package com.yaoshizuting.aspect;

import com.yaoshizuting.entity.AuditLog;
import com.yaoshizuting.mapper.AuditLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private AuditLogAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new AuditLogAspect(auditLogMapper);
    }

    @Test
    void testLogAfterReturning_WithValidRequest_SavesLog() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/policy");
        request.setMethod("PUT");
        request.setRemoteAddr("192.168.1.1");
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.setAttribute("userId", 1L);
        request.setAttribute("mobile", "13800138000");
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        com.yaoshizuting.annotation.AuditLog auditLogAnnotation = 
            mock(com.yaoshizuting.annotation.AuditLog.class);
        when(auditLogAnnotation.module()).thenReturn("系统配置");
        when(auditLogAnnotation.operation()).thenReturn("更新政策配置");
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{"test-params"});
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        aspect.logAfterReturning(joinPoint, auditLogAnnotation);

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(logCaptor.capture());
        
        AuditLog savedLog = logCaptor.getValue();
        assertEquals("系统配置", savedLog.getModule());
        assertEquals("更新政策配置", savedLog.getOperation());
        assertEquals(1L, savedLog.getOperatorId());
        assertEquals("13800138000", savedLog.getOperatorName());
        assertEquals("PUT", savedLog.getRequestMethod());
        assertEquals("/api/admin/policy", savedLog.getRequestUrl());
        assertEquals("192.168.1.1", savedLog.getClientIp());
        assertNotNull(savedLog.getCreateTime());
    }

    @Test
    void testLogAfterReturning_NoRequestContext_DoesNotSave() {
        RequestContextHolder.resetRequestAttributes();

        com.yaoshizuting.annotation.AuditLog auditLogAnnotation = 
            mock(com.yaoshizuting.annotation.AuditLog.class);

        aspect.logAfterReturning(joinPoint, auditLogAnnotation);

        verify(auditLogMapper, never()).insert(any());
    }

    @Test
    void testLogAfterReturning_WithXForwardedFor_ExtractsRealIP() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 70.41.3.18");
        request.setRequestURI("/api/test");
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        com.yaoshizuting.annotation.AuditLog auditLogAnnotation = 
            mock(com.yaoshizuting.annotation.AuditLog.class);
        
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        aspect.logAfterReturning(joinPoint, auditLogAnnotation);

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(logCaptor.capture());
        
        assertEquals("203.0.113.1, 70.41.3.18", logCaptor.getValue().getClientIp());
    }

    @Test
    void testLogAfterReturning_WithException_LogsError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        com.yaoshizuting.annotation.AuditLog auditLogAnnotation = 
            mock(com.yaoshizuting.annotation.AuditLog.class);
        
        when(joinPoint.getArgs()).thenThrow(new RuntimeException("Test error"));

        assertDoesNotThrow(() -> {
            aspect.logAfterReturning(joinPoint, auditLogAnnotation);
        });
    }

    @Test
    void testLogAfterReturning_WithMultipartFile_RecordsSafeMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/product/upload");
        request.setMethod("POST");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        com.yaoshizuting.annotation.AuditLog auditLogAnnotation =
            mock(com.yaoshizuting.annotation.AuditLog.class);
        when(auditLogAnnotation.module()).thenReturn("商品管理");
        when(auditLogAnnotation.operation()).thenReturn("上传商品图片");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "product.png",
            "image/png",
            new byte[] {1, 2, 3});
        when(joinPoint.getArgs()).thenReturn(new Object[]{file});
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        aspect.logAfterReturning(joinPoint, auditLogAnnotation);

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(logCaptor.capture());
        assertTrue(logCaptor.getValue().getRequestParams().contains("\"filename\":\"product.png\""));
        assertTrue(logCaptor.getValue().getRequestParams().contains("\"size\":3"));
    }
}
