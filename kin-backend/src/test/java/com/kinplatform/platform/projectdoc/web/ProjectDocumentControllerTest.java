package com.kinplatform.platform.projectdoc.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.platform.projectdoc.ProjectDocumentService;
import com.kinplatform.platform.projectdoc.ProjectDocumentStatus;
import com.kinplatform.platform.projectdoc.dto.DocumentResponse;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProjectDocumentControllerTest {

    private final ProjectDocumentService documentService = mock(ProjectDocumentService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Authentication auth = mock(Authentication.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                    new ProjectDocumentController(documentService, userRepository))
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
    void uploadDeArchivoDevuelve200() throws Exception {
        DocumentResponse response = DocumentResponse.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .filename("notas.txt")
                .mimeType("text/plain")
                .size(10)
                .status(ProjectDocumentStatus.PROCESADO)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        when(documentService.upload(any(), any(), any())).thenReturn(response);

        MockMultipartFile file =
                new MockMultipartFile("file", "notas.txt", "text/plain", "hola".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/projects/{id}/documents", projectId)
                        .file(file)
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("notas.txt"))
                .andExpect(jsonPath("$.status").value("PROCESADO"));
    }

    @Test
    void listDevuelve200() throws Exception {
        when(documentService.listByProject(userId, projectId)).thenReturn(List.of());

        mockMvc.perform(get("/projects/{id}/documents", projectId).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}


