package com.kinplatform.ai.knowledge.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinplatform.ai.knowledge.adapter.KnowledgeMetricsSummaryService.CacheSummary;
import com.kinplatform.ai.knowledge.adapter.KnowledgeMetricsSummaryService.KnowledgeMetricsSummary;
import com.kinplatform.ai.knowledge.adapter.KnowledgeMetricsSummaryService.SourceSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KnowledgeMetricsSummaryServiceTest {

    @Test
    void summarize_deberiaAgregarPorFuenteYCache() {
        var registry = new SimpleMeterRegistry();
        var metrics = new KnowledgeAdapterMetrics(registry);
        metrics.request("trm-col");
        metrics.request("trm-col");
        metrics.success("trm-col");
        metrics.failure("trm-col");
        metrics.timeout("trm-col");
        metrics.rejected("trm-col");
        metrics.rateLimited("trm-col");
        metrics.latency("trm-col", 400);
        metrics.latency("trm-col", 200);
        metrics.cacheHit();
        metrics.cacheMiss();
        metrics.cacheMiss();

        KnowledgeMetricsSummary summary = new KnowledgeMetricsSummaryService(registry).summarize();

        SourceSummary trm = summary.sources().get("trm-col");
        assertThat(trm).isNotNull();
        assertThat(trm.requests()).isEqualTo(2.0);
        assertThat(trm.success()).isEqualTo(1.0);
        assertThat(trm.failure()).isEqualTo(1.0);
        assertThat(trm.timeout()).isEqualTo(1.0);
        assertThat(trm.rejected()).isEqualTo(1.0);
        assertThat(trm.rateLimited()).isEqualTo(1.0);
        assertThat(trm.latencyP50Ms()).isNotNull();
        assertThat(trm.latencyP95Ms()).isNotNull();
        assertThat(trm.latencyP99Ms()).isNotNull();
        assertThat(trm.latencyP50Ms()).isLessThanOrEqualTo(trm.latencyP95Ms());

        CacheSummary cache = summary.cache();
        assertThat(cache.hits()).isEqualTo(1);
        assertThat(cache.misses()).isEqualTo(2);
        assertThat(cache.hitRatio()).isEqualTo(1.0 / 3.0);
        assertThat(summary.sources()).isInstanceOf(Map.class);
    }

    @Test
    void summarize_sinActividad_deberiaSerVacio() {
        KnowledgeMetricsSummary summary = new KnowledgeMetricsSummaryService(new SimpleMeterRegistry()).summarize();

        assertThat(summary.sources()).isEmpty();
        assertThat(summary.cache().hits()).isZero();
        assertThat(summary.cache().hitRatio()).isNull();
    }
}
