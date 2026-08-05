package com.kinplatform.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String PRODUCTION_ORIGIN = "https://kin-platform.vercel.app";

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var originList = new ArrayList<>(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());
        // Garantía de producción: el frontend de Vercel nunca se pierde,
        // incluso si ALLOWED_ORIGINS está definido sin incluirlo.
        if (!originList.contains(PRODUCTION_ORIGIN)) {
            originList.add(PRODUCTION_ORIGIN);
        }
        var origins = originList.toArray(new String[0]);
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true);
    }
}
