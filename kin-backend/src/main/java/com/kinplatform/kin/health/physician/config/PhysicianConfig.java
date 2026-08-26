package com.kinplatform.kin.health.physician.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de portal de médicos (ADR-031).
 *
 * <p>Habilita {@link PhysicianProperties}; el resto de beans
 * ({@code PhysicianService}, {@code PhysicianController},
 * {@code ClinicalAlertEventListener}) se auto-descubren por componente.</p>
 */
@Configuration
@EnableConfigurationProperties(PhysicianProperties.class)
public class PhysicianConfig {}
