package com.kinplatform.platform.project;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.context.ProjectContextSyncPort;
import com.kinplatform.platform.enterprise.integration.DimensionSourceMapping;
import com.kinplatform.platform.enterprise.integration.DimensionSourceMapping.DimensionKey;
import com.kinplatform.platform.projectinfo.ProjectStructuredInfoService;
import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;
import com.kinplatform.platform.projectinfo.dto.StructuredInfoEntry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sincroniza el {@link ProjectContext} con el agregado {@link Project}
 * (solución de una única fuente coherente para el usuario).
 *
 * <p>Es el ÚNICO punto donde el análisis escribe los campos de presentación
 * del {@code Project}: llena {@code title} desde {@code PROJECT_NAME} y
 * {@code description} desde {@code SOLUTION} (o {@code PROBLEM} como respaldo)
 * únicamente cuando el campo del {@code Project} está vacío — nunca sobrescribe
 * información válida con datos peores. Implementa el puerto de dominio
 * {@link ProjectContextSyncPort} (inversión de dependencia: la aplicación
 * depende del puerto, no al revés).</p>
 */
@Component
public class ProjectContextSynchronizer implements ProjectContextSyncPort {

    private final ProjectRepository projectRepository;
    private final ProjectStructuredInfoService structuredInfoService;

    public ProjectContextSynchronizer(
            ProjectRepository projectRepository,
            ProjectStructuredInfoService structuredInfoService) {
        this.projectRepository = projectRepository;
        this.structuredInfoService = structuredInfoService;
    }

    @Override
    @Transactional
    public void sync(UUID userId, UUID projectId, ProjectContext context) {
        if (projectId == null || context == null) {
            return;
        }
        if (userId != null) {
            syncStructuredInfo(userId, projectId, context);
        }
        projectRepository.findById(projectId).ifPresent(project -> {
            boolean changed = false;

            String name = value(context, AnalyzedDimension.PROJECT_NAME);
            if (isBlank(project.getTitle()) && name != null) {
                project.setTitle(name);
                changed = true;
            }

            String description = candidateDescription(context);
            if (isBlank(project.getDescription()) && description != null) {
                project.setDescription(description);
                changed = true;
            }

            if (changed) {
                projectRepository.save(project);
            }
        });
    }

    private void syncStructuredInfo(UUID userId, UUID projectId, ProjectContext ctx) {
        List<StructuredInfoEntry> entries = new ArrayList<>();

        for (AnalyzedDimension dim : AnalyzedDimension.values()) {
            String value = ctx.value(dim);
            if (value == null || value.isBlank()) {
                continue;
            }

            List<DimensionKey> candidates = DimensionSourceMapping.candidateKeys(dim);
            for (DimensionKey key : candidates) {
                entries.add(new StructuredInfoEntry(
                        key.section(),
                        key.key(),
                        value,
                        StructuredInfoSourceType.USER_INPUT));
                break;
            }
        }

        if (!entries.isEmpty()) {
            structuredInfoService.upsert(userId, projectId, entries);
        }
    }

    private String candidateDescription(ProjectContext context) {
        String solution = value(context, AnalyzedDimension.SOLUTION);
        if (solution != null) {
            return solution;
        }
        return value(context, AnalyzedDimension.PROBLEM);
    }

    private static String value(ProjectContext context, AnalyzedDimension dim) {
        var raw = context.value(dim);
        return (raw != null && !raw.isBlank()) ? raw.trim() : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


