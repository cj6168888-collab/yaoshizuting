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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyConfigServiceImpl implements PolicyConfigService {

    private final PolicyConfigMapper policyConfigMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String POLICY_CACHE_PREFIX = "policy:";
    private static final Map<String, BigDecimal> DEFAULT_CONFIGS = Map.ofEntries(
            Map.entry("STORE_JOIN_FEE", new BigDecimal("13960.00")),
            Map.entry("AGENT_JOIN_FEE", new BigDecimal("39800.00")),
            Map.entry("PARTNER_JOIN_FEE", new BigDecimal("99800.00")),
            Map.entry("STORE_REWARD_DIRECT", new BigDecimal("9000.00")),
            Map.entry("AGENT_REWARD_DIRECT", new BigDecimal("9000.00")),
            Map.entry("PARTNER_REWARD_DIRECT", new BigDecimal("9000.00")),
            Map.entry("STORE_DIRECT_REWARD_START_COUNT", new BigDecimal("2")),
            Map.entry("STORE_INDIRECT_REWARD_ENABLED", BigDecimal.ZERO),
            Map.entry("REWARD_INDIRECT", BigDecimal.ZERO),
            Map.entry("AGENT_REWARD_DIRECT_AGENT", new BigDecimal("16000.00")),
            Map.entry("PARTNER_REWARD_DIRECT_PARTNER", new BigDecimal("40000.00")),
            Map.entry("PARTNER_REWARD_DIRECT_AGENT", new BigDecimal("16000.00")),
            Map.entry("PARTNER_TEAM_MANAGEMENT", new BigDecimal("998.00")),
            Map.entry("PARTNER_TEAM_MANAGEMENT_START_COUNT", new BigDecimal("2")),
            Map.entry("PARTNER_TEAM_MANAGEMENT_END_COUNT", new BigDecimal("100")),
            Map.entry("PARTNER_MANAGE_FEE", new BigDecimal("39800.00")),
            Map.entry("HEADQUARTER_SUPPORT_FEE", new BigDecimal("9800.00")),
            Map.entry("PRODUCT_DISCOUNT", new BigDecimal("0.15")),
            Map.entry("CLOUD_WAREHOUSE_FEE", new BigDecimal("39.80")),
            Map.entry("WITHDRAWAL_FEE_RATE", new BigDecimal("0.005")),
            Map.entry("WITHDRAWAL_MIN_AMOUNT", new BigDecimal("100.00"))
    );

    @Override
    public BigDecimal getConfigValue(String key) {
        String cacheKey = POLICY_CACHE_PREFIX + key;
        
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return new BigDecimal(cached.toString());
        }

        PolicyConfig config = policyConfigMapper.selectByKey(key);
        if (config == null || config.getStatus() == 0) {
            BigDecimal defaultValue = DEFAULT_CONFIGS.get(key);
            if (config == null && defaultValue != null) {
                redisTemplate.opsForValue().set(cacheKey, defaultValue.toString(), 1, TimeUnit.HOURS);
                return defaultValue;
            }
            throw new BusinessException("配置键不存在: " + key);
        }

        redisTemplate.opsForValue().set(cacheKey, config.getConfigValue().toString(), 1, TimeUnit.HOURS);
        return config.getConfigValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String key, BigDecimal value, String description) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("配置值不能小于0");
        }

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

    @Override
    public List<String> getPolicyWarnings() {
        List<String> warnings = new ArrayList<>();

        BigDecimal storeJoinFee = getConfigValueOrDefault("STORE_JOIN_FEE");
        BigDecimal directStoreReward = max(
                getConfigValueOrDefault("STORE_REWARD_DIRECT"),
                getConfigValueOrDefault("AGENT_REWARD_DIRECT"),
                getConfigValueOrDefault("PARTNER_REWARD_DIRECT"));
        BigDecimal indirectStoreReward = getConfigValueOrDefault("STORE_INDIRECT_REWARD_ENABLED")
                .compareTo(BigDecimal.ZERO) > 0 ? getConfigValueOrDefault("REWARD_INDIRECT") : BigDecimal.ZERO;
        BigDecimal teamManagement = getConfigValueOrDefault("PARTNER_TEAM_MANAGEMENT");
        BigDecimal storeRewardTotal = directStoreReward.add(indirectStoreReward).add(teamManagement);
        if (storeRewardTotal.compareTo(storeJoinFee) > 0) {
            warnings.add("店铺加盟单笔最高分润 " + money(storeRewardTotal)
                    + " 已超过店铺加盟费 " + money(storeJoinFee) + "，请调整直推、间推或团队管理津贴。");
        }

        int directStart = getConfigValueOrDefault("STORE_DIRECT_REWARD_START_COUNT").intValue();
        if (directStart < 1) {
            warnings.add("直推起奖店铺数小于1，会导致奖励门槛无效。");
        }

        int teamStart = getConfigValueOrDefault("PARTNER_TEAM_MANAGEMENT_START_COUNT").intValue();
        int teamEnd = getConfigValueOrDefault("PARTNER_TEAM_MANAGEMENT_END_COUNT").intValue();
        if (teamStart < 1 || teamEnd < teamStart) {
            warnings.add("团队管理津贴起止店铺数不合理，请确保起始值大于等于1且不超过结束值。");
        }

        BigDecimal agentJoinFee = getConfigValueOrDefault("AGENT_JOIN_FEE");
        BigDecimal agentReward = max(
                getConfigValueOrDefault("AGENT_REWARD_DIRECT_AGENT"),
                getConfigValueOrDefault("PARTNER_MANAGE_FEE"));
        if (agentReward.compareTo(agentJoinFee) > 0) {
            warnings.add("代理加盟单笔最高分润 " + money(agentReward)
                    + " 已超过代理加盟费 " + money(agentJoinFee) + "。");
        }

        BigDecimal partnerJoinFee = getConfigValueOrDefault("PARTNER_JOIN_FEE");
        BigDecimal partnerReward = getConfigValueOrDefault("PARTNER_REWARD_DIRECT_PARTNER");
        if (partnerReward.compareTo(partnerJoinFee) > 0) {
            warnings.add("合伙人加盟直推奖励 " + money(partnerReward)
                    + " 已超过合伙人加盟费 " + money(partnerJoinFee) + "。");
        }

        return warnings;
    }

    private BigDecimal getConfigValueOrDefault(String key) {
        BigDecimal value = getConfigValue(key);
        return value == null ? DEFAULT_CONFIGS.getOrDefault(key, BigDecimal.ZERO) : value;
    }

    private BigDecimal max(BigDecimal first, BigDecimal... rest) {
        BigDecimal result = first;
        for (BigDecimal value : rest) {
            if (value.compareTo(result) > 0) {
                result = value;
            }
        }
        return result;
    }

    private String money(BigDecimal value) {
        return "¥" + value.stripTrailingZeros().toPlainString();
    }
}
