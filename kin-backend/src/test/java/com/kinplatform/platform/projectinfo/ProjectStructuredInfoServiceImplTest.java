package com.kinplatform.platform.projectinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kinplatform.platform.project.Project;
import com.kinplatform.platform.project.ProjectRepository;
import com.kinplatform.platform.projectinfo.dto.StructuredInfoEntry;
import com.kinplatform.platform.projectinfo.dto.StructuredInfoResponse;
import com.kinplatform.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectStructuredInfoServiceImplTest {

    @Mock
    private ProjectStructuredInfoRepository repository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectStructuredInfoServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Project project = Project.builder()
                .id(projectId)
                .user(User.builder().id(userId).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    @Test
    void upsertCreaEntradasConSeccionNormalizadaYOrigenPreservado() {
        when(repository.findById(any(ProjectStructuredInfoId.class))).thenReturn(Optional.empty());
        when(repository.save(any(ProjectStructuredInfo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByProjectIdOrderBySectionAscKeyAsc(projectId))
                .thenReturn(List.of(ProjectStructuredInfo.builder()
                        .projectId(projectId)
                        .section("FINANZAS")
                        .key("inversion_inicial")
                        .value("80000000")
                        .sourceType(StructuredInfoSourceType.USER_INPUT)
                        .build()));

        List<StructuredInfoResponse> result = service.upsert(
                userId,
                projectId,
                List.of(new StructuredInfoEntry(
                        "finanzas", "Inversión Inicial", " 80000000 ", StructuredInfoSourceType.USER_INPUT)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSection()).isEqualTo("FINANZAS");
        assertThat(result.get(0).getKey()).isEqualTo("inversion_inicial");
        assertThat(result.get(0).getValue()).isEqualTo("80000000");
        assertThat(result.get(0).getSourceType()).isEqualTo(StructuredInfoSourceType.USER_INPUT);
    }

    @Test
    void listByProjectDevuelveEntradasOrdenadas() {
        when(repository.findByProjectIdOrderBySectionAscKeyAsc(projectId))
                .thenReturn(List.of(ProjectStructuredInfo.builder()
                        .projectId(projectId)
                        .section("MERCADO")
                        .key("publico_objetivo")
                        .value("Familias rurales")
                        .sourceType(StructuredInfoSourceType.IMPORTED_DOCUMENT)
                        .build()));

        List<StructuredInfoResponse> result = service.listByProject(userId, projectId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSourceType()).isEqualTo(StructuredInfoSourceType.IMPORTED_DOCUMENT);
    }

    @Test
    void proyectoDeOtroUsuarioNoEsAccesible() {
        Project other = Project.builder()
                .id(projectId)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.listByProject(userId, projectId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void upsertActualizaValorExistente() {
        ProjectStructuredInfo existing = ProjectStructuredInfo.builder()
                .projectId(projectId)
                .section("FINANZAS")
                .key("precio")
                .value("50000")
                .sourceType(StructuredInfoSourceType.USER_INPUT)
                .build();
        when(repository.findById(new ProjectStructuredInfoId(projectId, "FINANZAS", "precio")))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(ProjectStructuredInfo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByProjectIdOrderBySectionAscKeyAsc(projectId)).thenReturn(List.of(existing));

        service.upsert(
                userId,
                projectId,
                List.of(new StructuredInfoEntry("FINANZAS", "precio", "60000", StructuredInfoSourceType.CALCULATED)));

        assertThat(existing.getValue()).isEqualTo("60000");
        assertThat(existing.getSourceType()).isEqualTo(StructuredInfoSourceType.CALCULATED);
    }

    @Test
    void confirmConvierteImportadoEnUsuarioConservandoTrazabilidad() {
        ProjectStructuredInfo entity = ProjectStructuredInfo.builder()
                .projectId(projectId)
                .section("FINANZAS")
                .key("precio")
                .value("45000")
                .sourceType(StructuredInfoSourceType.IMPORTED_DOCUMENT)
                .build();
        when(repository.findById(new ProjectStructuredInfoId(projectId, "FINANZAS", "precio")))
                .thenReturn(Optional.of(entity));
        when(repository.save(any(ProjectStructuredInfo.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.confirm(userId, projectId, "finanzas", "Precio", "documento.pdf");

        assertThat(result.getSourceType()).isEqualTo(StructuredInfoSourceType.USER_INPUT);
        assertThat(result.getOriginalSourceType()).isEqualTo(StructuredInfoSourceType.IMPORTED_DOCUMENT);
        assertThat(result.getSourceDocument()).isEqualTo("documento.pdf");
        assertThat(result.getConfirmedAt()).isNotNull();
    }

    @Test
    void confirmYaConfirmadoEsIdempotente() {
        ProjectStructuredInfo entity = ProjectStructuredInfo.builder()
                .projectId(projectId)
                .section("FINANZAS")
                .key("precio")
                .value("45000")
                .sourceType(StructuredInfoSourceType.USER_INPUT)
                .build();
        when(repository.findById(new ProjectStructuredInfoId(projectId, "FINANZAS", "precio")))
                .thenReturn(Optional.of(entity));

        var result = service.confirm(userId, projectId, "FINANZAS", "precio", null);

        assertThat(result.getSourceType()).isEqualTo(StructuredInfoSourceType.USER_INPUT);
        assertThat(result.getOriginalSourceType()).isNull();
    }

    @Test
    void confirmDeProyectoAjenoEsRechazado() {
        Project other = Project.builder()
                .id(projectId)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.confirm(userId, projectId, "FINANZAS", "precio", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project not found");
    }
}


