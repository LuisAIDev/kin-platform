package com.kinplatform.kin.health.documents.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo de documentos clínicos (ADR-036).
 *
 * <p>Master switch, ruta de almacenamiento local y tamaño máximo de archivo.
 * Defaults: habilitado, {@code ./storage/documents}, 10 MB.</p>
 *
 * <p>Variables de entorno: {@code KIN_HEALTH_DOCUMENTS_ENABLED},
 * {@code KIN_HEALTH_DOCUMENTS_STORAGE_PATH} y
 * {@code KIN_HEALTH_DOCUMENTS_MAX_FILE_SIZE}.</p>
 */
@Data
@ConfigurationProperties(prefix = "kin.health.documents")
public class DocumentProperties {

    /** Master switch del módulo. */
    private boolean enabled = true;

    /** Carpeta local de almacenamiento de archivos. */
    private String storagePath = "./storage/documents";

    /** Tamaño máximo de archivo en bytes (default 10 MB). */
    private long maxFileSize = 10L * 1024 * 1024;
}
