package com.kinplatform.kin.health.triage.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import com.kinplatform.test.PostgresTestSupport;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración del adaptador JPA de triaje con PostgreSQL real (Testcontainers).
 *
 * <p>Verifica que Flyway V21/V22 crea el esquema, que el catálogo arranca con
 * 50 condiciones y 100+ relaciones (ADR-028, fase profesional), que el
 * historial por usuario persiste/consulta correctamente (JSONB) y que
 * {@code applyUpdate} es un upsert idempotente. Cada test usa un
 * {@code userId} distinto para no compartir datos residuales.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class JpaTriageRepositoryIntegrationTest extends PostgresTestSupport {

    @Autowired
    private JpaTriageKnowledgeRepository knowledgeRepository;

    @Autowired
    private TriageConsultationRepository consultationRepository;

    @Test
    @Transactional
    void catalogo_deberiaArrancarCon100CondicionesY200Relaciones() {
        TriageCatalog catalog = knowledgeRepository.loadCatalog();

        assertFalse(catalog.isEmpty());
        assertTrue(catalog.symptoms().size() >= 90, "debe haber al menos 90 síntomas sembrados");
        assertTrue(catalog.conditions().size() >= 100, "debe haber al menos 100 condiciones sembradas");
        assertTrue(catalog.relations().size() >= 200, "debe haber al menos 200 relaciones síntoma-condición");
        assertTrue(catalog.conditionById(UUID.fromString("22220000-0000-0000-0000-000000010002"))
                .isPresent());
        assertTrue(
                catalog.conditionById(UUID.fromString("22220000-0000-0000-0000-000000010100"))
                        .isPresent(),
                "debe existir la condición 100 (Sepsis)");
    }

    @Test
    @Transactional
    void catalogo_deberiaCargarValidationStatus() {
        TriageCatalog catalog = knowledgeRepository.loadCatalog();

        var sepsis = catalog.conditionById(UUID.fromString("22220000-0000-0000-0000-000000010100"))
                .orElseThrow();
        assertEquals(com.kinplatform.kin.health.triage.domain.ValidationStatus.APPROVED, sepsis.validationStatus());
        var resfriado = catalog.conditionById(UUID.fromString("22220000-0000-0000-0000-000000010001"))
                .orElseThrow();
        assertEquals(com.kinplatform.kin.health.triage.domain.ValidationStatus.PENDING, resfriado.validationStatus());
    }

    @Test
    @Transactional
    void catalogo_deberiaCargarAliasesDeSintomas() {
        TriageCatalog catalog = knowledgeRepository.loadCatalog();

        var cefalea = catalog.symptomById(UUID.fromString("22220000-0000-0000-0000-000000000006"))
                .orElseThrow();
        assertFalse(cefalea.aliases().isEmpty(), "dolor de cabeza debe tener aliases (p. ej. cefalea)");
        assertTrue(cefalea.aliases().contains("cefalea"));
    }

    @Test
    @Transactional
    void applyUpdate_deberiaAgregarElementosNuevos() {
        var symptomId = UUID.randomUUID();
        var conditionId = UUID.randomUUID();
        var update = com.kinplatform.kin.health.triage.domain.CatalogUpdate.of(
                "test-source",
                List.of(com.kinplatform.kin.health.triage.domain.Symptom.of(symptomId, "síntoma test", "desc", "X00")),
                List.of(com.kinplatform.kin.health.triage.domain.Condition.of(
                        conditionId, "Condición test", "desc", "X99", Severity.LEVE, Urgency.BAJA, "recomendación")),
                List.of(com.kinplatform.kin.health.triage.domain.SymptomConditionRelation.of(
                        symptomId, conditionId, 0.5, false)));

        var result = knowledgeRepository.applyUpdate(update);

        assertEquals(1, result.symptomsAdded());
        assertEquals(1, result.conditionsAdded());
        assertEquals(1, result.relationsAdded());
    }

    @Test
    @Transactional
    void applyUpdate_deberiaSerIdempotente() {
        var symptomId = com.kinplatform.kin.engine.DeterministicId.from("triage-symptom", "síntoma idem", "X00");
        var conditionId = com.kinplatform.kin.engine.DeterministicId.from("triage-condition", "Condición idem", "X99");
        var update = com.kinplatform.kin.health.triage.domain.CatalogUpdate.of(
                "test-source",
                List.of(com.kinplatform.kin.health.triage.domain.Symptom.of(symptomId, "síntoma idem", "desc", "X00")),
                List.of(com.kinplatform.kin.health.triage.domain.Condition.of(
                        conditionId, "Condición idem", "desc", "X99", Severity.LEVE, Urgency.BAJA, "recomendación")),
                List.of(com.kinplatform.kin.health.triage.domain.SymptomConditionRelation.of(
                        symptomId, conditionId, 0.5, false)));

        knowledgeRepository.applyUpdate(update);
        var second = knowledgeRepository.applyUpdate(update);

        assertEquals(0, second.symptomsAdded());
        assertEquals(0, second.conditionsAdded());
        assertEquals(0, second.relationsAdded());
    }

    @Test
    @Transactional
    void catalogo_deberiaResolverRelacionesPorCondicion() {
        TriageCatalog catalog = knowledgeRepository.loadCatalog();

        var gripe = catalog.conditionById(UUID.fromString("22220000-0000-0000-0000-000000010002"))
                .orElseThrow();
        var relations = catalog.relationsForCondition(gripe.id());

        assertFalse(relations.isEmpty());
        assertTrue(relations.stream().anyMatch(r -> r.required()));
    }

    @Test
    @Transactional
    void historial_deberiaGuardarYConsultarPorUsuario() {
        UUID userId = UUID.randomUUID();
        var condition = knowledgeRepository.loadCatalog().conditions().get(0);
        var result = new TriageConditionResult(
                condition.id(),
                condition.name(),
                condition.description(),
                0.75,
                Severity.MODERADO,
                Urgency.MEDIA,
                "Consultar médico.",
                List.of("fiebre"));
        var consultation = TriageConsultation.of(
                UUID.randomUUID(), userId, List.of("fiebre", "tos"), List.of(result), OffsetDateTime.now());

        consultationRepository.save(consultation);

        var history = consultationRepository.findByUserId(userId);
        assertEquals(1, history.size());
        assertEquals(2, history.get(0).symptoms().size());
        assertEquals(1, history.get(0).results().size());
        assertEquals(condition.id(), history.get(0).results().get(0).conditionId());
        assertNotNull(history.get(0).createdAt());
    }

    @Test
    @Transactional
    void historial_deberiaAislarPorUsuario() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        var condition = knowledgeRepository.loadCatalog().conditions().get(0);
        var result = new TriageConditionResult(
                condition.id(),
                condition.name(),
                condition.description(),
                0.75,
                Severity.LEVE,
                Urgency.BAJA,
                "Observar evolución.",
                List.of("tos"));
        consultationRepository.save(
                TriageConsultation.of(UUID.randomUUID(), owner, List.of("tos"), List.of(result), OffsetDateTime.now()));

        assertTrue(consultationRepository.findByUserId(owner).size() == 1);
        assertTrue(consultationRepository.findByUserId(other).isEmpty());
    }
}
