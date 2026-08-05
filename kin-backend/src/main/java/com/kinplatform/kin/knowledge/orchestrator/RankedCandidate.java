package com.kinplatform.kin.knowledge.orchestrator;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceValidation;

/**
 * Par inmutable candidato + validación (integración física): mantiene sincronizado
 * el resultado de la validación con el candidato que lo originó al reordenar.
 */
public record RankedCandidate(
    KnowledgeCandidate candidate,
    SourceValidation validation
) {

    public RankedCandidate {
        validation = validation == null ? SourceValidation.rejected("Sin validación") : validation;
    }
}
