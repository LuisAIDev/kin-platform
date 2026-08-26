package com.kinplatform.kin.health.dashboard.config;

import com.kinplatform.kin.health.dashboard.domain.CareRecommendationRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de dashboard de salud (ADR-030).
 *
 * <p>Declara el {@link CareRecommendationRegistry} (motor determinista de plan
 * de cuidado) y habilita {@link DashboardProperties}. El resto de beans
 * ({@code DashboardService}, {@code DashboardController}) se auto-descubren por
 * componente.</p>
 */
@Configuration
@EnableConfigurationProperties(DashboardProperties.class)
public class DashboardConfig {

    @Bean
    public CareRecommendationRegistry careRecommendationRegistry() {
        return CareRecommendationRegistry.defaults();
    }
}
