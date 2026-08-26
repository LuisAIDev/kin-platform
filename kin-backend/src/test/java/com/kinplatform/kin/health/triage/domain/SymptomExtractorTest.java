package com.kinplatform.kin.health.triage.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SymptomExtractorTest {

    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");
    private static final UUID S2 = UUID.fromString("22220000-0000-0000-0000-000000000002");
    private static final UUID S3 = UUID.fromString("22220000-0000-0000-0000-000000000003");
    private static final UUID S4 = UUID.fromString("22220000-0000-0000-0000-000000000004");

    private static TriageCatalog catalog() {
        return new TriageCatalog(
                List.of(
                        Symptom.of(S1, "fiebre", "Temperatura elevada", null, List.of("calentura", "fiebre alta")),
                        Symptom.of(S2, "tos", "Tos", null, List.of("tos seca")),
                        Symptom.of(S3, "dolor de cabeza", "Cefalea", null, List.of("cefalea")),
                        Symptom.of(S4, "dolor muscular", "Dolores musculares", null, List.of("mialgia"))),
                List.of(),
                List.of());
    }

    private static final KeywordSymptomExtractor KEYWORD = new KeywordSymptomExtractor();

    @Test
    void extraer_deberiaDetectarSintomasPorSubcadena() {
        var found = KEYWORD.extract(catalog(), "Tengo fiebre y mucha tos");

        assertTrue(found.contains("fiebre"));
        assertTrue(found.contains("tos"));
        assertEquals(2, found.size());
    }

    @Test
    void extraer_deberiaIgnorarMayusculas() {
        var found = KEYWORD.extract(catalog(), "FIEBRE alta");

        assertEquals(List.of("fiebre"), found);
    }

    @Test
    void extraer_conTextoVacio_deberiaDevolverVacio() {
        assertTrue(KEYWORD.extract(catalog(), "  ").isEmpty());
        assertTrue(KEYWORD.extract(catalog(), null).isEmpty());
        assertTrue(KEYWORD.extract(null, "fiebre").isEmpty());
    }

    @Test
    void extraer_sinCoincidencias_deberiaDevolverVacio() {
        assertTrue(KEYWORD.extract(catalog(), "cuéntame sobre mi proyecto").isEmpty());
    }

    @Test
    void extraer_deberiaResolverAliasesAlNombreCanonico() {
        var found = KEYWORD.extract(catalog(), "tengo cefalea y tos seca");

        assertTrue(found.contains("dolor de cabeza"));
        assertTrue(found.contains("tos"));
        assertEquals(2, found.size());
    }

    @Test
    void extraer_deberiaResolverMensajeProfesional() {
        var found = KEYWORD.extract(catalog(), "tengo tos seca, fiebre de 38° y dolor muscular");

        assertTrue(found.contains("tos"));
        assertTrue(found.contains("fiebre"));
        assertTrue(found.contains("dolor muscular"));
        assertEquals(3, found.size());
    }
}
