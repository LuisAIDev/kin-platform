package com.kinplatform.kin.knowledge.orchestrator;

import java.util.List;

/**
 * Puerto de ranking (integración física): ordena únicamente resultados ya
 * validados. No valida y no consulta políticas — solo ordena de forma
 * determinista por confianza derivada.
 */
public interface ContextRanker {

    List<RankedCandidate> rank(List<RankedCandidate> candidates);
}
