package com.kinplatform.ai.report.adapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data de {@link ProjectReportEntity}.
 */
public interface ProjectReportJpaRepository extends JpaRepository<ProjectReportEntity, UUID> {

    Optional<ProjectReportEntity> findFirstByProjectIdOrderByVersionDesc(UUID projectId);

    Optional<ProjectReportEntity> findByProjectIdAndVersion(UUID projectId, int version);

    List<ProjectReportEntity> findByProjectIdOrderByVersionAsc(UUID projectId);
}
