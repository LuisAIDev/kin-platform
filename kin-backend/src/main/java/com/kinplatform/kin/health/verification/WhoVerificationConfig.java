package com.kinplatform.kin.health.verification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de verificación clínica con la OMS (ADR-041).
 */
@Configuration
@EnableConfigurationProperties(WhoVerificationProperties.class)
public class WhoVerificationConfig {}
