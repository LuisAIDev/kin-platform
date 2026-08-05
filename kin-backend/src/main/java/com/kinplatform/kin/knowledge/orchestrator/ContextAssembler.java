package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeResult;

import java.util.List;

/**
 * Puerto de ensamblado (integración física): recibe únicamente resultados ya
 * rankeados y produce el {@link KnowledgeResult} del ciclo (normalización de
 * hechos, confianza y explicación deterministas). También fabrica resultados
 * vacíos con motivo auditable (offline-first).
 */
public interface ContextAssembler {

    KnowledgeResult assemble(KnowledgeQuery query, List<RankedCandidate> ranked);

    KnowledgeResult emptyResult(String reason);
}
