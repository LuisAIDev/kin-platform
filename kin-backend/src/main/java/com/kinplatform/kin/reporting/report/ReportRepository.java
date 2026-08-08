package com.kinplatform.kin.reporting.report;

import com.kinplatform.kin.reporting.report.model.ConsultingReport;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del {@link ConsultingReport}.
 *
 * <p>Definido en el dominio; la infraestructura lo implementa (p. ej. con la
 * tabla {@code project_reports}). {@code version} es incremental por proyecto
 * (1..n) y el histórico completo se conserva: {@code findLatest} devuelve la
 * generación más reciente y {@code findByVersion} una versión concreta.
 * {@code listVersions} expone solo la metadata (sin el JSON completo).</p>
 *
 * <p>{@code save} retorna la versión asignada y {@code StoredReport} agrupa la
 * versión con el reporte para no perderla al cargarlo.</p>
 */
public interface ReportRepository {

    /** Reporte persistido con su versión incremental. */
    record StoredReport(int version, ConsultingReport report) {
        public StoredReport {
            if (report == null) {
                throw new IllegalArgumentException("report no puede ser null");
            }
        }
    }

    /** Metadata de una versión persistida (para listados ligeros). */
    record ReportVersionInfo(int version, UUID reportId, String reportVersion,
                             OffsetDateTime generatedAt) {
    }

    /**
     * Persiste el reporte como una versión nueva del proyecto.
     *
     * @param projectId proyecto al que pertenece (obligatorio)
     * @param report    reporte a persistir (obligatorio)
     * @return versión incremental asignada
     */
    int save(UUID projectId, ConsultingReport report);

    /** Última versión persistida del proyecto, si existe. */
    Optional<StoredReport> findLatest(UUID projectId);

    /** Versión concreta del proyecto, si existe. */
    Optional<StoredReport> findByVersion(UUID projectId, int version);

    /** Metadata de todas las versiones del proyecto, en orden ascendente. */
    List<ReportVersionInfo> listVersions(UUID projectId);
}
