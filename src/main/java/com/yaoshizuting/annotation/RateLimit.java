package com.yaoshizuting.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int value() default 100;
    
    int limit() default 100;
    
    int period() default 60;
    
    String message() default "请求过于频繁，请稍后再试";
    
    RateLimitType type() default RateLimitType.IP;
    
    enum RateLimitType {
        IP,
        USER,
        API
    }
}
