package com.kinplatform.payment;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra {@link WompiProperties} como bean. Sin esta clase (o un
 * {@code @ConfigurationPropertiesScan}) el bean no existe y la inyección en
 * {@link WompiService} fallaría al arrancar.
 */
@Configuration
@EnableConfigurationProperties(WompiProperties.class)
public class WompiConfig {
}
