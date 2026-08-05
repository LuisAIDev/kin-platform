package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceValidation;
import com.kinplatform.kin.knowledge.orchestrator.CandidateValidator;

import java.util.List;

/**
 * Adaptador de integración: expone el {@link SourceValidator} del núcleo congelado
 * (ADR-014) tras el puerto {@link CandidateValidator}. Garantiza que la validación
 * determinista se ejecute exactamente antes del ranking y nunca se omita.
 */
public class SourceValidatorAdapter implements CandidateValidator {

    private final SourceValidator validator;

    public SourceValidatorAdapter(SourceValidator validator) {
        this.validator = validator == null ? SourceValidator.strict() : validator;
    }

    @Override
    public List<SourceValidation> validateAll(List<KnowledgeCandidate> candidates) {
        return validator.validateAll(candidates);
    }
}
