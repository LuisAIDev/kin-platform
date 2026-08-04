package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.ports.EnterpriseProjectAccessControl;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Adaptador de infraestructura del control de acceso Enterprise (remediación C1).
 *
 * <p>Verifica la propiedad del proyecto contra el usuario autenticado: resuelve
 * el {@link User} por email (del token JWT) y el {@link Project} por su
 * identificador, y compara el propietario. Devuelve {@code false} si el
 * proyecto o el usuario no existen (sin revelar cuál) para no filtrar
 * existencia.</p>
 */
@Component
public class ProjectOwnerAccessControl implements EnterpriseProjectAccessControl {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    /**
     * @param projectRepository repositorio de proyectos de KIN (obligatorio)
     * @param userRepository    repositorio de usuarios (obligatorio)
     */
    public ProjectOwnerAccessControl(ProjectRepository projectRepository,
                                     UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(UUID projectId, String authenticatedUserEmail) {
        if (projectId == null || authenticatedUserEmail == null) {
            return false;
        }
        User user = userRepository.findByEmail(authenticatedUserEmail).orElse(null);
        if (user == null) {
            return false;
        }
        Project project = projectRepository.findById(projectId).orElse(null);
        return project != null
            && project.getUser() != null
            && user.getId().equals(project.getUser().getId());
    }
}
