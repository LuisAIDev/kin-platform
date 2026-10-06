package com.kinplatform.common.knowledge.engine;

import com.kinplatform.common.knowledge.KnowledgeSource;
import com.kinplatform.common.knowledge.orchestrator.ProviderRegistry;
import com.kinplatform.common.knowledge.planner.ProviderType;

import java.util.List;

/**
 * Adaptador de integración: expone el {@link SourceRegistry} del núcleo congelado
 * (ADR-014) tras el puerto {@link ProviderRegistry}. Resuelve un
 * {@link ProviderType} abstracto a las fuentes disponibles del registro;
 * el filtrado fino por tipo queda como configuración futura (los adaptadores
 * actuales no declaran tipo).
 *
 * <p>Nunca nombra proveedores concretos y vive en infraestructura del dominio
 * (engine), manteniendo el orquestador libre de implementaciones.</p>
 */
public class SourceRegistryAdapter implements ProviderRegistry {

    private final SourceRegistry registry;

    public SourceRegistryAdapter(SourceRegistry registry) {
        this.registry = registry == null ? SourceRegistry.empty() : registry;
    }

    @Override
    public List<KnowledgeSource> sourcesFor(ProviderType providerType) {
        return registry.all();
    }
}


