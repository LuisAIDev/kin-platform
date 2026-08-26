package com.kinplatform.kin.health.triage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de triaje (ADR-028).
 *
 * <p>Enlaza los switches operativos de la capacidad sin tocar el dominio:
 * master switch, límite de resultados, fuente de conocimiento y el
 * enriquecimiento del catálogo (fase profesional): NLP, fuente externa y
 * actualización bajo demanda. Defaults seguros: módulo habilitado, 5 condiciones
 * máximas y catálogo offline-first (bundle empaquetado).</p>
 *
 * <p>Variables de entorno soportadas: {@code KIN_HEALTH_TRIAGE_ENABLED},
 * {@code KIN_HEALTH_TRIAGE_MAX_CONDITIONS},
 * {@code KIN_HEALTH_TRIAGE_NLP_ENABLED},
 * {@code KIN_HEALTH_TRIAGE_CATALOG_EXTERNAL_ENABLED} y
 * {@code KIN_HEALTH_TRIAGE_CATALOG_BASE_URL}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.triage")
public class TriageProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Límite máximo de condiciones en el resultado (default 5). */
    private int maxConditions = 5;

    /** Fuente de conocimiento: {@code db} (default) o {@code file} (futuro). */
    private String dataSource = "db";

    /** Habilita el {@code SymptomExtractor} basado en OpenNLP (default true). */
    private boolean nlpEnabled = true;

    /**
     * Auto-actualización del catálogo desde fuentes externas (ADR-028, fase de
     * consolidación). Default {@code false}: la importación solo ocurre bajo
     * demanda vía endpoint admin. Los datos importados entran como PENDING y
     * deben pasar validación clínica antes de usarse en producción.
     */
    private boolean autoUpdate = false;

    /** Caché del catálogo (fase de producción): envuelve el repositorio JPA con
     * {@code @Cacheable} (en memoria por defecto; Redis si está habilitado). */
    private boolean catalogCacheEnabled = true;

    /** Configuración del enriquecimiento del catálogo desde fuentes externas. */
    private Catalog catalog = new Catalog();

    @Data
    public static class Catalog {

        /** Id de la fuente médica (KnowledgeSource). */
        private String sourceId = "health-catalog";

        /** Nombre legible de la fuente médica. */
        private String sourceName = "Health Catalog";

        /** Master switch de la API médica externa (default offline-first). */
        private boolean externalEnabled = false;

        /** URL base de la API médica externa (WHO/PubMed/dataset estructurado). */
        private String baseUrl = "";

        /** Recurso empaquetado del dataset CIE-10/WHO (offline-first). */
        private String bundledResource = "data/triage-catalog-extended.json";
    }
}
