package com.yaoshizuting.exception;

import com.yaoshizuting.aspect.RateLimitAspect;
import com.yaoshizuting.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusinessExceptionReturnsBusinessCodeAndMessage() {
        ApiResponse<Void> response = handler.handleBusinessException(new BusinessException(409, "duplicate"));

        assertEquals(409, response.getCode());
        assertEquals("duplicate", response.getMessage());
    }

    @Test
    void handleValidationExceptionJoinsFieldMessages() throws Exception {
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.addError(new FieldError("request", "mobile", "mobile required"));
        bindingResult.addError(new FieldError("request", "code", "code required"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                methodParameter(), bindingResult);

        ApiResponse<Void> response = handler.handleValidationException(exception);

        assertEquals(400, response.getCode());
        assertEquals("mobile required, code required", response.getMessage());
    }

    @Test
    void handleBindExceptionJoinsFieldMessages() {
        BindException exception = new BindException(new Object(), "request");
        exception.addError(new FieldError("request", "amount", "amount required"));
        exception.addError(new FieldError("request", "account", "account required"));

        ApiResponse<Void> response = handler.handleBindException(exception);

        assertEquals(400, response.getCode());
        assertEquals("amount required, account required", response.getMessage());
    }

    @Test
    void handleRateLimitExceededExceptionReturnsTooManyRequestsCode() {
        RateLimitAspect.RateLimitExceededException exception =
                new RateLimitAspect.RateLimitExceededException("too many requests");

        ApiResponse<Void> response = handler.handleRateLimitExceededException(exception);

        assertEquals(429, response.getCode());
        assertEquals("too many requests", response.getMessage());
    }

    @Test
    void handleMethodArgumentTypeMismatchExceptionReturnsBadRequestCode() throws Exception {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "parentId",
                methodParameter(),
                new NumberFormatException("bad number"));

        ApiResponse<Void> response = handler.handleMethodArgumentTypeMismatchException(exception);

        assertEquals(400, response.getCode());
        assertEquals("parentId格式无效", response.getMessage());
    }

    @Test
    void handleExceptionReturnsGenericError() {
        ApiResponse<Void> response = handler.handleException(new RuntimeException("boom"));

        assertEquals(500, response.getCode());
        assertNotNull(response.getMessage());
    }

    private MethodParameter methodParameter() throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void sampleMethod(String value) {
    }
}
