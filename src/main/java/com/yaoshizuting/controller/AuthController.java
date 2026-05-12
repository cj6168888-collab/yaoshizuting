package com.yaoshizuting.controller;

import com.yaoshizuting.annotation.RateLimit;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.LoginRequest;
import com.yaoshizuting.dto.LoginResponse;
import com.yaoshizuting.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Random;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_EXPIRE = 300;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/sendCode/{mobile}")
    @RateLimit(limit = 1, period = 60, type = RateLimit.RateLimitType.IP)
    public ApiResponse<Void> sendCode(@PathVariable String mobile) {
        if (!isValidMobile(mobile)) {
            return ApiResponse.error(400, "手机号格式不正确");
        }
        
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        String cacheKey = SMS_CODE_PREFIX + mobile;
        redisTemplate.opsForValue().set(cacheKey, code, SMS_CODE_EXPIRE);
        
        log.info("发送验证码: mobile={}", mobile);
        return ApiResponse.success();
    }

    private boolean isValidMobile(String mobile) {
        return mobile != null && mobile.matches("^1[3-9]\\d{9}$");
    }
}
