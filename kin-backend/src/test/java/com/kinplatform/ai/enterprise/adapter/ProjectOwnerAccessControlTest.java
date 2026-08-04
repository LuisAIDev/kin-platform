package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests del adaptador de control de acceso Enterprise (remediación C1, IDOR).
 */
class ProjectOwnerAccessControlTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProjectOwnerAccessControl accessControl =
        new ProjectOwnerAccessControl(projectRepository, userRepository);

    private final User owner = User.builder().id(UUID.randomUUID()).email("owner@kin.test").build();
    private final Project project =
        Project.builder().id(UUID.randomUUID()).user(owner).build();

    @Test
    void isOwner_proyectoDelUsuario_esTrue() {
        when(userRepository.findByEmail("owner@kin.test")).thenReturn(Optional.of(owner));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertTrue(accessControl.isOwner(project.getId(), "owner@kin.test"));
    }

    @Test
    void isOwner_proyectoDeOtroUsuario_esFalse() {
        var intruder = User.builder().id(UUID.randomUUID()).email("other@kin.test").build();
        when(userRepository.findByEmail("other@kin.test")).thenReturn(Optional.of(intruder));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertFalse(accessControl.isOwner(project.getId(), "other@kin.test"));
    }

    @Test
    void isOwner_proyectoInexistente_esFalse() {
        when(userRepository.findByEmail("owner@kin.test")).thenReturn(Optional.of(owner));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.empty());

        assertFalse(accessControl.isOwner(project.getId(), "owner@kin.test"));
    }

    @Test
    void isOwner_usuarioInexistente_esFalse() {
        when(userRepository.findByEmail("ghost@kin.test")).thenReturn(Optional.empty());
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertFalse(accessControl.isOwner(project.getId(), "ghost@kin.test"));
    }

    @Test
    void isOwner_conArgumentosNulos_esFalse() {
        assertFalse(accessControl.isOwner(null, "owner@kin.test"));
        assertFalse(accessControl.isOwner(project.getId(), null));
    }
}
