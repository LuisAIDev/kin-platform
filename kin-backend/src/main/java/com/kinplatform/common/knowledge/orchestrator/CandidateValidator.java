package com.kinplatform.common.knowledge.orchestrator;

import com.kinplatform.common.knowledge.KnowledgeCandidate;
import com.kinplatform.common.knowledge.SourceValidation;

import java.util.List;

/**
 * Puerto de validación de candidatos (integración física): se ejecuta
 * exactamente antes del ranking, nunca después, y nunca se omite. El adaptador
 * concreto envuelve la validación determinista existente del dominio.
 */
public interface CandidateValidator {

    List<SourceValidation> validateAll(List<KnowledgeCandidate> candidates);
}


