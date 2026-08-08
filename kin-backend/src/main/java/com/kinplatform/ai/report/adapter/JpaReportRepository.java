package com.kinplatform.ai.report.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.reporting.report.ReportRepository;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.report.model.ReportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JPA del puerto {@link ReportRepository}.
 *
 * <p>Persiste cada generación del {@link ConsultingReport} como una fila de la
 * tabla {@code project_reports}, con el reporte completo serializado como JSON
 * en {@code report_json} y su metadata en columnas dedicadas para listados
 * ligeros. La versión es incremental por proyecto (1..n) y se conserva el
 * histórico completo.</p>
 */
@Component
public class JpaReportRepository implements ReportRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaReportRepository.class);

    private final ProjectReportJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public JpaReportRepository(ProjectReportJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public int save(UUID projectId, ConsultingReport report) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId no puede ser null");
        }
        if (report == null) {
            throw new IllegalArgumentException("report no puede ser null");
        }
        int version = repository.findFirstByProjectIdOrderByVersionDesc(projectId)
                .map(ProjectReportEntity::getVersion)
                .map(v -> v + 1)
                .orElse(1);
        ReportMetadata metadata = report.metadata();
        var entity = ProjectReportEntity.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .version(version)
                .reportId(report.id())
                .reportVersion(metadata == null ? "" : metadata.reportVersion())
                .generatedAt(metadata == null || metadata.generatedAt() == null
                        ? OffsetDateTime.now() : metadata.generatedAt())
                .reportJson(toJson(report))
                .build();
        repository.save(entity);
        log.info("ConsultingReport persistido para project={}, version={}", projectId, version);
        return version;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredReport> findLatest(UUID projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        return repository.findFirstByProjectIdOrderByVersionDesc(projectId)
                .map(this::toStoredReport);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredReport> findByVersion(UUID projectId, int version) {
        if (projectId == null) {
            return Optional.empty();
        }
        return repository.findByProjectIdAndVersion(projectId, version)
                .map(this::toStoredReport);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportVersionInfo> listVersions(UUID projectId) {
        if (projectId == null) {
            return List.of();
        }
        return repository.findByProjectIdOrderByVersionAsc(projectId).stream()
                .map(entity -> new ReportVersionInfo(
                        entity.getVersion(), entity.getReportId(),
                        entity.getReportVersion(), entity.getGeneratedAt()))
                .toList();
    }

    private StoredReport toStoredReport(ProjectReportEntity entity) {
        return new StoredReport(entity.getVersion(), fromJson(entity.getReportJson()));
    }

    private String toJson(ConsultingReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ConsultingReport", e);
        }
    }

    private ConsultingReport fromJson(String json) {
        try {
            return objectMapper.readValue(json, ConsultingReport.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize ConsultingReport", e);
        }
    }
}
