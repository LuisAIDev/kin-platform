package com.kinplatform.kin.health.followup.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wiring del módulo de seguimiento de pacientes (ADR-033).
 *
 * <p>Habilita {@link FollowUpProperties} y la <strong>programación</strong>
 * ({@code @EnableScheduling}): activa los procesos programados del módulo
 * (vencimiento de tareas, recordatorios y alerta de evolución sin registrar).
 * Como efecto secundario deseado, también activa el {@code OutboxRelay}
 * ({@code @Scheduled}, ADR-026) que estaba definido pero sin programación
 * habilitada a nivel de aplicación.</p>
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(FollowUpProperties.class)
public class FollowUpConfig {}
