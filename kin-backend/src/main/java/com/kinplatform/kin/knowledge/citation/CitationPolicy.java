package com.kinplatform.kin.knowledge.citation;

import com.kinplatform.kin.knowledge.KnowledgeFact;

/**
 * Política de citación (Fase 5 — Citation Engine, Specification Pattern):
 * decide determinísticamente si un hecho puede citarse. Nunca inventa
 * información ni consulta fuentes.
 */
public interface CitationPolicy {

    CitationDecision decide(KnowledgeFact fact);
}
