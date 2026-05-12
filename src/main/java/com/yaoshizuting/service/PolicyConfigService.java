package com.yaoshizuting.service;

import java.math.BigDecimal;

public interface PolicyConfigService {

    BigDecimal getConfigValue(String key);

    void updateConfig(String key, BigDecimal value, String description);
}
