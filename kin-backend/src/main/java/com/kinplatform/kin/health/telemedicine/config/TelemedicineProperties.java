package com.kinplatform.kin.health.telemedicine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de telemedicina (ADR-032).
 *
 * <p>Master switch y clave de cifrado del contenido de mensajes en reposo.
 * Default: módulo habilitado.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_TELEMEDICINE_ENABLED} y
 * {@code KIN_HEALTH_TELEMEDICINE_CRYPTO_SECRET}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.telemedicine")
public class TelemedicineProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Clave para cifrar el contenido de mensajes en reposo (AES/GCM). */
    private String cryptoSecret = "kin-telemedicine-dev-key";
}
