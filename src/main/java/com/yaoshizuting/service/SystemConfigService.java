package com.yaoshizuting.service;

public interface SystemConfigService {

    String getConfigValue(String key);

    String getConfigValue(String key, String defaultValue);

    String getConfigValueMasked(String key);

    void updateConfig(String key, String value, String description);
}
