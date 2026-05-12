package com.yaoshizuting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaoshizuting.entity.PolicyConfig;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.PolicyConfigMapper;
import com.yaoshizuting.service.PolicyConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyConfigServiceImpl implements PolicyConfigService {

    private final PolicyConfigMapper policyConfigMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String POLICY_CACHE_PREFIX = "policy:";

    @Override
    public BigDecimal getConfigValue(String key) {
        String cacheKey = POLICY_CACHE_PREFIX + key;
        
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return new BigDecimal(cached.toString());
        }

        PolicyConfig config = policyConfigMapper.selectByKey(key);
        if (config == null || config.getStatus() == 0) {
            throw new BusinessException("配置键不存在: " + key);
        }

        redisTemplate.opsForValue().set(cacheKey, config.getConfigValue().toString(), 1, TimeUnit.HOURS);
        return config.getConfigValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String key, BigDecimal value, String description) {
        PolicyConfig config = policyConfigMapper.selectByKey(key);
        if (config == null) {
            config = new PolicyConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            config.setVersion(1);
            config.setStatus(1);
            policyConfigMapper.insert(config);
        } else {
            config.setConfigValue(value);
            if (description != null) {
                config.setDescription(description);
            }
            config.setVersion(config.getVersion() + 1);
            policyConfigMapper.updateById(config);
        }

        String cacheKey = POLICY_CACHE_PREFIX + key;
        redisTemplate.delete(cacheKey);
        log.info("更新政策配置: {} = {}", key, value);
    }
}
