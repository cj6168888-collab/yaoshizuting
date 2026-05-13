package com.yaoshizuting.service;

import java.math.BigDecimal;
import java.util.List;

public interface PolicyConfigService {

    BigDecimal getConfigValue(String key);

    void updateConfig(String key, BigDecimal value, String description);

    List<String> getPolicyWarnings();
}
