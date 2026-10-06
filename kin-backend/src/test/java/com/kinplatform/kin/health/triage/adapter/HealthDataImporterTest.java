package com.kinplatform.kin.health.triage.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.health.triage.InMemoryTriageKnowledgeRepository;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.common.knowledge.KnowledgeResult;
import com.kinplatform.common.knowledge.engine.KnowledgeEngine;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HealthDataImporterTest {

    private static final String ENTRY = "{"
            + "\"name\":\"Migraña\",\"description\":\"Cefalea recurrente\",\"icdCode\":\"G43\","
            + "\"severity\":\"MODERADO\",\"urgency\":\"MEDIA\",\"recommendation\":\"Consultar.\","
            + "\"symptoms\":[{\"name\":\"dolor de cabeza\",\"weight\":0.9,\"required\":true}]}";

    private static HealthKnowledgeAdapter adapter() {
        return new HealthKnowledgeAdapter("health-catalog", "Health Catalog", false, "", null);
    }

    @Test
    void importFromExternal_conMotorConDatos_deberiaAplicar() {
        var engine = Mockito.mock(KnowledgeEngine.class);
        var repo = new InMemoryTriageKnowledgeRepository(TriageCatalog.empty());
        var parser = new HealthCatalogParser();
        var importer = new HealthDataImporter(engine, adapter(), parser, repo);

        var fact = com.kinplatform.common.knowledge.KnowledgeFact.of(
                ENTRY,
                "health-catalog",
                "https://who.int/h",
                java.time.OffsetDateTime.now(),
                com.kinplatform.common.knowledge.SourceTrust.OFFICIAL_PUBLIC,
                "SALUD");
        when(engine.evaluate(any()))
                .thenReturn(new KnowledgeResult(
                        List.of(fact), List.of("health-catalog"), List.of(), 1.0, "ok", "KnowledgeEngine", "v1"));

        var result = importer.importFromExternal();

        assertTrue(result.changed());
        assertTrue(
                repo.loadCatalog().conditions().stream().anyMatch(c -> c.name().equals("Migraña")));
        assertTrue(repo.loadCatalog().symptoms().stream().anyMatch(s -> s.name().equals("dolor de cabeza")));
    }

    @Test
    void importFromExternal_conMotorVacio_deberiaDegradarAlBundle() {
        var engine = Mockito.mock(KnowledgeEngine.class);
        when(engine.evaluate(any())).thenReturn(KnowledgeResult.empty());
        var repo = new InMemoryTriageKnowledgeRepository(TriageCatalog.empty());
        var importer = new HealthDataImporter(engine, adapter(), new HealthCatalogParser(), repo);

        var result = importer.importFromExternal();

        assertTrue(result.changed());
        assertTrue(repo.loadCatalog().conditions().size() >= 6);
    }

    @Test
    void importFromExternal_conMotorFallando_deberiaDegradarAlBundle() {
        var engine = Mockito.mock(KnowledgeEngine.class);
        when(engine.evaluate(any())).thenThrow(new RuntimeException("motor caído"));
        var repo = new InMemoryTriageKnowledgeRepository(TriageCatalog.empty());
        var importer = new HealthDataImporter(engine, adapter(), new HealthCatalogParser(), repo);

        var result = importer.importFromExternal();

        assertTrue(result.changed());
        assertFalse(repo.loadCatalog().isEmpty());
    }

    @Test
    void importFromExternal_conTodoFallando_deberiaConservarCatalogo() {
        var engine = Mockito.mock(KnowledgeEngine.class);
        when(engine.evaluate(any())).thenThrow(new RuntimeException("motor caído"));
        var repo = new InMemoryTriageKnowledgeRepository(TriageCatalog.empty());
        var broken = new HealthKnowledgeAdapter("health-catalog", "HC", false, "", "inexistente.json");
        var importer = new HealthDataImporter(engine, broken, new HealthCatalogParser(), repo);

        var result = importer.importFromExternal();

        assertEquals(0, result.conditionsAdded());
        assertTrue(repo.loadCatalog().isEmpty());
    }
}


