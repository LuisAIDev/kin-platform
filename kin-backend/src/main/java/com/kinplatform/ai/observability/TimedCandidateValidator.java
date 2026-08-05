package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceValidation;
import com.kinplatform.kin.knowledge.orchestrator.CandidateValidator;

import java.util.List;

/**
 * Decorador observador del {@link CandidateValidator} (Fase 7 — observabilidad).
 * Mide la latencia de validación y registra candidatos recibidos y descartados.
 * La validación se ejecuta exactamente antes del ranking (delegación intacta).
 */
public class TimedCandidateValidator implements CandidateValidator {

    private final CandidateValidator delegate;
    private final KnowledgeMetrics metrics;

    public TimedCandidateValidator(CandidateValidator delegate, KnowledgeMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics == null ? new KnowledgeMetrics(null) : metrics;
    }

    @Override
    public List<SourceValidation> validateAll(List<KnowledgeCandidate> candidates) {
        long start = System.nanoTime();
        List<SourceValidation> validations = delegate.validateAll(candidates);
        metrics.stage("validation", TimedQueryPlanner.toMs(start));
        int size = candidates == null ? 0 : candidates.size();
        long discarded = validations == null ? 0
            : validations.stream().filter(validation -> !validation.accepted()).count();
        metrics.candidatesReceived(size);
        metrics.candidatesDiscarded((int) discarded);
        return validations;
    }
}
