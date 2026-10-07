package com.kinplatform.platform.projectinfo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.platform.projectinfo.ProjectStructuredInfoService;
import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;
import com.kinplatform.platform.projectinfo.dto.StructuredInfoResponse;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProjectStructuredInfoControllerTest {

    private final ProjectStructuredInfoService infoService = mock(ProjectStructuredInfoService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Authentication auth = mock(Authentication.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new ProjectStructuredInfoController(infoService, userRepository))
            .build();

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(auth.getName()).thenReturn("user@kin.com");
        when(userRepository.findByEmail("user@kin.com"))
                .thenReturn(Optional.of(User.builder().id(userId).build()));
    }

    @Test
    void upsertConEntradaValidaDevuelve200() throws Exception {
        when(infoService.upsert(eq(userId), eq(projectId), any()))
                .thenReturn(List.of(StructuredInfoResponse.builder()
                        .projectId(projectId)
                        .section("FINANZAS")
                        .key("inversion_inicial")
                        .value("80000000")
                        .sourceType(StructuredInfoSourceType.USER_INPUT)
                        .updatedAt(OffsetDateTime.now())
                        .build()));

        String body =
                """
                {"entries":[{"section":"FINANZAS","key":"inversion_inicial",\
                "value":"80000000","sourceType":"USER_INPUT"}]}
                """;

        mockMvc.perform(post("/projects/{id}/info", projectId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].section").value("FINANZAS"))
                .andExpect(jsonPath("$[0].sourceType").value("USER_INPUT"));
    }

    @Test
    void upsertConEntradasVaciasDevuelve400() throws Exception {
        mockMvc.perform(post("/projects/{id}/info", projectId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listDevuelve200() throws Exception {
        when(infoService.listByProject(userId, projectId)).thenReturn(List.of());

        mockMvc.perform(get("/projects/{id}/info", projectId).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void confirmDevuelve200YConservaTrazabilidad() throws Exception {
        when(infoService.confirm(eq(userId), eq(projectId), eq("FINANZAS"), eq("precio"), eq("documento.pdf")))
                .thenReturn(StructuredInfoResponse.builder()
                        .projectId(projectId)
                        .section("FINANZAS")
                        .key("precio")
                        .value("45000")
                        .sourceType(StructuredInfoSourceType.USER_INPUT)
                        .originalSourceType(StructuredInfoSourceType.IMPORTED_DOCUMENT)
                        .sourceDocument("documento.pdf")
                        .build());

        mockMvc.perform(post("/projects/{id}/info/{section}/{key}/confirm", projectId, "FINANZAS", "precio")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceDocument\":\"documento.pdf\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("USER_INPUT"))
                .andExpect(jsonPath("$.originalSourceType").value("IMPORTED_DOCUMENT"))
                .andExpect(jsonPath("$.sourceDocument").value("documento.pdf"));
    }
}



