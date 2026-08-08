package com.kinplatform.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Metadata de una versión persistida del reporte (listado ligero, sin el JSON
 * completo).
 */
public record ReportVersionResponse(
        int version,
        UUID reportId,
        String reportVersion,
        OffsetDateTime generatedAt
) {
}
