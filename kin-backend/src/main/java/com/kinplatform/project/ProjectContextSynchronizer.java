package com.kinplatform.project;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.context.ProjectContextSyncPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    public ProjectContextSynchronizer(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public void sync(UUID projectId, ProjectContext context) {
        if (projectId == null || context == null) {
            return;
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
