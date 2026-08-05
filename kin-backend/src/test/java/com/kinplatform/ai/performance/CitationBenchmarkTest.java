package com.kinplatform.ai.performance;

import com.kinplatform.kin.knowledge.KnowledgeFact;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.citation.CitationEngine;
import com.kinplatform.kin.knowledge.citation.CitationStyle;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Benchmark del Citation Engine (Fase 8): tiempo de generación, costo por número
 * de fuentes y deduplicación.
 */
class CitationBenchmarkTest {

    private static final int ITERATIONS = 300;

    @Test
    void benchmarkPorNumeroDeFuentes() {
        var engine = new CitationEngine();
        int[] sizes = {1, 5, 20};

        for (int size : sizes) {
            var result = resultWith(size, false);
            var stats = BenchmarkSupport.measure(
                () -> engine.produce(result, null, CitationStyle.APPENDIX), ITERATIONS);
            double costPerSource = stats.avgMs() / size;
            System.out.printf("[PERF] citation fuentes=%d: avg=%.3fms p95=%.3fms costo/fuente=%.4fms%n",
                size, stats.avgMs(), stats.p95Ms(), costPerSource);

            var citation = engine.produce(result, null, CitationStyle.APPENDIX);
            assertEquals(size, citation.bundle().entries().size());
        }
    }

    @Test
    void dedup_deberiaReducirEntradas() {
        var engine = new CitationEngine();
        var result = resultWith(10, true);

        var citation = engine.produce(result, null, CitationStyle.INLINE);

        assertEquals(10, result.facts().size());
        assertEquals(1, citation.bundle().entries().size());
    }

    private static KnowledgeResult resultWith(int count, boolean duplicates) {
        var facts = new ArrayList<KnowledgeFact>();
        for (int i = 0; i < count; i++) {
            int idx = duplicates ? 0 : i;
            facts.add(KnowledgeFact.of("Dato de mercado verificado con contexto suficiente. ",
                "src-" + idx, "https://example.com/" + idx, OffsetDateTime.now().minusDays(2),
                SourceTrust.OFFICIAL_PUBLIC, "MERCADO"));
        }
        return new KnowledgeResult(facts, List.of(), List.of(), 1.0, "ok", "KnowledgeEngine", "v1");
    }
}
