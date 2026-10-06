package com.kinplatform.common.knowledge.orchestrator;

import com.kinplatform.common.knowledge.KnowledgeCandidate;
import com.kinplatform.common.knowledge.SourceValidation;

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


