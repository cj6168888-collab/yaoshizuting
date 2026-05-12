package com.yaoshizuting.controller;

import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.LoginRequest;
import com.yaoshizuting.dto.LoginResponse;
import com.yaoshizuting.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userService, redisTemplate);
    }

    @Test
    void loginDelegatesToUserServiceAndReturnsResponse() {
        LoginRequest request = new LoginRequest();
        request.setMobile("13800138000");
        request.setCode("123456");
        request.setInviteCode("INV10001");

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken("jwt-token");
        loginResponse.setUserId(10001L);
        loginResponse.setRole(0);
        loginResponse.setMobile("13800138000");
        when(userService.login(request)).thenReturn(loginResponse);

        ApiResponse<LoginResponse> response = controller.login(request);

        assertEquals(200, response.getCode());
        assertEquals("操作成功", response.getMessage());
        assertSame(loginResponse, response.getData());
        verify(userService).login(request);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void sendCodeRejectsInvalidMobileWithoutRedisWrite() {
        ApiResponse<Void> response = controller.sendCode("12000138000");

        assertEquals(400, response.getCode());
        assertEquals("手机号格式不正确", response.getMessage());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void sendCodeStoresSixDigitCodeWithExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ApiResponse<Void> response = controller.sendCode("13800138000");

        assertEquals(200, response.getCode());
        assertEquals("操作成功", response.getMessage());
        verify(valueOperations).set(
                eq("sms:code:13800138000"),
                argThat(code -> code instanceof String value && value.matches("\\d{6}")),
                eq(300L));
    }
}
