package com.kinplatform.common.ai.knowledge.adapter;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de adquisición de conocimiento externo (ADR-021).
 *
 * <p>Enlaza los switches operativos de la capacidad sin tocar el dominio
 * congelado (ADR-014): allowlist de dominios, master switch del adapter HTTP,
 * timeouts, límites, content-types, retries, lista de fuentes autorizadas y
 * fuente controlada de prueba. Defaults offline-first: allowlist vacía +
 * {@code external-enabled=false}.</p>
 *
 * <p>Variables de entorno soportadas (ver {@code .env.example}):
 * {@code KNOWLEDGE_EXTERNAL_ENABLED}, {@code KNOWLEDGE_ALLOWED_DOMAINS},
 * {@code KNOWLEDGE_HTTP_BASE_URL}, {@code KNOWLEDGE_TEST_SOURCE_ENABLED} y la
 * lista de fuentes {@code KIN_KNOWLEDGE_SOURCES_0_*} / perfil {@code staging}
 * ({@code application-staging.yml}).</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.knowledge")
public class KinKnowledgeProperties {

    /** Dominios de conexión permitidos (allowlist); vacío = offline-first. */
    private List<String> allowedDomains = List.of();

    /** Master switch del adapter HTTP real. */
    private boolean externalEnabled;

    /**
     * Modo sombra (ADR-025, Fase 1 del plan de activación): con {@code true} el
     * {@code KnowledgeStage} ejecuta el motor completo (consulta real, validación,
     * caché y métricas {@code kin.knowledge.adapter.*}) pero **no** escribe
     * {@code knowledgeResult} en el contexto → el {@code EnrichmentStage} recibe
     * vacío y el usuario no ve cambios. Default {@code false} (modo normal).
     */
    private boolean shadowEnabled;

    /** Configuración del adapter HTTP seguro (fuente única, formato items). */
    private Http http = new Http();

    /**
     * Lista de fuentes externas autorizadas (multi-fuente, ADR-021). Cada
     * fuente define su {@code format} (decoder específico) y columnas de hecho.
     * Vacío = solo se usa la fuente única {@code http} si está configurada.
     */
    private List<SourceConfig> sources = List.of();

    /** Fuente controlada de prueba (determinista, sin red). */
    private TestSource testSource = new TestSource();

    @Data
    public static class Http {
        private String sourceId = "external-http";
        private String sourceName = "External Knowledge";
        /** URL base de la fuente autorizada; requerida cuando está habilitado. */
        private String baseUrl = "";

        private Duration connectTimeout = Duration.ofSeconds(1);
        private Duration requestTimeout = Duration.ofSeconds(3);
        private long maxResponseBytes = 262_144L;
        private int maxRedirects = 3;
        private List<String> allowedContentTypes = List.of("application/json");
        private int retries = 1;
        private Duration retryBackoff = Duration.ofMillis(250);
        /** Solo dev/test: permite hosts loopback explícitos. Nunca en prod. */
        private boolean allowLoopback;
    }

    /** Formato de respuesta de una fuente externa (decoder específico). */
    public enum SourceFormat {
        /** Contrato JSON {@code {"items":[{content,url,publishedAt}]}} (default). */
        JSON_ITEMS,
        /** API v2 del Banco Mundial: {@code [página, [filas con indicator/country/date/value]}}. */
        WORLD_BANK_V2,
        /** SODA2 (Socrata, p. ej. datos.gov.co): array de filas con columnas. */
        SODA_JSON,
        /** INE España: {@code [{Nombre, Data:[{Anyo, Valor, Fecha}]}]}. */
        INE_JSON,
        /** SDMX-JSON (p. ej. ECB): {@code dataSets[0].series[key].observations} + estructura. */
        SDMX_JSON
    }

    @Data
    public static class SourceConfig {
        /** Identificador único de la fuente (p. ej. {@code worldbank-col}). */
        private String id = "";
        /** Nombre legible de la fuente. */
        private String name = "";
        /** Descripción opcional de la fuente (para la Matriz Maestra / operadores). */
        private String description = "";
        /** URL base con la consulta (se le añade {@code &q=<topic>}). */
        private String baseUrl = "";
        /** Formato/decoder de la respuesta. */
        private SourceFormat format = SourceFormat.JSON_ITEMS;
        /**
         * Parámetro que transporta el tema (p. ej. {@code q}); vacío = la URL de
         * la fuente es una consulta fija y no se anexa nada (requerido por SODA).
         */
        private String queryParam = "q";
        /** Columnas a incluir en cada hecho (SODA). */
        private List<String> factColumns = List.of();
        /** Página/landing de referencia para la URL de cada hecho (SODA). */
        private String landingPage = "";
        /** Ventana de frescura máxima para los hechos (default 24 h). */
        private Duration maxAge = Duration.ofHours(24);
        /**
         * Nivel de la allowlist global (ADR-023): {@code 1} = multilateral/global,
         * {@code 2} = nacional. Permite seleccionar fuentes por país en el futuro.
         */
        private int level = 1;
        /** Región/país de la fuente: {@code GLOBAL}, {@code COL}, {@code ESP}, {@code MEX}, ... */
        private String region = "GLOBAL";
        /**
         * Categorías de proyecto a las que aplica la fuente (ADR-024). Vacío =
         * fuente de contexto general (aplica a cualquier categoría). Una fuente
         * puede servir a varias categorías (p. ej. tasas de interés → FINTECH y
         * EMPRESARIAL).
         */
        private List<String> categories = List.of();
        /**
         * Habilita/deshabilita la fuente sin eliminarla (ADR-025). {@code false} =
         * se conserva en la configuración pero no se consulta.
         */
        private boolean enabled = true;
        /**
         * Prioridad de la fuente en la selección (ADR-025): mayor valor = se
         * consulta antes. Empate mantiene el orden de configuración (determinista).
         */
        private int priority;
    }

    @Data
    public static class TestSource {
        private boolean enabled;
        private String sourceId = "controlled-test";
        private String sourceName = "Controlled Test Source";
        private List<TestCandidate> candidates = List.of();
    }

    @Data
    public static class TestCandidate {
        private String content = "";
        private String url = "";
        private String category = "";
        private String sourceType = "official_public";
        /** ISO-8601; vacío = {@code now}. */
        private String publishedAt = "";
    }
}

