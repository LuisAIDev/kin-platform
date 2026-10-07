package com.kinplatform.platform.ai_enterprise.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kinplatform.platform.enterprise.integration.EnterpriseIntegrationData;
import com.kinplatform.platform.projectdoc.ProjectDocument;
import com.kinplatform.platform.projectdoc.ProjectDocumentRepository;
import com.kinplatform.platform.projectdoc.ProjectDocumentStatus;
import com.kinplatform.platform.projectinfo.ProjectStructuredInfo;
import com.kinplatform.platform.projectinfo.ProjectStructuredInfoRepository;
import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaEnterpriseDataProviderTest {

    @Mock
    private ProjectStructuredInfoRepository structuredInfoRepository;

    @Mock
    private ProjectDocumentRepository documentRepository;

    private JpaEnterpriseDataProvider provider;

    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        provider = new JpaEnterpriseDataProvider(structuredInfoRepository, documentRepository);
    }

    @Test
    void cargaInformacionEstructuradaYDocumentosProcesados() {
        when(structuredInfoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId))
                .thenReturn(List.of(ProjectStructuredInfo.builder()
                        .projectId(projectId)
                        .section("FINANZAS")
                        .key("inversion_inicial")
                        .value("80000000")
                        .sourceType(StructuredInfoSourceType.USER_INPUT)
                        .build()));
        when(documentRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(ProjectDocument.builder()
                        .id(UUID.randomUUID())
                        .projectId(projectId)
                        .filename("plan.pdf")
                        .mimeType("application/pdf")
                        .size(100)
                        .status(ProjectDocumentStatus.PROCESADO)
                        .extractedText("La inversión inicial es de 80 millones.")
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build()));

        EnterpriseIntegrationData data = provider.load(projectId, null);

        assertThat(data.supplementalData().financial("inversion_inicial").value())
                .isEqualTo("80000000");
        assertThat(data.documents()).hasSize(1);
        assertThat(data.documents().get(0).summary()).contains("inversión inicial");
        assertThat(data.documents().get(0).relevantFacts()).isNotEmpty();
    }

    @Test
    void documentoConErrorNoExponeTexto() {
        when(structuredInfoRepository.findByProjectIdOrderBySectionAscKeyAsc(projectId))
                .thenReturn(List.of());
        when(documentRepository.findByProjectIdOrderByCreatedAtDesc(projectId))
                .thenReturn(List.of(ProjectDocument.builder()
                        .id(UUID.randomUUID())
                        .projectId(projectId)
                        .filename("roto.pdf")
                        .mimeType("application/pdf")
                        .size(10)
                        .status(ProjectDocumentStatus.ERROR)
                        .errorMessage("corrupto")
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build()));

        EnterpriseIntegrationData data = provider.load(projectId, null);

        assertThat(data.documents()).hasSize(1);
        assertThat(data.documents().get(0).summary()).isNull();
        assertThat(data.documents().get(0).status()).isEqualTo("ERROR");
    }
}



