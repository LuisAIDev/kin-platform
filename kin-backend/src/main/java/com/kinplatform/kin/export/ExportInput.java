package com.kinplatform.kin.export;

import com.kinplatform.kin.export.model.ExportMode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Datos del proyecto necesarios para la exportación (DTO de dominio).
 *
 * <p>Desacopla el ensamblador de las entidades JPA: la capa de aplicación
 * construye este input a partir de {@code Project}, {@code ConsultingReport} y
 * {@code project_info}. El historial de chat NO es una fuente de este input.</p>
 *
 * @param title         nombre del proyecto
 * @param description   descripción del proyecto
 * @param categoryName  nombre de la categoría
 * @param status        estado del proyecto (texto)
 * @param viabilityScore score de viabilidad (puede ser {@code null})
 * @param aiSummary     resumen generado por KIN (puede ser {@code null})
 * @param createdAt     fecha de creación
 * @param updatedAt     fecha de actualización
 * @param report        reporte de consultoría (puede ser {@code null})
 * @param mode          modo de exportación
 */
public record ExportInput(
        String title,
        String description,
        String categoryName,
        String status,
        BigDecimal viabilityScore,
        String aiSummary,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        com.kinplatform.platform.reporting.report.model.ConsultingReport report,
        java.util.List<StructuredInfo> info,
        ExportMode mode) {

    public ExportInput {
        title = title == null ? "" : title;
        description = description == null ? "" : description;
        categoryName = categoryName == null ? "" : categoryName;
        status = status == null ? "" : status;
        info = info == null ? java.util.List.of() : java.util.List.copyOf(info);
        mode = mode == null ? ExportMode.COMPLETE : mode;
    }
}

