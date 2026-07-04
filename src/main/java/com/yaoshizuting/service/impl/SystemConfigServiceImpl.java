package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.SystemConfig;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.SystemConfigMapper;
import com.yaoshizuting.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String CACHE_PREFIX = "sysconfig:";
    private static final Map<String, String> DEFAULT_CONFIGS = Map.of(
            "WECHAT_APP_ID", "",
            "WECHAT_MCH_ID", "",
            "WECHAT_API_KEY", "",
            "WECHAT_API_V3_KEY", "",
            "WECHAT_NOTIFY_URL", "",
            "PAYMENT_CALLBACK_ALLOWED_IPS", "127.0.0.1,::1",
            "ALIPAY_PUBLIC_KEY", ""
    );

    private final SystemConfigMapper systemConfigMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String getConfigValue(String key) {
        String cacheKey = CACHE_PREFIX + key;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.toString();
        }

        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null || Integer.valueOf(0).equals(config.getStatus())) {
            String defaultValue = DEFAULT_CONFIGS.get(key);
            if (config == null && defaultValue != null) {
                redisTemplate.opsForValue().set(cacheKey, defaultValue, 1, TimeUnit.HOURS);
                return defaultValue;
            }
            throw new BusinessException(404, "配置不存在: " + key);
        }

        String value = config.getConfigValue();
        redisTemplate.opsForValue().set(cacheKey, value == null ? "" : value, 1, TimeUnit.HOURS);
        return value;
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        try {
            String value = getConfigValue(key);
            return value != null && !value.isBlank() ? value : defaultValue;
        } catch (BusinessException e) {
            return defaultValue;
        }
    }

    @Override
    public String getConfigValueMasked(String key) {
        String value = getConfigValue(key);
        if (!isSensitiveKey(key)) {
            return value;
        }
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String key, String value, String description) {
        if (value == null) {
            value = "";
        }

        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            config.setVersion(1);
            config.setStatus(1);
            systemConfigMapper.insert(config);
        } else {
            config.setConfigValue(value);
            if (description != null) {
                config.setDescription(description);
            }
            config.setVersion(config.getVersion() == null ? 1 : config.getVersion() + 1);
            systemConfigMapper.updateById(config);
        }

        redisTemplate.delete(CACHE_PREFIX + key);
        log.info("Updated system config: {}", key);
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toUpperCase();
        return normalized.contains("SECRET")
                || normalized.contains("PASSWORD")
                || normalized.contains("TOKEN")
                || normalized.contains("API_KEY")
                || normalized.contains("API_V3_KEY")
                || normalized.contains("APP_CODE")
                || normalized.contains("APPCODE")
                || normalized.contains("ACCESS_KEY")
                || normalized.contains("CUSTOMER")
                || normalized.contains("PUBLIC_KEY");
    }
}
