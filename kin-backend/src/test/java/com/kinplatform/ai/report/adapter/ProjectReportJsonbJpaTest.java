package com.kinplatform.ai.report.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kinplatform.platform.reporting.report.model.ConsultingReport;
import com.kinplatform.platform.reporting.report.model.ExecutiveSummary;
import com.kinplatform.platform.reporting.report.model.FinancialSection;
import com.kinplatform.platform.reporting.report.model.InnovationSection;
import com.kinplatform.platform.reporting.report.model.MarketSection;
import com.kinplatform.platform.reporting.report.model.NextStepsSection;
import com.kinplatform.platform.reporting.report.model.OpportunitiesSection;
import com.kinplatform.platform.reporting.report.model.RecommendationsSection;
import com.kinplatform.platform.reporting.report.model.ReportBuilder;
import com.kinplatform.platform.reporting.report.model.ReportMetadata;
import com.kinplatform.platform.reporting.report.model.RisksSection;
import com.kinplatform.platform.reporting.report.model.ScoresSection;
import com.kinplatform.platform.reporting.report.model.SourcesSection;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression test: {@code ProjectReportEntity.reportJson} es una columna
 * {@code jsonb} en PostgreSQL mapeada con {@code @JdbcTypeCode(SqlTypes.JSON)}
 * sobre un String JSON. Verifica el round-trip real del JSON contra PostgreSQL
 * 18 (Testcontainers, con Flyway V1..V15) sin el error
 * "column report_json is of type jsonb but expression is of type character
 * varying" (SQLState 42804).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
class ProjectReportJsonbJpaTest extends PostgresTestSupport {

    @Autowired
    private ProjectReportJpaRepository repository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private UUID projectId;

    @BeforeEach
    void seedProject() {
        var user = userRepository.save(User.builder()
                .email("report-jsonb-" + UUID.randomUUID() + "@test.com")
                .passwordHash("test-hash")
                .fullName("Jsonb Test")
                .role(UserRole.FREE)
                .build());
        var project = projectRepository.save(
                Project.builder().user(user).title("Proyecto Jsonb").build());
        projectId = project.getId();
    }

    private ConsultingReport report() {
        return ReportBuilder.create(UUID.randomUUID())
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

    private ProjectReportEntity entity(UUID projectId, int version, String json) {
        return ProjectReportEntity.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .version(version)
                .reportId(report().id())
                .reportVersion("v1")
                .generatedAt(OffsetDateTime.now())
                .reportJson(json)
                .build();
    }

    @Test
    void save_leer_actualizar_jsonb_deberiaFuncionarSinSQLState42804() throws Exception {
        var json = MAPPER.writeValueAsString(report());

        repository.save(entity(projectId, 1, json));
        repository.flush();

        var saved = repository.findFirstByProjectIdOrderByVersionDesc(projectId).orElseThrow();
        assertNotNull(saved.getId());
        // jsonb normaliza el JSON (claves reordenadas/espaciado); se compara semánticamente.
        assertEquals(MAPPER.readTree(json), MAPPER.readTree(saved.getReportJson()));

        var updated = MAPPER.createObjectNode()
                .put("actualizado", true)
                .put("contenido", "v\u00E1lido \u00FC\u00F1\u00EDcode");
        var updatedJson = MAPPER.writeValueAsString(updated);
        saved.setReportJson(updatedJson);
        repository.save(saved);
        repository.flush();
        entityManager.clear();

        var reloaded = repository.findFirstByProjectIdOrderByVersionDesc(projectId).orElseThrow();
        assertEquals(MAPPER.readTree(updatedJson), MAPPER.readTree(reloaded.getReportJson()));
    }

    @Test
    void jsonGrande_conCaracteresUnicode_deberiaPersistirse() throws Exception {
        var large = MAPPER.createObjectNode()
                .put("titulo", "\u00E1\u00E9\u00ED\u00F3\u00FA".repeat(200))
                .put("cuerpo", "x".repeat(5000));
        var largeJson = MAPPER.writeValueAsString(large);

        repository.save(entity(projectId, 1, largeJson));
        repository.flush();
        entityManager.clear();

        var saved = repository.findFirstByProjectIdOrderByVersionDesc(projectId).orElseThrow();
        assertEquals(MAPPER.readTree(largeJson), MAPPER.readTree(saved.getReportJson()));
    }

    @Test
    void versionesIncrementales_seConservanEnHistorico() throws Exception {
        var json = MAPPER.writeValueAsString(report());

        repository.save(entity(projectId, 1, json));
        repository.save(entity(projectId, 2, json));
        repository.flush();

        assertEquals(2, repository.findByProjectIdOrderByVersionAsc(projectId).size());
        assertTrue(repository.findByProjectIdAndVersion(projectId, 1).isPresent());
        assertTrue(repository.findByProjectIdAndVersion(projectId, 2).isPresent());
    }
}

