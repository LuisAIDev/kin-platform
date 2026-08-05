package com.kinplatform.ai.observability;

import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.engine.DomainContextAssembler;
import com.kinplatform.kin.knowledge.engine.DomainContextRanker;
import com.kinplatform.kin.knowledge.engine.SourceRegistry;
import com.kinplatform.kin.knowledge.engine.SourceRegistryAdapter;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import com.kinplatform.kin.knowledge.engine.SourceValidatorAdapter;
import com.kinplatform.kin.knowledge.orchestrator.ContextAssembler;
import com.kinplatform.kin.knowledge.orchestrator.ExecutionEnvironment;
import com.kinplatform.kin.knowledge.orchestrator.KnowledgeOrchestrator;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationRequest;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationResult;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationState;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationStrategy;
import com.kinplatform.kin.knowledge.policy.KnowledgePolicyEngine;
import com.kinplatform.kin.knowledge.policy.PolicyConfig;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Runtime de Knowledge Engine con observabilidad Enterprise (Fase 7).
 *
 * <p>Compone el orquestador con colaboradores observados (decoradores) y, al
 * ejecutar el ciclo, registra métricas por etapa (policy/planner/cache/registry/
 * fetch/validation/ranking/assembler/citation), métricas del orquestador
 * derivadas del resultado (estados, degradaciones, offline, graceful, fail-fast,
 * presupuesto) y logs estructurados con correlación. Vive en infraestructura:
 * NO modifica el dominio y produce exactamente el mismo {@link KnowledgeResult}
 * que el {@code KnowledgeGateway} con los mismos colaboradores.</p>
 */
public final class ObservableKnowledgeRuntime {

    private final KnowledgeOrchestrator orchestrator;
    private final KnowledgeMetrics metrics;
    private final ContextAssembler assembler;

    private ObservableKnowledgeRuntime(KnowledgeOrchestrator orchestrator, KnowledgeMetrics metrics,
                                       ContextAssembler assembler) {
        this.orchestrator = orchestrator;
        this.metrics = metrics;
        this.assembler = assembler;
    }

    /**
     * Construye el runtime observado sobre las fuentes/validador/caché dados.
     */
    public static ObservableKnowledgeRuntime create(SourceRegistry registry, SourceValidator validator,
                                                    KnowledgeRepository repository, MeterRegistry meterRegistry) {
        KnowledgeMetrics metrics = new KnowledgeMetrics(meterRegistry);
        var planner = new TimedQueryPlanner(metrics);
        var policy = new TimedPolicyEngine(metrics);
        var providerRegistry = new TimedProviderRegistry(new SourceRegistryAdapter(registry), metrics);
        var candidateValidator = new TimedCandidateValidator(new SourceValidatorAdapter(validator), metrics);
        var ranker = new TimedContextRanker(new DomainContextRanker(), metrics);
        var assembler = new TimedContextAssembler(new DomainContextAssembler(), metrics);
        var repo = new TimedKnowledgeRepository(repository, metrics);
        var orchestrator = new KnowledgeOrchestrator(planner, policy, providerRegistry, repo,
            candidateValidator, ranker, assembler);
        return new ObservableKnowledgeRuntime(orchestrator, metrics, assembler);
    }

    public KnowledgeMetrics metrics() {
        return metrics;
    }

    public KnowledgeOrchestrator orchestrator() {
        return orchestrator;
    }

    /**
     * Ejecuta el ciclo de conocimiento con observación. Devuelve el mismo
     * {@link KnowledgeResult} que el gateway (comportamiento idéntico).
     */
    public KnowledgeResult acquire(KnowledgeRequest request) {
        CorrelationContext.Correlation correlation = CorrelationContext.current();
        long start = System.nanoTime();
        try {
            if (request == null || request.topic() == null || request.topic().isBlank()) {
                KnowledgeResult empty = assembler.emptyResult("Tema vacío; no se consultaron fuentes.");
                logCycle(correlation, start, "EMPTY");
                return empty;
            }
            var orchestration = orchestrator.coordinateWithResult(OrchestrationRequest.of(
                request, PolicyConfig.defaults(), OrchestrationStrategy.GRACEFUL_DEGRADATION,
                ExecutionEnvironment.online()));
            long tookMs = TimedQueryPlanner.toMs(start);
            recordOrchestration(orchestration.orchestration());
            recordKnowledge(orchestration.knowledge());
            metrics.cycle(tookMs, orchestration.orchestration().finalState().name());
            KnowledgeResult knowledge = orchestration.knowledge();
            if (knowledge == null) {
                String reason = orchestration.orchestration().failureReason();
                knowledge = assembler.emptyResult(reason == null || reason.isBlank()
                    ? "No se obtuvo conocimiento externo." : reason);
            }
            logCycle(correlation, start, knowledge.isEmpty() ? "EMPTY" : "OK");
            return knowledge;
        } catch (RuntimeException ex) {
            long tookMs = TimedQueryPlanner.toMs(start);
            metrics.cycle(tookMs, "ERROR");
            metrics.providerError("UNKNOWN");
            KnowledgeStructuredLog.error(ex.getClass().getSimpleName());
            return assembler.emptyResult("Error en el ciclo de conocimiento: " + ex.getMessage());
        }
    }

    private void recordOrchestration(OrchestrationResult orchestration) {
        for (OrchestrationState state : orchestration.statesVisited()) {
            metrics.stateTransition(state.name());
        }
        if (orchestration.degraded()) {
            metrics.degraded();
        }
        if (orchestration.plan() != null) {
            OrchestrationStrategy strategy = orchestration.plan().strategy();
            if (strategy == OrchestrationStrategy.OFFLINE_MODE) {
                metrics.offlineMode();
            }
            if (strategy == OrchestrationStrategy.GRACEFUL_DEGRADATION) {
                metrics.gracefulDegradation();
            }
            if (strategy == OrchestrationStrategy.FAIL_FAST) {
                metrics.failFast();
            }
        }
        if (orchestration.decisions().stream()
            .anyMatch(decision -> decision.type() == OrchestrationDecisionType.STOP_CONSULTS)) {
            metrics.budgetExhausted();
        }
        metrics.providersSelected(orchestration.selectedProviderTypes().size());
    }

    private void recordKnowledge(KnowledgeResult knowledge) {
        if (knowledge == null) {
            return;
        }
        metrics.sourcesAccepted(knowledge.factCount());
        long rejected = knowledge.validations().stream()
            .filter(validation -> !validation.accepted()).count();
        metrics.sourcesRejected((int) rejected);
        metrics.averageScore(knowledge.confidence());
        metrics.averageConfidence(knowledge.confidence());
    }

    private void logCycle(CorrelationContext.Correlation correlation, long startNanos, String result) {
        CorrelationContext.set(correlation);
        KnowledgeStructuredLog.cycle(TimedQueryPlanner.toMs(startNanos), result);
    }
}
