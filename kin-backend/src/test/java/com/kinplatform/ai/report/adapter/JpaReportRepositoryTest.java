package com.kinplatform.ai.report.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kinplatform.kin.reporting.report.ReportRepository;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import com.kinplatform.kin.reporting.report.model.ExecutiveSummary;
import com.kinplatform.kin.reporting.report.model.FinancialSection;
import com.kinplatform.kin.reporting.report.model.InnovationSection;
import com.kinplatform.kin.reporting.report.model.MarketSection;
import com.kinplatform.kin.reporting.report.model.NextStepsSection;
import com.kinplatform.kin.reporting.report.model.OpportunitiesSection;
import com.kinplatform.kin.reporting.report.model.RecommendationsSection;
import com.kinplatform.kin.reporting.report.model.ReportBuilder;
import com.kinplatform.kin.reporting.report.model.ReportMetadata;
import com.kinplatform.kin.reporting.report.model.RisksSection;
import com.kinplatform.kin.reporting.report.model.ScoresSection;
import com.kinplatform.kin.reporting.report.model.SourcesSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaReportRepositoryTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private ProjectReportJpaRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private JpaReportRepository reportRepository;

    private JpaReportRepository repo() {
        if (reportRepository == null) {
            reportRepository = new JpaReportRepository(repository, objectMapper);
        }
        return reportRepository;
    }

    private ConsultingReport report() {
        return ReportBuilder.create(PROJECT_ID)
                .executiveSummary(ExecutiveSummary.empty())
                .scores(ScoresSection.empty())
                .recommendations(RecommendationsSection.empty())
                .risks(RisksSection.empty())
                .opportunities(OpportunitiesSection.empty())
                .financial(FinancialSection.empty())
                .market(MarketSection.empty())
                .innovation(InnovationSection.empty())
                .nextSteps(NextStepsSection.empty())
                .sources(SourcesSection.empty())
                .metadata(new ReportMetadata("v1", "2.0.0-alpha.1", OffsetDateTime.now(),
                        "ReportEngine", Map.of(), 80.0, 0.85, List.of()))
                .build();
    }

    private ProjectReportEntity entity(int version, String json) {
        return ProjectReportEntity.builder()
                .id(UUID.randomUUID())
                .projectId(PROJECT_ID)
                .version(version)
                .reportId(report().id())
                .reportVersion("v1")
                .generatedAt(OffsetDateTime.now())
                .reportJson(json)
                .build();
    }

    @Test
    void save_deberiaAsignarVersion1CuandoNoHayHistorico() {
        when(repository.findFirstByProjectIdOrderByVersionDesc(PROJECT_ID))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int version = repo().save(PROJECT_ID, report());

        assertEquals(1, version);
        var captor = org.mockito.ArgumentCaptor.forClass(ProjectReportEntity.class);
        verify(repository).save(captor.capture());
        var entity = captor.getValue();
        assertEquals(PROJECT_ID, entity.getProjectId());
        assertEquals(1, entity.getVersion());
        assertEquals(report().id(), entity.getReportId());
        assertEquals("v1", entity.getReportVersion());
        assertNotNull(entity.getGeneratedAt());
        assertNotNull(entity.getReportJson());
        assertTrue(entity.getReportJson().contains("\"executiveSummary\""));
    }

    @Test
    void save_deberiaIncrementarVersionDelHistorico() {
        when(repository.findFirstByProjectIdOrderByVersionDesc(PROJECT_ID))
                .thenReturn(Optional.of(entity(2, "{}")));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int version = repo().save(PROJECT_ID, report());

        assertEquals(3, version);
    }

    @Test
    void save_conProjectIdNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> repo().save(null, report()));
        verify(repository, never()).save(any());
    }

    @Test
    void save_conReporteNull_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> repo().save(PROJECT_ID, null));
        verify(repository, never()).save(any());
    }

    @Test
    void findLatest_deberiaRestaurarElReporteConSuVersion() {
        var expected = report();
        when(repository.findFirstByProjectIdOrderByVersionDesc(PROJECT_ID))
                .thenReturn(Optional.of(entity(4, jsonOfReport(expected))));

        var stored = repo().findLatest(PROJECT_ID).orElseThrow();

        assertEquals(4, stored.version());
        assertEquals(expected.id(), stored.report().id());
        assertEquals(expected.metadata().reportVersion(), stored.report().metadata().reportVersion());
        assertEquals(expected.metadata().generatedAt().toInstant(),
                stored.report().metadata().generatedAt().toInstant());
    }

    @Test
    void findLatest_sinHistorico_deberiaDevolverVacio() {
        when(repository.findFirstByProjectIdOrderByVersionDesc(PROJECT_ID))
                .thenReturn(Optional.empty());

        assertTrue(repo().findLatest(PROJECT_ID).isEmpty());
    }

    @Test
    void findLatest_conProjectIdNull_deberiaDevolverVacio() {
        assertTrue(repo().findLatest(null).isEmpty());
    }

    @Test
    void findByVersion_deberiaDevolverLaVersionPedida() {
        var expected = report();
        when(repository.findByProjectIdAndVersion(PROJECT_ID, 2))
                .thenReturn(Optional.of(entity(2, jsonOfReport(expected))));

        var stored = repo().findByVersion(PROJECT_ID, 2).orElseThrow();

        assertEquals(2, stored.version());
        assertEquals(expected.id(), stored.report().id());
    }

    @Test
    void listVersions_deberiaDevolverMetadataEnOrdenAscendente() {
        when(repository.findByProjectIdOrderByVersionAsc(PROJECT_ID))
                .thenReturn(List.of(entity(1, "{}"), entity(2, "{}")));

        var versions = repo().listVersions(PROJECT_ID);

        assertEquals(2, versions.size());
        assertEquals(1, versions.get(0).version());
        assertEquals(2, versions.get(1).version());
        assertEquals(report().id(), versions.get(0).reportId());
        assertEquals("v1", versions.get(0).reportVersion());
        assertNotNull(versions.get(0).generatedAt());
    }

    @Test
    void listVersions_conProjectIdNull_deberiaDevolverListaVacia() {
        assertTrue(repo().listVersions(null).isEmpty());
    }

    @Test
    void save_conErrorDeSerializacion_deberiaLanzarIllegalState() throws JsonProcessingException {
        var failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {};
            }
        };
        var repoWithError = new JpaReportRepository(repository, failingMapper);
        when(repository.findFirstByProjectIdOrderByVersionDesc(PROJECT_ID))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> repoWithError.save(PROJECT_ID, report()));
    }

    @Test
    void find_conJsonInvalido_deberiaLanzarIllegalState() {
        when(repository.findFirstByProjectIdOrderByVersionDesc(PROJECT_ID))
                .thenReturn(Optional.of(entity(1, "not-json")));

        assertThrows(IllegalStateException.class, () -> repo().findLatest(PROJECT_ID));
    }

    @Test
    void contrato_deberiaSerReportRepository() {
        assertTrue(ReportRepository.class.isAssignableFrom(JpaReportRepository.class));
        assertFalse(JpaReportRepository.class.isInterface());
    }

    private String jsonOfReport(ConsultingReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
