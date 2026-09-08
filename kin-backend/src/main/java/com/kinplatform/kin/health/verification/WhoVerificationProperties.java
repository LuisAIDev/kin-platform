package com.kinplatform.kin.health.verification;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la verificación clínica con fuentes oficiales de la OMS
 * (ADR-041): ICD-API (Clasificación Internacional de Enfermedades, CIE-10/11)
 * y Global Health Observatory (GHO).
 *
 * <p>La ICD-API exige OAuth2 Client Credentials ({@code clientId}/
 * {@code clientSecret}) emitidos en https://icd.who.int/icdapi. Sin credenciales
 * el flujo degrada con elegancia: la IA avisa de que no pudo verificar contra la
 * base oficial y NO inventa códigos ni cifras.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_VERIFICATION_ENABLED},
 * {@code WHO_ICD_CLIENT_ID}, {@code WHO_ICD_CLIENT_SECRET}, etc.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.verification")
public class WhoVerificationProperties {

    /** Master switch de la verificación OMS dentro del flujo de IA de salud. */
    private boolean enabled = true;

    /** Configuración de la ICD-API de la OMS. */
    private Icd icd = new Icd();

    /** Configuración del Global Health Observatory (GHO) de la OMS. */
    private Gho gho = new Gho();

    /** Umbral de fallos consecutivos para abrir el Circuit Breaker. */
    private int failureThreshold = 3;

    /** Tiempo (ms) que permanece abierto el Circuit Breaker antes de reintentar. */
    private long openTimeoutMillis = 30_000;

    @Data
    public static class Icd {

        /** Habilitar consultas a la ICD-API. Requiere credenciales OAuth2. */
        private boolean enabled = false;

        /** Base URL de la API (cloud: https://id.who.int). */
        private String baseUrl = "https://id.who.int";

        /** Versión de la API (cabecera {@code API-Version}). */
        private String apiVersion = "v2";

        /** Release de la clasificación consultada (p. ej. 2024-01). */
        private String releaseId = "2024-01";

        /** Linearización consultada (ICD-11 MMS para códigos). */
        private String linearization = "mms";

        /** Token endpoint OAuth2 de gestión de accesos de la OMS. */
        private String tokenEndpoint = "https://icdaccessmanagement.who.int/connect/token";

        /** Scope OAuth2 solicitado en el token. */
        private String scope = "icdapi_access";

        /** Client ID OAuth2 (portal ICD-API). */
        private String clientId = "";

        /** Client Secret OAuth2 (portal ICD-API). */
        private String clientSecret = "";

        /** Timeout de conexión (ms). */
        private int connectTimeoutMillis = 5000;

        /** Timeout de lectura (ms). */
        private int readTimeoutMillis = 8000;

        /** Máximo de resultados por búsqueda. */
        private int maxResults = 5;

        /** Máximo de búsquedas por turno de conversación. */
        private int maxQueriesPerTurn = 2;
    }

    @Data
    public static class Gho {

        /** Habilitar consultas al GHO (no requiere credenciales). */
        private boolean enabled = true;

        /** Base URL del GHO. */
        private String baseUrl = "https://apps.who.int/gho/athena/api/GHO";

        /** Timeout de conexión (ms). */
        private int connectTimeoutMillis = 5000;

        /** Timeout de lectura (ms). */
        private int readTimeoutMillis = 8000;

        /**
         * Indicadores GHO consultados automáticamente en cada análisis (opcional).
         * Ejemplo: indicador "NCD_BMI_30A" (obesidad) o códigos del catálogo GHO.
         * Vacío por defecto: no se hacen llamadas GHO automáticas.
         */
        private List<GhoIndicator> indicators = new ArrayList<>();
    }

    /** Indicador GHO configurable para dar contexto estadístico global. */
    @Data
    public static class GhoIndicator {

        /** Código oficial del indicador en el catálogo GHO. */
        private String indicatorCode;

        /** Etiqueta descriptiva usada en el contexto del prompt. */
        private String label;

        /** Código de país ISO 3166-1 alpha-3 (opcional; vacío = dimensión GLOBAL). */
        private String countryCode = "";
    }
}
