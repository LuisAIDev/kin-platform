package com.kinplatform.kin.health.triage.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.SourceTrust;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HealthCatalogParserTest {

    private static final HealthCatalogParser PARSER = new HealthCatalogParser();

    private static String jsonEntry(String name, String severity, String urgency, String symptom, double weight) {
        return "{"
                + "\"name\":\"" + name + "\","
                + "\"description\":\"Desc " + name + "\","
                + "\"icdCode\":\"X00\","
                + "\"severity\":\"" + severity + "\","
                + "\"urgency\":\"" + urgency + "\","
                + "\"recommendation\":\"Consultar médico.\","
                + "\"symptoms\":[{\"name\":\"" + symptom + "\",\"weight\":" + weight + ",\"required\":true}]"
                + "}";
    }

    @Test
    void parseCandidates_deberiaConstruirCatalogUpdate() {
        var candidate = new KnowledgeCandidate(
                jsonEntry("Candidiasis vaginal", "LEVE", "MEDIA", "picazón en la piel", 0.8),
                "health-catalog",
                "Health Catalog",
                "https://www.who.int/health-topics/x",
                OffsetDateTime.now(),
                "application/json",
                java.util.Map.of());

        var update = PARSER.parseCandidates(List.of(candidate));

        assertFalse(update.isEmpty());
        assertEquals(1, update.conditions().size());
        assertEquals("Candidiasis vaginal", update.conditions().get(0).name());
        assertEquals(1, update.symptoms().size());
        assertEquals("picazón en la piel", update.symptoms().get(0).name());
        assertEquals(1, update.relations().size());
        assertTrue(update.relations().get(0).required());
        assertEquals(0.8, update.relations().get(0).weight(), 0.001);
        assertEquals("health-catalog", update.source());
    }

    @Test
    void parseCandidates_conContenidoNoEstructurado_deberiaIgnorar() {
        var candidate = new KnowledgeCandidate(
                "texto libre sin JSON",
                "src",
                "Src",
                "https://example.com",
                OffsetDateTime.now(),
                "application/json",
                java.util.Map.of());

        var update = PARSER.parseCandidates(List.of(candidate));

        assertTrue(update.isEmpty());
    }

    @Test
    void parseFacts_deberiaConstruirCatalogUpdate() {
        var fact = KnowledgeFact.of(
                jsonEntry("Hepatitis aguda", "GRAVE", "ALTA", "ictericia", 0.9),
                "health-catalog",
                "https://www.who.int/health-topics/h",
                OffsetDateTime.now(),
                SourceTrust.OFFICIAL_PUBLIC,
                "SALUD");

        var update = PARSER.parseFacts(List.of(fact));

        assertFalse(update.isEmpty());
        assertEquals(1, update.conditions().size());
        assertEquals("Hepatitis aguda", update.conditions().get(0).name());
        assertEquals(1, update.symptoms().size());
        assertEquals("ictericia", update.symptoms().get(0).name());
    }

    @Test
    void parseCandidates_vacio_deberiaProducirActualizacionVacia() {
        assertTrue(PARSER.parseCandidates(List.of()).isEmpty());
        assertTrue(PARSER.parseFacts(List.of()).isEmpty());
    }

    @Test
    void parseCandidates_deberiaDeduplicarPorNombre() {
        var c1 = new KnowledgeCandidate(
                jsonEntry("Meningitis", "GRAVE", "ALTA", "rigidez de nuca", 0.95),
                "health-catalog",
                "Health Catalog",
                "https://www.who.int/health-topics/m",
                OffsetDateTime.now(),
                "application/json",
                java.util.Map.of());
        var c2 = new KnowledgeCandidate(
                jsonEntry("Meningitis", "GRAVE", "ALTA", "fiebre", 0.7),
                "health-catalog",
                "Health Catalog",
                "https://www.who.int/health-topics/m",
                OffsetDateTime.now(),
                "application/json",
                java.util.Map.of());

        var update = PARSER.parseCandidates(List.of(c1, c2));

        assertEquals(1, update.conditions().size());
        assertEquals(2, update.symptoms().size());
        assertEquals(2, update.relations().size());
    }

    @Test
    void ids_deberianSerDeterministas() {
        var c1 = new KnowledgeCandidate(
                jsonEntry("Erisipela", "GRAVE", "ALTA", "enrojecimiento de la piel", 0.9),
                "health-catalog",
                "Health Catalog",
                "https://www.who.int/health-topics/e",
                OffsetDateTime.now(),
                "application/json",
                java.util.Map.of());

        var a = PARSER.parseCandidates(List.of(c1));
        var b = PARSER.parseCandidates(List.of(c1));

        assertEquals(a.conditions().get(0).id(), b.conditions().get(0).id());
        assertEquals(a.symptoms().get(0).id(), b.symptoms().get(0).id());
        assertEquals(UUID.class, a.conditions().get(0).id().getClass());
    }
}
