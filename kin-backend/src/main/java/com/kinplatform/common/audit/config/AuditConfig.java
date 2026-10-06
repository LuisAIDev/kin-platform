package com.kinplatform.common.audit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de auditoría (ADR-035).
 */
@Configuration
@EnableConfigurationProperties(AuditProperties.class)
public class AuditConfig {}

