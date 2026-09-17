package com.kinplatform.kin.health.telemedicine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de telemedicina (ADR-032).
 *
 * <p>{@link com.kinplatform.kin.health.telemedicine.adapter.ContentCipher}
 * es un {@code @Component} auto-descubierto por Spring. El {@code cryptoSecret}
 * se inyecta vía {@code @Value} y se valida en {@code @PostConstruct}.</p>
 */
@Configuration
@EnableConfigurationProperties(TelemedicineProperties.class)
public class TelemedicineConfig {
}
