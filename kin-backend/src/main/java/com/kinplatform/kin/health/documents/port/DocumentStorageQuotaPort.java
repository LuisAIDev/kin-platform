package com.kinplatform.kin.health.documents.port;

import java.util.UUID;

/**
 * Puerto para consultar la cuota de almacenamiento de documentos del usuario.
 */
public interface DocumentStorageQuotaPort {

    /**
     * Bytes de almacenamiento usados por el usuario en documentos activos.
     */
    long getStorageUsedBytes(UUID userId);

    /**
     * Límite de almacenamiento en bytes según el plan del usuario.
     * Retorna Long.MAX_VALUE si es ilimitado.
     */
    long getStorageLimitBytes(UUID userId);

    /**
     * Indica si el usuario puede subir un archivo del tamaño dado sin exceder su cuota.
     */
    boolean canUpload(UUID userId, long fileSizeBytes);
}