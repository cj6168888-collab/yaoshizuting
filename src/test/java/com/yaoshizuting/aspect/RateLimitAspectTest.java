package com.yaoshizuting.aspect;

import com.yaoshizuting.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.aspectj.lang.Signature;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private RateLimit rateLimit;

    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RateLimitAspect(redisTemplate);
        ReflectionTestUtils.setField(aspect, "enabled", true);
        ReflectionTestUtils.setField(aspect, "defaultLimit", 100);
        ReflectionTestUtils.setField(aspect, "defaultPeriod", 60);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("testMethod()");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.setRequestURI("/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testEnforceRateLimit_UnderLimit_Passes() throws Throwable {
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.IP);
        when(valueOperations.increment(anyString())).thenReturn(2L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);
        
        assertEquals("success", result);
        verify(valueOperations).increment(anyString());
    }

    @Test
    void testEnforceRateLimit_ExceedsLimit_ThrowsException() throws Throwable {
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.IP);
        when(valueOperations.increment(anyString())).thenReturn(11L);

        assertThrows(RateLimitAspect.RateLimitExceededException.class, () -> {
            aspect.enforceRateLimit(joinPoint, rateLimit);
        });
    }

    @Test
    void testEnforceRateLimit_FirstRequest_SetsExpiry() throws Throwable {
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.IP);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);
        
        assertEquals("success", result);
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testEnforceRateLimit_WhenIncrementReturnsNull_SetsFallbackValueWithExpiry() throws Throwable {
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.IP);
        when(valueOperations.increment(anyString())).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).set(anyString(), eq(1), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testEnforceRateLimit_Disabled_PassesThrough() throws Throwable {
        ReflectionTestUtils.setField(aspect, "enabled", false);
        when(joinPoint.proceed()).thenReturn("success");
        
        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);
        
        assertEquals("success", result);
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    void testEnforceRateLimit_MultipleRequests_TracksCount() throws Throwable {
        when(rateLimit.limit()).thenReturn(3);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.IP);
        when(joinPoint.proceed()).thenReturn("success");

        for (int i = 1; i <= 3; i++) {
            when(valueOperations.increment(anyString())).thenReturn((long) i);
            Object result = aspect.enforceRateLimit(joinPoint, rateLimit);
            assertEquals("success", result);
        }

        when(valueOperations.increment(anyString())).thenReturn(4L);
        assertThrows(RateLimitAspect.RateLimitExceededException.class, () -> {
            aspect.enforceRateLimit(joinPoint, rateLimit);
        });
    }

    @Test
    void enforceRateLimitUsesValueWhenLimitIsNotSet() throws Throwable {
        when(rateLimit.limit()).thenReturn(0);
        when(rateLimit.value()).thenReturn(2);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.API);
        when(valueOperations.increment("rate_limit:api:testMethod()")).thenReturn(2L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).increment("rate_limit:api:testMethod()");
    }

    @Test
    void enforceRateLimitUsesDefaultLimitAndPeriod() throws Throwable {
        when(rateLimit.limit()).thenReturn(0);
        when(rateLimit.value()).thenReturn(0);
        when(rateLimit.period()).thenReturn(0);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.API);
        when(valueOperations.increment("rate_limit:api:testMethod()")).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).set("rate_limit:api:testMethod()", 1, 60L, TimeUnit.SECONDS);
    }

    @Test
    void enforceRateLimitBuildsUserKeyFromRequestAttribute() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.USER);
        when(valueOperations.increment("rate_limit:user:42:testMethod()")).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).increment("rate_limit:user:42:testMethod()");
    }

    @Test
    void enforceRateLimitBuildsAnonymousUserKeyWithoutAttribute() throws Throwable {
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.USER);
        when(valueOperations.increment("rate_limit:user:anonymous:testMethod()")).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).increment("rate_limit:user:anonymous:testMethod()");
    }

    @Test
    void enforceRateLimitBuildsUnknownIpKeyWithoutRequestContext() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.IP);
        when(valueOperations.increment("rate_limit:ip:unknown:testMethod()")).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).increment("rate_limit:ip:unknown:testMethod()");
    }

    @Test
    void enforceRateLimitBuildsAnonymousUserKeyWithoutRequestContext() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.USER);
        when(valueOperations.increment("rate_limit:user:anonymous:testMethod()")).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).increment("rate_limit:user:anonymous:testMethod()");
    }

    @Test
    void enforceRateLimitFallsBackToUnknownIpWhenRequestAttributesAreInvalid() throws Throwable {
        RequestContextHolder.setRequestAttributes(mock(RequestAttributes.class));
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.IP);
        when(valueOperations.increment("rate_limit:ip:unknown:testMethod()")).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).increment("rate_limit:ip:unknown:testMethod()");
    }

    @Test
    void enforceRateLimitFallsBackToAnonymousUserWhenRequestAttributesAreInvalid() throws Throwable {
        RequestContextHolder.setRequestAttributes(mock(RequestAttributes.class));
        when(rateLimit.limit()).thenReturn(10);
        when(rateLimit.period()).thenReturn(60);
        when(rateLimit.type()).thenReturn(RateLimit.RateLimitType.USER);
        when(valueOperations.increment("rate_limit:user:anonymous:testMethod()")).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.enforceRateLimit(joinPoint, rateLimit);

        assertEquals("success", result);
        verify(valueOperations).increment("rate_limit:user:anonymous:testMethod()");
    }
}
