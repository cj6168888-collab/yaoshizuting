package com.yaoshizuting.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class StaticResourceConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicPattern = uploadProperties.getPublicPath().replaceAll("/+$", "") + "/**";
        String location = Path.of(uploadProperties.getRootPath()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(publicPattern).addResourceLocations(location);
    }
}
