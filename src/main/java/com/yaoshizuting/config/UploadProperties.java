package com.yaoshizuting.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String rootPath = "uploads";

    private String publicPath = "/uploads";

    private long maxImageSize = 5 * 1024 * 1024L;
}
