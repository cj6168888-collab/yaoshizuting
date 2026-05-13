package com.yaoshizuting.aspect;

import com.yaoshizuting.annotation.RateLimit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${rate-limit.default-limit:100}")
    private int defaultLimit;

    @Value("${rate-limit.default-period:60}")
    private int defaultPeriod;

    public RateLimitAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!enabled) {
            return joinPoint.proceed();
        }

        String key = generateKey(joinPoint, rateLimit);
        int limit = rateLimit.limit() > 0 ? rateLimit.limit() : rateLimit.value();
        if (limit <= 0) {
            limit = defaultLimit;
        }
        int period = rateLimit.period() > 0 ? rateLimit.period() : defaultPeriod;

        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == null) {
            redisTemplate.opsForValue().set(key, 1, period, TimeUnit.SECONDS);
            currentCount = 1L;
        } else if (currentCount == 1L) {
            redisTemplate.expire(key, period, TimeUnit.SECONDS);
        }

        if (currentCount > limit) {
            log.warn("Rate limit exceeded: key={}, count={}, limit={}", key, currentCount, limit);
            throw new RateLimitExceededException("请求过于频繁，请稍后再试");
        }

        log.debug("Rate limit check passed: key={}, count={}/{}", key, currentCount, limit);
        
        return joinPoint.proceed();
    }

    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String clientId = getClientId();
        String methodName = joinPoint.getSignature().toShortString();
        
        return switch (rateLimit.type()) {
            case IP -> "rate_limit:ip:" + clientId + ":" + methodName;
            case USER -> "rate_limit:user:" + getUserId() + ":" + methodName;
            case API -> "rate_limit:api:" + methodName;
        };
    }

    private String getClientId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest().getRemoteAddr() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getUserId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            Object userId = attributes != null ? attributes.getRequest().getAttribute("userId") : null;
            return userId != null ? userId.toString() : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
