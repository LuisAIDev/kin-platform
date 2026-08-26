package com.kinplatform.kin.health.triage.adapter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.triage.domain.KeywordSymptomExtractor;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Benchmark del extractor NER V2 frente al keyword matching (ADR-028, fase de
 * consolidación). El criterio de aceptación exige una mejora ≥ 10 % en
 * precisión sobre un conjunto de prueba con textos clínicos en español.
 */
class OpenNLPSymptomExtractorV2Test {

    private static final UUID S1 = UUID.fromString("22220000-0000-0000-0000-000000000001");
    private static final UUID S2 = UUID.fromString("22220000-0000-0000-0000-000000000002");
    private static final UUID S3 = UUID.fromString("22220000-0000-0000-0000-000000000003");
    private static final UUID S4 = UUID.fromString("22220000-0000-0000-0000-000000000006");
    private static final UUID S5 = UUID.fromString("22220000-0000-0000-0000-000000000019");

    private static TriageCatalog catalog() {
        return new TriageCatalog(
                List.of(
                        Symptom.of(S1, "fiebre", "T", null, List.of("calentura", "fiebre alta")),
                        Symptom.of(S2, "tos", "T", null, List.of("tos seca")),
                        Symptom.of(S3, "dolor de garganta", "T", null, List.of("garganta irritada")),
                        Symptom.of(S4, "dolor de cabeza", "T", null, List.of("cefalea")),
                        Symptom.of(S5, "dolor muscular", "T", null, List.of("mialgia", "dolores musculares"))),
                List.of(),
                List.of());
    }

    /** Casos clínicos: texto → síntomas esperados (oro). */
    private record Case(String text, Set<String> expected) {}

    private static final List<Case> CORPUS = List.of(
            new Case("tengo tos seca, fiebre de 38° y dolor muscular", Set.of("tos", "fiebre", "dolor muscular")),
            new Case("no tengo fiebre pero me duele mucho la cabeza", Set.of("dolor de cabeza")),
            new Case(
                    "sin tos, con dolor de garganta y cefalea intensa", Set.of("dolor de garganta", "dolor de cabeza")),
            new Case("solo dolor muscular, sin fiebre", Set.of("dolor muscular")),
            new Case("me duele la garganta al tragar", Set.of("dolor de garganta")));

    private static double recall(Set<String> found, Set<String> expected) {
        if (expected.isEmpty()) {
            return found.isEmpty() ? 1.0 : 0.0;
        }
        long hits = found.stream().filter(expected::contains).count();
        return (double) hits / expected.size();
    }

    private static double precision(Set<String> found, Set<String> expected) {
        if (found.isEmpty()) {
            return expected.isEmpty() ? 1.0 : 0.0;
        }
        long hits = found.stream().filter(expected::contains).count();
        return (double) hits / found.size();
    }

    private static double f1(double p, double r) {
        return p + r == 0 ? 0.0 : 2 * p * r / (p + r);
    }

    @Test
    void v2_deberiaMejorarPrecisionAlMenos10PorCientoSobreKeywords() {
        var keyword = new KeywordSymptomExtractor();
        var v2 = new OpenNLPSymptomExtractorV2();
        var catalog = catalog();

        double kwF1 = corpusF1(keyword, catalog);
        double v2F1 = corpusF1(v2, catalog);

        assertTrue(
                v2F1 >= kwF1 * 1.10, "V2 debe superar keywords en ≥10% (keywords F1=" + kwF1 + ", V2 F1=" + v2F1 + ")");
    }

    @Test
    void v2_deberiaDetectarSintomasEnCorpusClinico() {
        var v2 = new OpenNLPSymptomExtractorV2();
        var catalog = catalog();

        var found = v2.extract(catalog, "tengo tos seca, fiebre de 38° y dolor muscular");

        assertTrue(found.contains("tos"));
        assertTrue(found.contains("fiebre"));
        assertTrue(found.contains("dolor muscular"));
    }

    @Test
    void v2_deberiaRespetarNegaciones() {
        var v2 = new OpenNLPSymptomExtractorV2();
        var catalog = catalog();

        var found = v2.extract(catalog, "no tengo fiebre pero me duele la cabeza");

        assertTrue(!found.contains("fiebre"));
        assertTrue(found.contains("dolor de cabeza"));
    }

    @Test
    void v2_deberiaSerDeterminista() {
        var v2 = new OpenNLPSymptomExtractorV2();
        var catalog = catalog();

        var a = v2.extract(catalog, "tengo fiebre y tos");
        var b = v2.extract(catalog, "tengo fiebre y tos");

        assertTrue(a.equals(b));
    }

    private static double corpusF1(
            com.kinplatform.kin.health.triage.domain.SymptomExtractor extractor, TriageCatalog catalog) {
        double sum = 0;
        for (Case c : CORPUS) {
            Set<String> found = Set.copyOf(extractor.extract(catalog, c.text()));
            sum += f1(precision(found, c.expected()), recall(found, c.expected()));
        }
        return sum / CORPUS.size();
    }
}
