package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.orchestrator.ProviderRegistry;
import com.kinplatform.kin.knowledge.planner.ProviderType;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorador observador del {@link ProviderRegistry} (Fase 7 — observabilidad).
 * Mide la resolución del registro por {@link ProviderType} y envuelve cada
 * fuente en un {@link TimedKnowledgeSource} para medir latencia, errores y
 * timeouts del fetch. Métricas por tipo abstracto, nunca por proveedor concreto.
 */
public class TimedProviderRegistry implements ProviderRegistry {

    private final ProviderRegistry delegate;
    private final KnowledgeMetrics metrics;

    public TimedProviderRegistry(ProviderRegistry delegate, KnowledgeMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
    }

    @Override
    public List<KnowledgeSource> sourcesFor(ProviderType providerType) {
        long start = System.nanoTime();
        List<KnowledgeSource> sources = delegate.sourcesFor(providerType);
        metrics.providerRegistryLatency(name(providerType), TimedQueryPlanner.toMs(start));
        List<KnowledgeSource> wrapped = new ArrayList<>();
        if (sources != null) {
            for (KnowledgeSource source : sources) {
                wrapped.add(source == null ? null
                    : new TimedKnowledgeSource(providerType, source, metrics));
            }
        }
        return wrapped;
    }

    private static String name(ProviderType type) {
        return type == null ? "UNKNOWN" : type.name();
    }
}
