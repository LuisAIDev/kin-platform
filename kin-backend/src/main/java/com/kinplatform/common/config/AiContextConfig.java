package com.kinplatform.common.config;

import com.kinplatform.ai.context.adapter.HeuristicContextAnalyzerAdapter;
import com.kinplatform.kin.context.ContextAnalyzerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiContextConfig {

    @Bean
    public ContextAnalyzerPort contextAnalyzerPort() {
        return new HeuristicContextAnalyzerAdapter();
    }
}
