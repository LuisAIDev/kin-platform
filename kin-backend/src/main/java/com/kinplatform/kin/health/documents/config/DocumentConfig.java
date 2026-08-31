package com.kinplatform.kin.health.documents.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del módulo de documentos clínicos (ADR-036).
 */
@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentConfig {}
