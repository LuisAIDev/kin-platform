package com.kinplatform.ai.enterprise.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ProjectContext;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.projectinfo.ProjectStructuredInfo;
import com.kinplatform.projectinfo.ProjectStructuredInfoRepository;
import com.kinplatform.projectinfo.StructuredInfoSourceType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultEnterpriseContextEnricherTest {

    @Mock
    private ProjectStructuredInfoRepository repository;

    private final UUID projectId = UUID.randomUUID();

    @Test
    void rellenaDimensionesPendientesDesdeInformacionEstructurada() {
        when(repository.findByProjectIdOrderBySectionAscKeyAsc(projectId))
                .thenReturn(List.of(ProjectStructuredInfo.builder()
                        .projectId(projectId)
                        .section("MERCADO")
                        .key("competidores")
                        .value("Competidor A")
                        .sourceType(StructuredInfoSourceType.IMPORTED_DOCUMENT)
                        .build()));
        ProjectContext context = ProjectContext.fromProject("KIN SaaS", "Plataforma", "TECH");

        DefaultEnterpriseContextEnricher enricher = new DefaultEnterpriseContextEnricher(repository);
        ProjectContext enriched = enricher.enrich(projectId, context);

        assertThat(enriched.value(AnalyzedDimension.COMPETITION)).isEqualTo("Competidor A");
        assertThat(enriched.value(AnalyzedDimension.PROJECT_NAME)).isEqualTo("KIN SaaS");
        assertThat(enriched.isDimensionCovered(AnalyzedDimension.COMPETITION)).isTrue();
    }

    @Test
    void noSobrescribeDimensionConfirmadaYConservaMetadatos() {
        when(repository.findByProjectIdOrderBySectionAscKeyAsc(projectId))
                .thenReturn(List.of(ProjectStructuredInfo.builder()
                        .projectId(projectId)
                        .section("DATOS_GENERALES")
                        .key("sector")
                        .value("SERVICIOS")
                        .sourceType(StructuredInfoSourceType.IMPORTED_DOCUMENT)
                        .build()));
        ProjectContext context = ProjectContext.restore(
                Map.of(AnalyzedDimension.SECTOR, "TECH"),
                EnumSet.of(AnalyzedDimension.SECTOR),
                ConversationDecision.ask(AnalyzedDimension.MVP, 5, "test"),
                3,
                false);

        DefaultEnterpriseContextEnricher enricher = new DefaultEnterpriseContextEnricher(repository);
        ProjectContext enriched = enricher.enrich(projectId, context);

        assertThat(enriched.value(AnalyzedDimension.SECTOR)).isEqualTo("TECH");
        assertThat(enriched.exchangeCount()).isEqualTo(3);
        assertThat(enriched.currentDecision()).isEqualTo(ConversationDecision.ask(AnalyzedDimension.MVP, 5, "test"));
    }

    @Test
    void sinInformacionDevuelveContextoEquivalente() {
        when(repository.findByProjectIdOrderBySectionAscKeyAsc(projectId)).thenReturn(List.of());
        ProjectContext context = ProjectContext.fromProject("Negocio", "Desc", "SERVICIOS");

        DefaultEnterpriseContextEnricher enricher = new DefaultEnterpriseContextEnricher(repository);
        ProjectContext enriched = enricher.enrich(projectId, context);

        assertThat(enriched.value(AnalyzedDimension.PROJECT_NAME)).isEqualTo("Negocio");
        assertThat(enriched.value(AnalyzedDimension.MVP)).isNull();
        assertThat(enriched.isDimensionCovered(AnalyzedDimension.MVP)).isFalse();
    }
}

