package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.orchestrator.ContextAssembler;
import com.kinplatform.kin.knowledge.orchestrator.ExecutionEnvironment;
import com.kinplatform.kin.knowledge.orchestrator.KnowledgeOrchestrator;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationRequest;
import com.kinplatform.kin.knowledge.orchestrator.OrchestrationStrategy;
import com.kinplatform.kin.knowledge.policy.KnowledgePolicyEngine;
import com.kinplatform.kin.knowledge.policy.PolicyConfig;
import com.kinplatform.kin.knowledge.planner.QueryPlanner;

/**
 * Punto de composición de la adquisición de conocimiento (ADR-014, §5.2) tras la
 * integración física del Knowledge Engine.
 *
 * <p>Ya no contiene lógica de coordinación: construye el request de orquestación,
 * delega el ciclo completo al {@link KnowledgeOrchestrator} y devuelve el
 * {@link KnowledgeResult}. No conoce estrategias, políticas ni fuentes concretas.
 * Sin red o sin fuentes, degrada con gracia a un resultado vacío (offline-first).</p>
 *
 * <p>El contrato público {@code acquire(KnowledgeRequest) → KnowledgeResult} y los
 * constructores existentes se conservan; el comportamiento observable es idéntico
 * al núcleo congelado (las métricas de confianza/explicación se replican en el
 * ensamblador de dominio).</p>
 */
public class KnowledgeGateway {

    private final KnowledgeOrchestrator orchestrator;
    private final ContextAssembler assembler;

    public KnowledgeGateway(SourceRegistry registry, SourceValidator validator) {
        this(registry, validator, null);
    }

    /**
     * @param registry   registro de fuentes; si es {@code null} se usa uno vacío
     * @param validator  validador de candidatos; si es {@code null} se usa el
     *                   validador estricto (offline-first)
     * @param repository caché de resultados validados (puede ser {@code null}:
     *                   sin caché, como el núcleo congelado)
     */
    public KnowledgeGateway(SourceRegistry registry, SourceValidator validator,
                            KnowledgeRepository repository) {
        var safeRegistry = registry == null ? SourceRegistry.empty() : registry;
        var safeValidator = validator == null ? SourceValidator.strict() : validator;
        this.orchestrator = new KnowledgeOrchestrator(
            new QueryPlanner(), new KnowledgePolicyEngine(),
            new SourceRegistryAdapter(safeRegistry), repository,
            new SourceValidatorAdapter(safeValidator),
            new DomainContextRanker(), new DomainContextAssembler());
        this.assembler = new DomainContextAssembler();
    }

    /**
     * Factory de inyección completa (integración): permite cablear un
     * {@link KnowledgeOrchestrator} con colaboradores de ejecución y un
     * {@link ContextAssembler} propios.
     */
    public static KnowledgeGateway wired(KnowledgeOrchestrator orchestrator, ContextAssembler assembler) {
        return new KnowledgeGateway(orchestrator, assembler);
    }

    private KnowledgeGateway(KnowledgeOrchestrator orchestrator, ContextAssembler assembler) {
        this.orchestrator = orchestrator;
        this.assembler = assembler == null ? new DomainContextAssembler() : assembler;
    }

    /**
     * Adquiere y normaliza conocimiento para una {@link KnowledgeRequest},
     * delegando el ciclo completo al orquestador. Sin red, degrada con gracia a
     * un resultado vacío (offline-first).
     */
    public KnowledgeResult acquire(KnowledgeRequest request) {
        if (request == null || request.topic() == null || request.topic().isBlank()) {
            return assembler.emptyResult("Tema vacío; no se consultaron fuentes.");
        }
        var orchestration = orchestrator.coordinateWithResult(OrchestrationRequest.of(
            request, PolicyConfig.defaults(), OrchestrationStrategy.GRACEFUL_DEGRADATION,
            ExecutionEnvironment.online()));
        KnowledgeResult knowledge = orchestration.knowledge();
        if (knowledge != null) {
            return knowledge;
        }
        String reason = orchestration.orchestration().failureReason();
        return assembler.emptyResult(reason == null || reason.isBlank()
            ? "No se obtuvo conocimiento externo." : reason);
    }
}
