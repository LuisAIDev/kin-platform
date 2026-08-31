package com.kinplatform.kin.health.aiassist.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kin.health.aiassist")
public class AIAssistProperties {

    private boolean enabled = true;
    private int maxTokens = 500;
    private double temperature = 0.2;
    private String model = "deepseek-v4-flash";
}
