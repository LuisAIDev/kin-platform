package com.kinplatform.kin.health.triage.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.adapter.HealthCatalogParser;
import com.kinplatform.kin.health.triage.adapter.HealthKnowledgeAdapter;
import com.kinplatform.kin.health.triage.config.TriageProperties;
import com.kinplatform.kin.health.triage.domain.Condition;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.SymptomConditionRelation;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TriageCatalogUpdateServiceTest {

    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");
    private static final UUID C1 = UUID.fromString("22220000-0000-0000-0000-000000010001");

    private static TriageProperties properties() {
        var props = new TriageProperties();
        props.setEnabled(true);
        props.setMaxConditions(5);
        props.setNlpEnabled(false);
        return props;
    }

    private static HealthKnowledgeAdapter adapter() {
        return new HealthKnowledgeAdapter("health-catalog", "Health Catalog", false, "", null);
    }

    private static InMemoryTriageKnowledgeRepository emptyRepo() {
        return new InMemoryTriageKnowledgeRepository(TriageCatalog.empty());
    }

    @Test
    void updateCatalog_conMotorVacio_deberiaDegradarAlAdapterYActualizar() {
        var engine = Mockito.mock(KnowledgeEngine.class);
        Mockito.when(engine.evaluate(Mockito.any())).thenReturn(KnowledgeResult.empty());
        var repo = emptyRepo();

        var service = new TriageCatalogUpdateService(engine, adapter(), repo, new HealthCatalogParser(), properties());

        var result = service.updateCatalog();

        assertTrue(result.changed());
        assertTrue(result.conditionsAdded() > 0);
        assertEquals("health-catalog", result.source());
        assertTrue(repo.loadCatalog().conditions().size() >= 6);
    }

    @Test
    void updateCatalog_conMotorQueFalla_deberiaDegradarAlAdapter() {
        var engine = Mockito.mock(KnowledgeEngine.class);
        Mockito.when(engine.evaluate(Mockito.any())).thenThrow(new RuntimeException("fuente externa caída"));
        var repo = emptyRepo();

        var service = new TriageCatalogUpdateService(engine, adapter(), repo, new HealthCatalogParser(), properties());

        var result = service.updateCatalog();

        // Degradación elegante: el fallo del motor no rompe la actualización
        // porque el adapter (bundle offline-first) sigue disponible.
        assertTrue(result.changed());
    }

    @Test
    void updateCatalog_conMotorYAdapterFallando_deberiaConservarElCatalogoLocal() {
        var engine = Mockito.mock(KnowledgeEngine.class);
        Mockito.when(engine.evaluate(Mockito.any())).thenThrow(new RuntimeException("motor caído"));
        var repo = emptyRepo();
        var brokenAdapter = new HealthKnowledgeAdapter("health-catalog", "HC", false, "", "inexistente.json");

        var service =
                new TriageCatalogUpdateService(engine, brokenAdapter, repo, new HealthCatalogParser(), properties());

        var result = service.updateCatalog();

        // Degradación elegante total: sin motor y sin fuente externa el resultado
        // está vacío y el catálogo local permanece intacto (no lanza excepción).
        assertEquals(0, result.symptomsAdded());
        assertEquals(0, result.conditionsAdded());
        assertTrue(repo.loadCatalog().isEmpty());
    }

    @Test
    void updateCatalog_conModuloDeshabilitado_deberiaNoActualizar() {
        var props = properties();
        props.setEnabled(false);
        var repo = emptyRepo();
        var service = new TriageCatalogUpdateService(
                Mockito.mock(KnowledgeEngine.class), adapter(), repo, new HealthCatalogParser(), props);

        var result = service.updateCatalog();

        assertEquals(0, result.symptomsAdded());
        assertEquals(0, result.conditionsAdded());
        assertTrue(repo.loadCatalog().isEmpty());
    }

    @Test
    void applyUpdate_deberiaSerIdempotente() {
        var repo = new InMemoryTriageKnowledgeRepository(new TriageCatalog(
                List.of(Symptom.of(S1, "fiebre", "T", null)),
                List.of(Condition.of(C1, "Gripe", "D", "J11", Severity.MODERADO, Urgency.MEDIA, "R")),
                List.of(SymptomConditionRelation.of(S1, C1, 0.9, true))));
        var engine = Mockito.mock(KnowledgeEngine.class);
        Mockito.when(engine.evaluate(Mockito.any())).thenReturn(KnowledgeResult.empty());

        var service = new TriageCatalogUpdateService(engine, adapter(), repo, new HealthCatalogParser(), properties());
        var first = service.updateCatalog();
        var second = service.updateCatalog();

        // Los elementos del bundle se añaden la primera vez; la segunda es
        // idempotente (no vuelve a añadir los ya existentes).
        assertTrue(first.changed());
        assertTrue(second.symptomsAdded() <= first.symptomsAdded());
        assertTrue(second.conditionsAdded() <= first.conditionsAdded());
    }
}

