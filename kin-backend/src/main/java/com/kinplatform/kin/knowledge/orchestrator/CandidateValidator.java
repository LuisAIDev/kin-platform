package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceValidation;

import java.util.List;

/**
 * Puerto de validación de candidatos (integración física): se ejecuta
 * exactamente antes del ranking, nunca después, y nunca se omite. El adaptador
 * concreto envuelve la validación determinista existente del dominio.
 */
public interface CandidateValidator {

    List<SourceValidation> validateAll(List<KnowledgeCandidate> candidates);
}
