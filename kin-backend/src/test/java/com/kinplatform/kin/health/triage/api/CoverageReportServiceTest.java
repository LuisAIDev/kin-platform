package com.kinplatform.kin.health.triage.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.domain.ValidationStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoverageReportServiceTest {

    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010002");
    private static final UUID C2 = UUID.fromString("22220000-0000-0000-0000-000000010050");
    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");
    private static final UUID S2 = UUID.fromString("22220000-0000-0000-0000-000000000002");
    private static final UUID S3 = UUID.fromString("22220000-0000-0000-0000-000000000003");

    @Test
    void generate_deberiaCalcularCoberturaPorCondicion() {
        var catalog = new TriageCatalog(
                List.of(
                        Symptom.of(S1, "fiebre", "T", null),
                        Symptom.of(S2, "tos", "T", null),
                        Symptom.of(S3, "dolor de cabeza", "T", null)),
                List.of(
                        Condition.of(
                                C1,
                                "Gripe",
                                "D",
                                "J11",
                                Severity.MODERADO,
                                Urgency.MEDIA,
                                "R",
                                ValidationStatus.APPROVED),
                        Condition.of(
                                C2,
                                "Deshidratación",
                                "D",
                                "E86",
                                Severity.GRAVE,
                                Urgency.ALTA,
                                "R",
                                ValidationStatus.PENDING)),
                List.of(
                        SymptomConditionRelation.of(S1, C1, 0.9, true),
                        SymptomConditionRelation.of(S2, C1, 0.6, false),
                        SymptomConditionRelation.of(S3, C1, 0.5, false)));
        var service = new CoverageReportService(new InMemoryTriageKnowledgeRepository(catalog));

        var report = service.generate();

        assertEquals(2, report.totalConditions());
        // Gripe tiene 3 síntomas (adecuada); Deshidratación tiene 0 (insuficiente)
        assertEquals(1, report.adequateConditions());
        assertEquals(1, report.insufficientConditions());
        var gripe = report.items().stream()
                .filter(i -> i.name().equals("Gripe"))
                .findFirst()
                .orElseThrow();
        assertEquals(3, gripe.symptomCount());
        assertEquals("APPROVED", gripe.validationStatus());
        assertTrue(!gripe.insufficient());
        var deshidratacion = report.items().stream()
                .filter(i -> i.name().equals("Deshidratación"))
                .findFirst()
                .orElseThrow();
        assertTrue(deshidratacion.insufficient());
        assertEquals(0, deshidratacion.symptomCount());
    }

    @Test
    void generate_conCatalogoVacio_deberiaDevolverCero() {
        var service = new CoverageReportService(new InMemoryTriageKnowledgeRepository(TriageCatalog.empty()));

        var report = service.generate();

        assertEquals(0, report.totalConditions());
        assertTrue(report.items().isEmpty());
    }
}
