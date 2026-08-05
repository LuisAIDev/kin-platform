package com.kinplatform.kin.knowledge.orchestrator;

/**
 * Contrato de una estrategia de orquestación (especificación Fase 5, Strategy
 * Pattern). Cada estrategia es un objeto registrable que expresa preferencia de
 * caché, restricción offline y severidad ante fallos. OCP: nuevas estrategias
 * se registran sin modificar la máquina de estados.
 */
public interface OrchestrationStrategyPolicy {

    OrchestrationStrategy strategy();

    /** Prefiere reutilizar la caché antes de consultar fuentes externas. */
    boolean prefersCache();

    /** Restringe el ciclo a conocimiento local (sin Internet). */
    boolean offlineOnly();

    /** Trata cualquier fallo como fatal (Fail Fast) en vez de degradar. */
    boolean failureIsFatal();
}
