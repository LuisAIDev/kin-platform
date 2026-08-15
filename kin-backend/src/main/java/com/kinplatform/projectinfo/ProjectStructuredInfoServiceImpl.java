package com.kinplatform.projectinfo;

import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.projectinfo.dto.StructuredInfoEntry;
import com.kinplatform.projectinfo.dto.StructuredInfoResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link ProjectStructuredInfoService}. Normaliza la sección
 * (mayúsculas) y la clave (minúsculas con guion bajo) y hace upsert por clave
 * compuesta {@code (project_id, section, key)}, preservando el
 * {@link StructuredInfoSourceType} de cada dato.
 */
@Service
public class ProjectStructuredInfoServiceImpl implements ProjectStructuredInfoService {

    private final ProjectStructuredInfoRepository repository;
    private final ProjectRepository projectRepository;

    public ProjectStructuredInfoServiceImpl(
            ProjectStructuredInfoRepository repository, ProjectRepository projectRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public List<StructuredInfoResponse> upsert(UUID userId, UUID projectId, List<StructuredInfoEntry> entries) {
        requireOwnedProject(userId, projectId);
        for (StructuredInfoEntry entry : entries) {
            String section = entry.getSection().trim().toUpperCase(Locale.ROOT);
            String key = normalizeKey(entry.getKey());
            ProjectStructuredInfoId id = new ProjectStructuredInfoId(projectId, section, key);
            ProjectStructuredInfo entity = repository.findById(id).orElseGet(() -> ProjectStructuredInfo.builder()
                    .projectId(projectId)
                    .section(section)
                    .key(key)
                    .build());
            entity.setValue(entry.getValue().trim());
            entity.setSourceType(entry.getSourceType());
            repository.save(entity);
        }
        return listByProject(userId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructuredInfoResponse> listByProject(UUID userId, UUID projectId) {
        requireOwnedProject(userId, projectId);
        return repository.findByProjectIdOrderBySectionAscKeyAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StructuredInfoResponse confirm(
            UUID userId, UUID projectId, String section, String key, String sourceDocument) {
        requireOwnedProject(userId, projectId);
        String normalizedSection = section.trim().toUpperCase(Locale.ROOT);
        String normalizedKey = normalizeKey(key);
        ProjectStructuredInfoId id = new ProjectStructuredInfoId(projectId, normalizedSection, normalizedKey);
        ProjectStructuredInfo entity = repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La información solicitada no existe"));
        if (entity.getSourceType() != StructuredInfoSourceType.USER_INPUT) {
            entity.setOriginalSourceType(entity.getSourceType());
            entity.setSourceType(StructuredInfoSourceType.USER_INPUT);
            if (sourceDocument != null && !sourceDocument.isBlank()) {
                entity.setSourceDocument(sourceDocument.trim());
            }
            entity.setConfirmedAt(OffsetDateTime.now());
            entity = repository.save(entity);
        }
        return toResponse(entity);
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private StructuredInfoResponse toResponse(ProjectStructuredInfo entity) {
        return StructuredInfoResponse.builder()
                .projectId(entity.getProjectId())
                .section(entity.getSection())
                .key(entity.getKey())
                .value(entity.getValue())
                .sourceType(entity.getSourceType())
                .originalSourceType(entity.getOriginalSourceType())
                .sourceDocument(entity.getSourceDocument())
                .confirmedAt(entity.getConfirmedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Project requireOwnedProject(UUID userId, UUID projectId) {
        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (project.getUser() == null || !project.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Project not found");
        }
        return project;
    }
}
