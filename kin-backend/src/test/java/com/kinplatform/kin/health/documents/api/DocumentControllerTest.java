package com.kinplatform.kin.health.documents.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.common.GlobalExceptionHandler;
import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de los endpoints REST de documentos clínicos (ADR-036) con MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentControllerTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final String PHYSICIAN_EMAIL = "medico@kin.com";
    private static final String PATIENT_EMAIL = "paciente@kin.com";

    @Mock
    private DocumentService documentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(authentication.getName()).thenReturn(PHYSICIAN_EMAIL);
        lenient().when(userRepository.findByEmail(PHYSICIAN_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN).email(PHYSICIAN_EMAIL).role(UserRole.PHYSICIAN).build()));
        lenient().when(userRepository.findByEmail(PATIENT_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT).email(PATIENT_EMAIL).role(UserRole.PATIENT).build()));
        mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(documentService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.GET, "/")
                        .with(request -> {
                            request.setUserPrincipal(authentication);
                            return request;
                        }))
                .build();
    }

    private static ClinicalDocument document() {
        return ClinicalDocument.of(
                UUID.randomUUID(), "resultado.pdf", 10, "application/pdf", "key",
                PHYSICIAN, PATIENT, PHYSICIAN, "Resultado", DocumentStatus.ACTIVE,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void upload_deberiaSubirDocumento() throws Exception {
        when(documentService.uploadDocument(eq(PHYSICIAN), eq(PATIENT), eq("resultado.pdf"),
                eq("application/pdf"), any(), eq("Resultado")))
                .thenReturn(document());
        MockMultipartFile file = new MockMultipartFile(
                "file", "resultado.pdf", MediaType.APPLICATION_PDF_VALUE, "contenido".getBytes());

        mockMvc.perform(multipart("/health/documents/upload")
                        .file(file)
                        .param("patientId", PATIENT.toString())
                        .param("description", "Resultado"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("resultado.pdf"));
    }

    @Test
    void upload_conRolPaciente_deberia403() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.pdf", MediaType.APPLICATION_PDF_VALUE, "x".getBytes());

        mockMvc.perform(multipart("/health/documents/upload")
                        .file(file)
                        .param("patientId", PATIENT.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentsForPatient_deberiaDevolverLista() throws Exception {
        when(documentService.listDocumentsForPatient(PHYSICIAN, PATIENT)).thenReturn(List.of(document()));

        mockMvc.perform(get("/health/documents/patients/" + PATIENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("resultado.pdf"));
    }

    @Test
    void myDocuments_deberiaDevolverListaDelPaciente() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        when(documentService.listMyDocuments(PATIENT)).thenReturn(List.of(document()));

        mockMvc.perform(get("/health/documents/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(PATIENT.toString()));
    }

    @Test
    void download_deberiaDevolverArchivo() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        UUID id = UUID.randomUUID();
        when(documentService.downloadDocument(id, PATIENT))
                .thenReturn(new DocumentService.DownloadedDocument("contenido".getBytes(), "resultado.pdf",
                        "application/pdf"));

        mockMvc.perform(get("/health/documents/" + id + "/download"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("contenido".getBytes()));
    }

    @Test
    void delete_deberiaEliminar() throws Exception {
        lenient().when(authentication.getName()).thenReturn(PATIENT_EMAIL);
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/health/documents/" + id))
                .andExpect(status().isNoContent());
    }
}
