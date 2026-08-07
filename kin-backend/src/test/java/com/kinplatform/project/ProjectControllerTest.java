package com.kinplatform.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.dto.PageResponse;
import com.kinplatform.project.dto.CreateProjectRequest;
import com.kinplatform.project.dto.ProjectResponse;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "u@kin.com";
    private static final UUID PROJECT_ID = UUID.randomUUID();

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProjectService projectService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService, userRepository))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(EMAIL, null, List.of());
    }

    private void stubUser() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.builder().id(USER_ID).email(EMAIL).build()));
    }

    private ProjectResponse response() {
        return ProjectResponse.builder()
                .id(PROJECT_ID)
                .userId(USER_ID)
                .title("Proyecto")
                .category("SALUD")
                .categoryName("Salud")
                .status(ProjectStatus.DRAFT)
                .build();
    }

    @Test
    void create_deberiaResponder201() throws Exception {
        stubUser();
        when(projectService.create(eq(USER_ID), any(CreateProjectRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/projects")
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Proyecto\",\"category\":\"SALUD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Proyecto"));
    }

    @Test
    void getAll_deberiaResponder200() throws Exception {
        stubUser();
        when(projectService.getAllByUser(eq(USER_ID), any()))
                .thenReturn(PageResponse.from(org.springframework.data.domain.Page.empty()));

        mockMvc.perform(get("/projects").principal(principal())).andExpect(status().isOk());
    }

    @Test
    void getById_deberiaResponder200() throws Exception {
        stubUser();
        when(projectService.getById(USER_ID, PROJECT_ID)).thenReturn(response());

        mockMvc.perform(get("/projects/" + PROJECT_ID).principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROJECT_ID.toString()));
    }

    @Test
    void update_deberiaResponder200() throws Exception {
        stubUser();
        when(projectService.update(eq(USER_ID), eq(PROJECT_ID), any())).thenReturn(response());

        mockMvc.perform(put("/projects/" + PROJECT_ID)
                        .principal(principal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nuevo\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_deberiaResponder204() throws Exception {
        stubUser();

        mockMvc.perform(delete("/projects/" + PROJECT_ID).principal(principal()))
                .andExpect(status().isNoContent());

        verify(projectService).delete(USER_ID, PROJECT_ID);
    }
}
