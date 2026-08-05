package com.kinplatform.ai.guardrails;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de los guardrails del prompt (Fase 15 — capa de aplicación).
 */
@Configuration
public class AIGuardrailConfig {

    @Bean
    public PromptGuardrail promptGuardrail() {
        return new PromptGuardrail();
    }
}
