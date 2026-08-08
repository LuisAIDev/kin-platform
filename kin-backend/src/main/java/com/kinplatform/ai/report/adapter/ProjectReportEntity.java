package com.kinplatform.ai.report.adapter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad que persiste una generación del {@code ConsultingReport} de un
 * proyecto (1:N con {@code projects.id}).
 *
 * <p>El reporte se almacena como JSON en {@code report_json}; la
 * serialización/deserialización es responsabilidad del adaptador
 * ({@link JpaReportRepository}), manteniendo la entidad agnóstica del
 * dominio. {@code version} es incremental por proyecto (clave única
 * {@code (project_id, version)}) y conserva el histórico completo.</p>
 */
@Entity
@Table(name = "project_reports", uniqueConstraints = {
    @UniqueConstraint(name = "uk_project_reports_project_version",
        columnNames = {"project_id", "version"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectReportEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "project_id", nullable = false, columnDefinition = "uuid")
    private UUID projectId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "report_id", nullable = false, columnDefinition = "uuid")
    private UUID reportId;

    @Column(name = "report_version", nullable = false, length = 32)
    private String reportVersion;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "report_json", nullable = false, columnDefinition = "jsonb")
    private String reportJson;
}
