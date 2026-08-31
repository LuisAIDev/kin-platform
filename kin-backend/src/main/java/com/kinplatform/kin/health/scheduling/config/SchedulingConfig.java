package com.kinplatform.kin.health.scheduling.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de agenda y disponibilidad (ADR-034).
 *
 * <p>Habilita {@link SchedulingProperties}; la programación ({@code @Scheduled})
 * ya está activa globalmente vía {@code FollowUpConfig} (ADR-033).</p>
 */
@Configuration
@EnableConfigurationProperties(SchedulingProperties.class)
public class SchedulingConfig {}
