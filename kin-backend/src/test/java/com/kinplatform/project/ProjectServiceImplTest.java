package com.kinplatform.project;

import com.kinplatform.chat.ChatMessageRepository;
import com.kinplatform.chat.MessageRole;
import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.project.dto.CreateProjectRequest;
import com.kinplatform.project.dto.ProjectResponse;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Captor
    private ArgumentCaptor<Project> projectCaptor;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String TITLE = "Mi Proyecto";
    private static final String DESCRIPTION = "Descripción del proyecto";
    private static final ProjectCategory CATEGORY = ProjectCategory.EMPRENDIMIENTO;

    private User user;
    private Project project;
    private CreateProjectRequest createRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email("user@test.com")
                .fullName("Test User")
                .role(UserRole.FREE)
                .build();

        project = Project.builder()
                .id(PROJECT_ID)
                .user(user)
                .title(TITLE)
                .description(DESCRIPTION)
                .category(CATEGORY)
                .status(ProjectStatus.DRAFT)
                .build();

        createRequest = new CreateProjectRequest();
        createRequest.setTitle(TITLE);
        createRequest.setDescription(DESCRIPTION);
        createRequest.setCategory(CATEGORY);

        pageable = PageRequest.of(0, 12);
    }

    @Test
    void createProject_deberiaCrearProyectoExitosamente_conDatosValidos() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        Project savedProject = Project.builder()
                .id(PROJECT_ID)
                .user(user)
                .title(TITLE.trim())
                .description(DESCRIPTION.trim())
                .category(CATEGORY)
                .status(ProjectStatus.DRAFT)
                .build();
        savedProject.onCreate();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(chatMessageRepository.countByProjectIdAndRole(PROJECT_ID, MessageRole.USER)).thenReturn(0L);

        ProjectResponse response = projectService.create(USER_ID, createRequest);

        assertNotNull(response);
        assertEquals(PROJECT_ID, response.getId());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(TITLE, response.getTitle());
        assertEquals(DESCRIPTION, response.getDescription());
        assertEquals(CATEGORY, response.getCategory());
        assertEquals(ProjectStatus.DRAFT, response.getStatus());

        verify(userRepository).findById(USER_ID);
        verify(projectRepository).save(projectCaptor.capture());

        Project captured = projectCaptor.getValue();
        assertEquals(user, captured.getUser());
        assertEquals(TITLE, captured.getTitle());
        assertEquals(DESCRIPTION, captured.getDescription());
        assertEquals(CATEGORY, captured.getCategory());
        assertEquals(ProjectStatus.DRAFT, captured.getStatus());
    }

    @Test
    void getProjectById_deberiaRetornarProyecto_cuandoExiste() {
        project.onCreate();
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(chatMessageRepository.countByProjectIdAndRole(PROJECT_ID, MessageRole.USER)).thenReturn(0L);

        ProjectResponse response = projectService.getById(USER_ID, PROJECT_ID);

        assertNotNull(response);
        assertEquals(PROJECT_ID, response.getId());
        assertEquals(USER_ID, response.getUserId());
        assertEquals(TITLE, response.getTitle());
        assertEquals(DESCRIPTION, response.getDescription());
        assertEquals(CATEGORY, response.getCategory());
        assertEquals(ProjectStatus.DRAFT, response.getStatus());

        verify(projectRepository).findById(PROJECT_ID);
    }

    @Test
    void getProjectById_deberiaLanzarExcepcion_cuandoNoExiste() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> projectService.getById(USER_ID, PROJECT_ID));

        assertEquals("Project not found", ex.getMessage());
        verify(projectRepository).findById(PROJECT_ID);
    }

    @Test
    void getProjectById_deberiaLanzarExcepcion_cuandoPerteneceOtroUsuario() {
        User otherUser = User.builder()
                .id(OTHER_USER_ID)
                .email("other@test.com")
                .build();
        Project otherProject = Project.builder()
                .id(PROJECT_ID)
                .user(otherUser)
                .title(TITLE)
                .build();

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(otherProject));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> projectService.getById(USER_ID, PROJECT_ID));

        assertEquals("Project does not belong to this user", ex.getMessage());
        verify(projectRepository).findById(PROJECT_ID);
    }

    @Test
    void deleteProject_deberiaEliminarExitosamente_cuandoUsuarioEsPropietario() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        projectService.delete(USER_ID, PROJECT_ID);

        verify(projectRepository).findById(PROJECT_ID);
        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_deberiaLanzarExcepcion_cuandoUsuarioNoEsPropietario() {
        User otherUser = User.builder()
                .id(OTHER_USER_ID)
                .email("other@test.com")
                .build();
        Project otherProject = Project.builder()
                .id(PROJECT_ID)
                .user(otherUser)
                .build();

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(otherProject));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> projectService.delete(USER_ID, PROJECT_ID));

        assertEquals("Project does not belong to this user", ex.getMessage());
        verify(projectRepository).findById(PROJECT_ID);
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void getAllProjects_deberiaRetornarPaginaCorrecta_conParametrosValidos() {
        project.onCreate();

        Project project2 = Project.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title("Proyecto 2")
                .description("Descripción 2")
                .category(ProjectCategory.PERSONAL)
                .status(ProjectStatus.DRAFT)
                .build();
        project2.onCreate();

        List<Project> projectList = List.of(project, project2);
        Page<Project> projectPage = new PageImpl<>(projectList, pageable, 2);

        when(projectRepository.findByUserIdOrderByCreatedAtDesc(USER_ID, pageable))
                .thenReturn(projectPage);
        when(chatMessageRepository.countByProjectIdAndRole(any(UUID.class), eq(MessageRole.USER)))
                .thenReturn(0L);

        PageResponse<ProjectResponse> response = projectService.getAllByUser(USER_ID, pageable);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(2, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getCurrentPage());
        assertEquals(12, response.getSize());

        assertEquals(PROJECT_ID, response.getContent().get(0).getId());
        assertEquals(project2.getId(), response.getContent().get(1).getId());

        verify(projectRepository).findByUserIdOrderByCreatedAtDesc(USER_ID, pageable);
        verify(chatMessageRepository, times(2))
                .countByProjectIdAndRole(any(UUID.class), eq(MessageRole.USER));
    }
}
