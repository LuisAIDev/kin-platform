package com.kinplatform.projectinfo;

import com.kinplatform.projectinfo.dto.StructuredInfoEntry;
import com.kinplatform.projectinfo.dto.StructuredInfoResponse;
import java.util.List;
import java.util.UUID;

/** Servicio de información estructurada del proyecto. */
public interface ProjectStructuredInfoService {

    List<StructuredInfoResponse> upsert(UUID userId, UUID projectId, List<StructuredInfoEntry> entries);

    List<StructuredInfoResponse> listByProject(UUID userId, UUID projectId);

    /**
     * Confirma explícitamente un dato importado (IMPORTED_DOCUMENT) como
     * USER_INPUT, conservando trazabilidad del origen original. Solo el
     * propietario del proyecto puede confirmar. Si el dato ya es USER_INPUT,
     * la operación es idempotente.
     */
    StructuredInfoResponse confirm(UUID userId, UUID projectId, String section, String key, String sourceDocument);
}
