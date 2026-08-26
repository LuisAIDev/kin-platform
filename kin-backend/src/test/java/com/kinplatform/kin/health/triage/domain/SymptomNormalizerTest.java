package com.kinplatform.kin.health.triage.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SymptomNormalizerTest {

    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");
    private static final UUID S2 = UUID.fromString("22220000-0000-0000-0000-000000000002");

    private static TriageCatalog catalog() {
        return new TriageCatalog(
                List.of(
                        Symptom.of(S1, "dolor de cabeza", "Cefalea", null, List.of("cefalea", "jaqueca")),
                        Symptom.of(S2, "fiebre", "Temperatura elevada", null, List.of("calentura"))),
                List.of(),
                List.of());
    }

    @Test
    void normalizar_deberiaMinusculizarYLimpiarPuntuacion() {
        assertEquals("dolor de cabeza", SymptomNormalizer.normalize("Dolor de cabeza."));
        assertEquals("fiebre de 38", SymptomNormalizer.normalize("fiebre de 38°"));
    }

    @Test
    void normalizar_nullDeberiaDevolverVacio() {
        assertEquals("", SymptomNormalizer.normalize(null));
    }

    @Test
    void resolver_deberiaMapearAliasAlNombreCanonico() {
        assertEquals("dolor de cabeza", SymptomNormalizer.resolve(catalog(), "cefalea"));
        assertEquals("dolor de cabeza", SymptomNormalizer.resolve(catalog(), "jaqueca"));
        assertEquals("fiebre", SymptomNormalizer.resolve(catalog(), "calentura"));
    }

    @Test
    void resolver_deberiaMapearNombreCanonico() {
        assertEquals("fiebre", SymptomNormalizer.resolve(catalog(), "fiebre"));
    }

    @Test
    void resolver_sinCoincidencia_deberiaDevolverNull() {
        assertNull(SymptomNormalizer.resolve(catalog(), "dolor de rodilla"));
        assertNull(SymptomNormalizer.resolve(catalog(), ""));
        assertNull(SymptomNormalizer.resolve(null, "fiebre"));
    }

    @Test
    void allTerms_deberiaIncluirCanonicoYAliases() {
        var terms = SymptomNormalizer.allTerms(Symptom.of(S1, "dolor de cabeza", "Cefalea", null, List.of("cefalea")));

        assertEquals(List.of("dolor de cabeza", "cefalea"), terms);
    }

    @Test
    void allTerms_sinAliases_deberiaDevolverSoloElCanonico() {
        assertEquals(List.of("fiebre"), SymptomNormalizer.allTerms(Symptom.of(S2, "fiebre", "T", null)));
    }
}
