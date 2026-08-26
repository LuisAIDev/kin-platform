package com.kinplatform.kin.health.differential.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de diagnóstico diferencial (ADR-029).
 *
 * <p>Master switch, límite de ítems y configuración del catálogo (bundle
 * offline-first u API médica externa). Defaults seguros: habilitado y 5 ítems.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_DIFFERENTIAL_ENABLED},
 * {@code KIN_HEALTH_DIFFERENTIAL_MAX_ITEMS} y
 * {@code KIN_HEALTH_DIFFERENTIAL_CATALOG_*}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.differential")
public class DifferentialProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Límite máximo de ítems en el resultado (default 5). */
    private int maxItems = 5;

    /** Configuración del enriquecimiento del catálogo. */
    private Catalog catalog = new Catalog();

    @Data
    public static class Catalog {

        /** Id de la fuente médica (KnowledgeSource). */
        private String sourceId = "health-differential";

        /** Nombre legible de la fuente médica. */
        private String sourceName = "Health Differential Catalog";

        /** Master switch de la API médica externa (default offline-first). */
        private boolean externalEnabled = false;

        /** URL base de la API médica externa. */
        private String baseUrl = "";

        /** Recurso empaquetado del dataset (offline-first). */
        private String bundledResource = "data/differential-catalog.json";
    }
}
