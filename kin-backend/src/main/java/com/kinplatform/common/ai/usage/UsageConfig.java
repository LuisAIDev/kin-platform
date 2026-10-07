package com.kinplatform.common.ai.usage;

import com.kinplatform.platform.usage.CostEstimator;
import com.kinplatform.platform.usage.HeuristicCostEstimator;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado de la estimación de costo de IA (Fase 1). Los precios por millón de
 * tokens provienen de configuración ({@code deepseek.cost.*}); si no están
 * definidos quedan en cero y el gate de costo se desactiva con log explícito
 * (no se inventa el precio de DeepSeek).
 */
@Configuration
public class UsageConfig {

    @Bean
    public CostEstimator costEstimator(
            @Value("${deepseek.cost.input-per-1m:0}") BigDecimal inputPricePer1M,
            @Value("${deepseek.cost.output-per-1m:0}") BigDecimal outputPricePer1M,
            @Value("${kin.ai.max-output-tokens:4096}") long maxOutputTokens) {
        return new HeuristicCostEstimator(inputPricePer1M, outputPricePer1M, maxOutputTokens);
    }
}


