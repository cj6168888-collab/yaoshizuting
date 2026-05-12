package com.yaoshizuting.service.impl;

import com.yaoshizuting.entity.PolicyConfig;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.PolicyConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PolicyConfigServiceImplTest {

    @Mock
    private PolicyConfigMapper policyConfigMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PolicyConfigServiceImpl policyConfigService;

    @Test
    void getConfigValueReturnsCachedValueWithoutDatabaseLookup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("policy:direct_rate")).thenReturn("0.12");

        BigDecimal value = policyConfigService.getConfigValue("direct_rate");

        assertEquals(new BigDecimal("0.12"), value);
        verify(policyConfigMapper, never()).selectByKey(any());
    }

    @Test
    void getConfigValueLoadsEnabledConfigAndCachesIt() {
        PolicyConfig config = new PolicyConfig();
        config.setConfigValue(new BigDecimal("0.18"));
        config.setStatus(1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("policy:team_rate")).thenReturn(null);
        when(policyConfigMapper.selectByKey("team_rate")).thenReturn(config);

        BigDecimal value = policyConfigService.getConfigValue("team_rate");

        assertEquals(new BigDecimal("0.18"), value);
        verify(valueOperations).set("policy:team_rate", "0.18", 1, TimeUnit.HOURS);
    }

    @Test
    void getConfigValueRejectsMissingConfig() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("policy:missing_rate")).thenReturn(null);
        when(policyConfigMapper.selectByKey("missing_rate")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> policyConfigService.getConfigValue("missing_rate"));

        assertEquals("配置键不存在: missing_rate", exception.getMessage());
        verify(valueOperations, never()).set(any(), any(), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void getConfigValueRejectsDisabledConfig() {
        PolicyConfig config = new PolicyConfig();
        config.setStatus(0);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("policy:disabled_rate")).thenReturn(null);
        when(policyConfigMapper.selectByKey("disabled_rate")).thenReturn(config);

        assertThrows(BusinessException.class, () -> policyConfigService.getConfigValue("disabled_rate"));
        verify(valueOperations, never()).set(any(), any(), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void updateConfigInsertsNewConfigAndInvalidatesCache() {
        when(policyConfigMapper.selectByKey("new_rate")).thenReturn(null);

        policyConfigService.updateConfig("new_rate", new BigDecimal("0.25"), "新配置");

        ArgumentCaptor<PolicyConfig> captor = ArgumentCaptor.forClass(PolicyConfig.class);
        verify(policyConfigMapper).insert(captor.capture());
        PolicyConfig inserted = captor.getValue();
        assertEquals("new_rate", inserted.getConfigKey());
        assertEquals(new BigDecimal("0.25"), inserted.getConfigValue());
        assertEquals("新配置", inserted.getDescription());
        assertEquals(1, inserted.getVersion());
        assertEquals(1, inserted.getStatus());
        verify(redisTemplate).delete("policy:new_rate");
    }

    @Test
    void updateConfigUpdatesExistingConfigAndKeepsDescriptionWhenNull() {
        PolicyConfig config = new PolicyConfig();
        config.setConfigKey("existing_rate");
        config.setConfigValue(new BigDecimal("0.10"));
        config.setDescription("原配置");
        config.setVersion(2);
        config.setStatus(1);

        when(policyConfigMapper.selectByKey("existing_rate")).thenReturn(config);

        policyConfigService.updateConfig("existing_rate", new BigDecimal("0.30"), null);

        assertEquals(new BigDecimal("0.30"), config.getConfigValue());
        assertEquals("原配置", config.getDescription());
        assertEquals(3, config.getVersion());
        verify(policyConfigMapper).updateById(config);
        verify(redisTemplate).delete("policy:existing_rate");
    }
}
