package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.planner.ProviderType;

import java.util.List;

/**
 * Puerto de registro de proveedores (integración física del Knowledge Engine).
 * Resuelve un {@link ProviderType} abstracto en las fuentes de conocimiento
 * disponibles — nunca proveedores concretos (DIAN, DANE, Google…). El dominio
 * no conoce implementaciones; los adaptadores viven en infraestructura/engine.
 */
public interface ProviderRegistry {

    List<KnowledgeSource> sourcesFor(ProviderType providerType);
}
