package com.kinplatform.project.dto;

import com.kinplatform.kin.reporting.report.model.ConsultingReport;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta del endpoint de reporte: envoltorio fino con la metadata de la
 * versión y el {@link ConsultingReport} completo (sin acoplarse a un formato de
 * salida específico).
 */
public record ReportResponse(
        UUID projectId,
        int version,
        UUID reportId,
        String reportVersion,
        OffsetDateTime generatedAt,
        ConsultingReport report
) {
}
