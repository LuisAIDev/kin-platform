package com.kinplatform.projectdoc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.projectdoc.dto.DocumentResponse;
import com.kinplatform.projectdoc.extract.DocumentExtractionService;
import com.kinplatform.projectdoc.extract.TextExtractionException;
import com.kinplatform.user.User;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProjectDocumentServiceImplTest {

    @Mock
    private ProjectDocumentRepository repository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DocumentExtractionService extractionService;

    @InjectMocks
    private ProjectDocumentServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Project project = Project.builder()
                .id(projectId)
                .user(User.builder().id(userId).build())
                .build();
        lenient().when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        lenient().when(repository.save(any(ProjectDocument.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void uploadValidoPersisteComoProcesadoConTextoExtraido() {
        when(extractionService.extract(any(), any())).thenReturn("contenido extraído");
        MockMultipartFile file = new MockMultipartFile(
                "file", "notas.txt", "text/plain", "contenido extraído".getBytes(StandardCharsets.UTF_8));

        DocumentResponse response = service.upload(userId, projectId, file);

        assertThat(response.getStatus()).isEqualTo(ProjectDocumentStatus.PROCESADO);
        assertThat(response.getFilename()).isEqualTo("notas.txt");
        ArgumentCaptor<ProjectDocument> captor = ArgumentCaptor.forClass(ProjectDocument.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getHash()).isNotBlank();
    }

    @Test
    void archivoVacioEsRechazado() {
        MockMultipartFile file = new MockMultipartFile("file", "notas.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> service.upload(userId, projectId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void archivoDemasiadoGrandeEsRechazado() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "grande.txt", "text/plain", big);

        assertThatThrownBy(() -> service.upload(userId, projectId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void extensionNoPermitidaEsRechazada() {
        MockMultipartFile file =
                new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[] {1});

        assertThatThrownBy(() -> service.upload(userId, projectId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato no permitido");
    }

    @Test
    void proyectoDeOtroUsuarioNoEsAccesible() {
        Project other = Project.builder()
                .id(projectId)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(other));
        MockMultipartFile file =
                new MockMultipartFile("file", "notas.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.upload(userId, projectId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void errorDeExtraccionMarcaDocumentoComoError() {
        when(extractionService.extract(any(), any())).thenThrow(new TextExtractionException("corrupto"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.pdf", "application/pdf", "no es un pdf".getBytes(StandardCharsets.UTF_8));

        DocumentResponse response = service.upload(userId, projectId, file);

        assertThat(response.getStatus()).isEqualTo(ProjectDocumentStatus.ERROR);
        assertThat(response.getErrorMessage()).contains("No se pudo procesar");
    }

    @Test
    void listarDeOtroProyectoNoEsPermitido() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByProject(userId, projectId)).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).findByProjectIdOrderByCreatedAtDesc(any());
    }
}
