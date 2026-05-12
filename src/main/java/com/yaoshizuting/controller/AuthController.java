package com.yaoshizuting.controller;

import com.yaoshizuting.annotation.RateLimit;
import com.yaoshizuting.dto.ApiResponse;
import com.yaoshizuting.dto.LoginRequest;
import com.yaoshizuting.dto.LoginResponse;
import com.yaoshizuting.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "认证登录", description = "短信验证码发送、手机登录与邀请绑定入口")
public class AuthController {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_EXPIRE = 300;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @PostMapping("/login")
    @Operation(summary = "手机验证码登录", description = "使用手机号和短信验证码登录，首次登录会自动创建普通会员，可附带邀请码绑定上级。")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/sendCode/{mobile}")
    @RateLimit(limit = 1, period = 60, type = RateLimit.RateLimitType.IP)
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送 6 位验证码，同一 IP 60 秒内限流 1 次。")
    public ApiResponse<Void> sendCode(
            @Parameter(description = "中国大陆手机号", example = "13800138000") @PathVariable String mobile) {
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
