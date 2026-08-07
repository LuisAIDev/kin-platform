package com.kinplatform.kin.knowledge.orchestrator;

import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.ASSEMBLY_OK;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.CACHE_ONLY;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.CONSULT_EXTERNAL;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.DEGRADE;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.FAIL;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.FETCH_COORDINATED;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.FINALIZE;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.NO_CONSULT;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.RANKING_OK;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.STOP;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.STOP_CONSULTS;
import static com.kinplatform.kin.knowledge.orchestrator.OrchestrationDecisionType.VALIDATION_OK;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeRepository;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.KnowledgeResult;
import com.kinplatform.kin.knowledge.KnowledgeSource;
import com.kinplatform.kin.knowledge.planner.PlannedQuery;
import com.kinplatform.kin.knowledge.planner.ProviderType;
import com.kinplatform.kin.knowledge.planner.QueryPlan;
import com.kinplatform.kin.knowledge.planner.QueryPlanner;
import com.kinplatform.kin.knowledge.policy.ContextBudget;
import com.kinplatform.kin.knowledge.policy.CostBudgetUsage;
import com.kinplatform.kin.knowledge.policy.KnowledgePolicyEngine;
import com.kinplatform.kin.knowledge.policy.ProviderSelection;
import com.kinplatform.kin.knowledge.policy.QueryMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coordinador determinista del ciclo de conocimiento (especificación Fase 5).
 *
 * <p>POJO puro de dominio: nunca ejecuta proveedores, nunca llama HTTP, nunca
 * conoce implementaciones ni infraestructura. Coordina mediante una máquina de
 * estados (State Pattern) y delega las decisiones al {@link KnowledgePolicyEngine}
 * (por su interfaz pública) y el plan al {@link QueryPlanner}. La ejecución
 * física (fetch/validación/ranking/ensamblado) queda delegada al integrador.</p>
 *
 * <p>Estrategias registrables ({@link OrchestrationStrategyPolicy}): Cache First,
 * Provider First, Hybrid, Local First, Internet First, Fail Fast, Graceful
 * Degradation y Offline Mode. Nunca condiciones gigantes.</p>
 *
 * <p>Único objeto mutable: {@link OrchestrationContext}; todos los contratos de
 * salida son inmutables.</p>
 */
public class KnowledgeOrchestrator {

    private final QueryPlanner planner;
    private final KnowledgePolicyEngine policyEngine;
    private final Map<OrchestrationStrategy, OrchestrationStrategyPolicy> strategyPolicies;
    private final ProviderRegistry providerRegistry;
    private final KnowledgeRepository repository;
    private final CandidateValidator validator;
    private final ContextRanker ranker;
    private final ContextAssembler assembler;

    public KnowledgeOrchestrator() {
        this(new QueryPlanner(), new KnowledgePolicyEngine());
    }

    public KnowledgeOrchestrator(QueryPlanner planner, KnowledgePolicyEngine policyEngine) {
        this(planner, policyEngine, null, null, null, null, null);
    }

    /**
     * Constructor de integración física: recibe los colaboradores de ejecución
     * mediante interfaces de dominio (nunca implementaciones ni Spring). Cuando
     * un colaborador es {@code null}, el ciclo coordina decisiones sin ejecutar
     * esa etapa (comportamiento original conservado).
     *
     * @param planner          planificador de consultas
     * @param policyEngine     motor de decisiones
     * @param providerRegistry resuelve {@link ProviderType} a fuentes
     * @param repository       caché de resultados validados (puede ser {@code null})
     * @param validator        validación de candidatos (nunca se omite si está presente)
     * @param ranker           ranking de resultados validados
     * @param assembler        ensamblado del {@link KnowledgeResult}
     */
    public KnowledgeOrchestrator(
            QueryPlanner planner,
            KnowledgePolicyEngine policyEngine,
            ProviderRegistry providerRegistry,
            KnowledgeRepository repository,
            CandidateValidator validator,
            ContextRanker ranker,
            ContextAssembler assembler) {
        this.planner = planner == null ? new QueryPlanner() : planner;
        this.policyEngine = policyEngine == null ? new KnowledgePolicyEngine() : policyEngine;
        this.providerRegistry = providerRegistry;
        this.repository = repository;
        this.validator = validator;
        this.ranker = ranker;
        this.assembler = assembler;
        this.strategyPolicies = registerDefaultStrategies();
    }

    /**
     * Coordina el ciclo completo para una solicitud, devolviendo un resultado
     * inmutable. Nunca lanza excepciones: ante cualquier anomalía degrada o
     * falla según la estrategia.
     */
    public OrchestrationResult coordinate(OrchestrationRequest request) {
        var ctx = new OrchestrationContext(request);
        run(ctx);
        return new OrchestrationResult(
                ctx.currentState(),
                ctx.visited(),
                ctx.plan(),
                ctx.decisions(),
                ctx.selectedTypes(),
                ctx.degraded(),
                ctx.failureReason());
    }

    /**
     * Coordina el ciclo con ejecución física (integración): devuelve la decisión
     * de orquestación, el {@link KnowledgeResult} adquirido (caché o fuentes) y
     * la bandera de reutilización de caché. Sin colaboradores de ejecución, el
     * conocimiento es vacío (solo decisiones).
     */
    public KnowledgeOrchestrationResult coordinateWithResult(OrchestrationRequest request) {
        var ctx = new OrchestrationContext(request);
        run(ctx);
        var orchestration = new OrchestrationResult(
                ctx.currentState(),
                ctx.visited(),
                ctx.plan(),
                ctx.decisions(),
                ctx.selectedTypes(),
                ctx.degraded(),
                ctx.failureReason());
        return new KnowledgeOrchestrationResult(orchestration, ctx.knowledgeResult(), ctx.cacheHit());
    }

    private void run(OrchestrationContext ctx) {
        transition(ctx, OrchestrationState.IDLE);
        if (!handleIdle(ctx)) {
            return;
        }
        transition(ctx, OrchestrationState.PLANNING);
        if (!handlePlanning(ctx)) {
            return;
        }
        transition(ctx, OrchestrationState.CACHE_LOOKUP);
        if (!handleCacheLookup(ctx)) {
            return;
        }
        transition(ctx, OrchestrationState.PROVIDER_SELECTION);
        if (!handleProviderSelection(ctx)) {
            return;
        }
        transition(ctx, OrchestrationState.FETCHING);
        if (!handleFetching(ctx)) {
            return;
        }
        transition(ctx, OrchestrationState.VALIDATION);
        if (!handleValidation(ctx)) {
            return;
        }
        transition(ctx, OrchestrationState.RANKING);
        if (!handleRanking(ctx)) {
            return;
        }
        transition(ctx, OrchestrationState.ASSEMBLING);
        handleAssembling(ctx);
    }

    private boolean handleIdle(OrchestrationContext ctx) {
        KnowledgeRequest request = ctx.request().knowledgeRequest();
        boolean empty = request == null
                || ((request.topic() == null || request.topic().isBlank())
                        && request.keywords().isEmpty());
        if (empty) {
            fail(ctx, "Solicitud inválida: sin tema ni palabras clave");
            return false;
        }
        return true;
    }

    private boolean handlePlanning(OrchestrationContext ctx) {
        var request = ctx.request();
        QueryPlan plan = planner.plan(request.knowledgeRequest());
        QueryMode mode = policyEngine
                .decideQuery(request.knowledgeRequest(), request.policyConfig().query())
                .mode();
        OrchestrationStrategy strategy = determineStrategy(request, plan, mode);
        ctx.setPlan(new OrchestrationPlan(strategy, mode, plan));
        if (mode == QueryMode.CACHE_ONLY) {
            ctx.addDecision(OrchestrationDecision.of(
                    CACHE_ONLY, OrchestrationState.PLANNING, "Solo caché decidido por el Policy Engine"));
            complete(ctx, "Solo caché; sin consulta externa");
            return false;
        }
        if (plan.isEmpty()) {
            ctx.addDecision(OrchestrationDecision.of(
                    NO_CONSULT, OrchestrationState.PLANNING, "Conocimiento estable; no se consulta"));
            complete(ctx, "Conocimiento estable; no se consulta");
            return false;
        }
        return true;
    }

    private boolean handleCacheLookup(OrchestrationContext ctx) {
        var request = ctx.request();
        if (repository != null && request.environment().cacheHealthy()) {
            KnowledgeQuery query = KnowledgeQuery.from(request.knowledgeRequest());
            var cached = repository.find(query);
            if (cached.isPresent()) {
                ctx.setCacheHit(true);
                ctx.setKnowledgeResult(cached.get());
                ctx.addDecision(OrchestrationDecision.of(
                        CACHE_ONLY, OrchestrationState.CACHE_LOOKUP, "Cache HIT; resultado reutilizado"));
                complete(ctx, "Cache hit; sin consulta externa");
                return false;
            }
            ctx.addDecision(OrchestrationDecision.of(
                    CONSULT_EXTERNAL, OrchestrationState.CACHE_LOOKUP, "Cache MISS; se consultan fuentes externas"));
            return true;
        }
        var strategy = ctx.plan().strategy();
        var policy = policyFor(strategy);
        boolean wantsCache = policy.prefersCache();
        if (!wantsCache) {
            ctx.addDecision(OrchestrationDecision.of(
                    CONSULT_EXTERNAL,
                    OrchestrationState.CACHE_LOOKUP,
                    "Se consultarán fuentes externas con estrategia " + strategy.displayName()));
            return true;
        }
        if (request.environment().cacheHealthy()) {
            ctx.addDecision(OrchestrationDecision.of(
                    CACHE_ONLY,
                    OrchestrationState.CACHE_LOOKUP,
                    "Caché disponible; estrategia " + strategy.displayName()));
            complete(ctx, "Cache first; sin consulta externa");
            return false;
        }
        ctx.addDecision(OrchestrationDecision.of(
                DEGRADE, OrchestrationState.CACHE_LOOKUP, "Caché no disponible; se degrada a consulta externa"));
        if (policy.failureIsFatal()) {
            fail(ctx, "Caché no disponible");
            return false;
        }
        ctx.markDegraded();
        ctx.addDecision(OrchestrationDecision.of(
                CONSULT_EXTERNAL,
                OrchestrationState.CACHE_LOOKUP,
                "Se consultan fuentes externas tras degradación de caché"));
        return true;
    }

    private boolean handleProviderSelection(OrchestrationContext ctx) {
        var request = ctx.request();
        var strategy = ctx.plan().strategy();
        var policy = policyFor(strategy);
        var candidateTypes = distinctProviderTypes(ctx.plan().queries());
        if (candidateTypes.isEmpty()) {
            ctx.markDegraded();
            complete(ctx, "Sin tipos de proveedor planificados");
            return false;
        }
        var candidateStrings = candidateTypes.stream()
                .map(ProviderTypeCatalog::sourceType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        ProviderSelection selection = policyEngine.selectProviders(
                candidateStrings, request.policyConfig().provider());
        var allowed = new ArrayList<ProviderType>();
        for (String type : selection.allowedTypes()) {
            ProviderTypeCatalog.fromSourceType(type).ifPresent(allowed::add);
        }
        var unhealthy = new ArrayList<ProviderType>();
        var healthy = new ArrayList<ProviderType>();
        for (ProviderType type : allowed) {
            if (request.environment().isAvailable(type)) {
                healthy.add(type);
            } else {
                unhealthy.add(type);
            }
        }
        if (!unhealthy.isEmpty()) {
            ctx.addDecision(OrchestrationDecision.of(
                    DEGRADE,
                    OrchestrationState.PROVIDER_SELECTION,
                    "Proveedor(es) no disponible(s): " + names(unhealthy)));
            if (policy.failureIsFatal()) {
                fail(ctx, "Proveedor(es) no disponible(s): " + names(unhealthy));
                return false;
            }
            ctx.markDegraded();
        }
        if (healthy.isEmpty()) {
            ctx.addDecision(OrchestrationDecision.of(
                    STOP,
                    OrchestrationState.PROVIDER_SELECTION,
                    "Sin proveedores disponibles para las facetas requeridas"));
            if (policy.failureIsFatal()) {
                fail(ctx, "Sin proveedores disponibles");
            } else {
                ctx.markDegraded();
                complete(ctx, "Sin proveedores disponibles; degradado");
            }
            return false;
        }
        ctx.setProviderSelection(selection);
        ctx.setSelectedTypes(healthy);
        return true;
    }

    private boolean handleFetching(OrchestrationContext ctx) {
        var request = ctx.request();
        var strategy = ctx.plan().strategy();
        var policy = policyFor(strategy);
        int queries = ctx.plan().queries().size();
        int external = (int) ctx.plan().queries().stream()
                .filter(query -> !isLocal(query.providerType()))
                .count();
        var budget = policyEngine.checkBudget(
                new CostBudgetUsage(queries, external), request.policyConfig().cost());
        if (budget.rejected()) {
            ctx.addDecision(OrchestrationDecision.of(
                    STOP_CONSULTS, OrchestrationState.FETCHING, "Presupuesto agotado: " + join(budget.reasons())));
            if (policy.failureIsFatal()) {
                fail(ctx, "Presupuesto agotado");
            } else {
                ctx.markDegraded();
                complete(ctx, "Presupuesto agotado; degradado");
            }
            return false;
        }
        if (external > 0 && (policy.offlineOnly() || !request.environment().internetAvailable())) {
            ctx.addDecision(OrchestrationDecision.of(
                    DEGRADE, OrchestrationState.FETCHING, "Sin acceso a Internet para consultas externas"));
            if (policy.failureIsFatal()) {
                fail(ctx, "Sin acceso a Internet");
            } else {
                ctx.markDegraded();
                complete(ctx, "Sin acceso a Internet; degradado");
            }
            return false;
        }
        if (providerRegistry != null && !ctx.selectedTypes().isEmpty()) {
            KnowledgeQuery query = KnowledgeQuery.from(request.knowledgeRequest());
            var candidates = new ArrayList<KnowledgeCandidate>();
            for (ProviderType type : ctx.selectedTypes()) {
                List<KnowledgeSource> sources = providerRegistry.sourcesFor(type);
                if (sources == null) {
                    continue;
                }
                for (var source : sources) {
                    var fetched = source.fetch(query);
                    if (fetched != null) {
                        candidates.addAll(fetched);
                    }
                }
            }
            ctx.setCandidates(candidates);
        }
        ctx.addDecision(OrchestrationDecision.of(
                FETCH_COORDINATED, OrchestrationState.FETCHING, "Ejecución coordinada; delegada al integrador"));
        return true;
    }

    private boolean handleValidation(OrchestrationContext ctx) {
        if (validator != null && !ctx.candidates().isEmpty()) {
            ctx.setValidations(validator.validateAll(ctx.candidates()));
        }
        ctx.addDecision(OrchestrationDecision.of(
                VALIDATION_OK,
                OrchestrationState.VALIDATION,
                "Validación delegada (SourceValidator + Quality Policies)"));
        return true;
    }

    private boolean handleRanking(OrchestrationContext ctx) {
        if (ranker != null
                && !ctx.candidates().isEmpty()
                && ctx.validations().size() == ctx.candidates().size()) {
            var pairs = new ArrayList<RankedCandidate>();
            var candidates = ctx.candidates();
            var validations = ctx.validations();
            for (int i = 0; i < candidates.size(); i++) {
                pairs.add(new RankedCandidate(candidates.get(i), validations.get(i)));
            }
            ctx.setRanked(ranker.rank(pairs));
        }
        ctx.addDecision(
                OrchestrationDecision.of(RANKING_OK, OrchestrationState.RANKING, "Ranking delegado (ContextRanker)"));
        return true;
    }

    private void handleAssembling(OrchestrationContext ctx) {
        var request = ctx.request();
        var contextDecision = policyEngine.checkContext(
                new ContextBudget(0, 0, 0), request.policyConfig().context());
        if (contextDecision.rejected()) {
            ctx.markDegraded();
            complete(ctx, "Límite de contexto excedido");
            return;
        }
        if (assembler != null) {
            KnowledgeQuery query = KnowledgeQuery.from(request.knowledgeRequest());
            KnowledgeResult result = assembler.assemble(query, ctx.ranked());
            ctx.setKnowledgeResult(result);
            if (repository != null && !result.isEmpty()) {
                repository.save(result, request.knowledgeRequest().timeWindow());
            }
        }
        ctx.addDecision(OrchestrationDecision.of(
                ASSEMBLY_OK, OrchestrationState.ASSEMBLING, "Ensamblado delegado (ContextAssembler)"));
        complete(ctx, "Ciclo coordinado exitosamente");
    }

    private OrchestrationStrategy determineStrategy(OrchestrationRequest request, QueryPlan plan, QueryMode mode) {
        if (mode == QueryMode.CACHE_ONLY || plan.isEmpty()) {
            return request.strategy();
        }
        OrchestrationStrategy requested = request.strategy();
        if (requested != OrchestrationStrategy.GRACEFUL_DEGRADATION) {
            return requested;
        }
        return switch (plan.strategy()) {
            case LOCAL_ONLY -> OrchestrationStrategy.LOCAL_FIRST;
            case INTERNET_ONLY -> OrchestrationStrategy.INTERNET_FIRST;
            case HYBRID -> OrchestrationStrategy.HYBRID;
            default -> OrchestrationStrategy.GRACEFUL_DEGRADATION;
        };
    }

    private void complete(OrchestrationContext ctx, String reason) {
        transition(ctx, OrchestrationState.COMPLETED);
        ctx.addDecision(OrchestrationDecision.of(FINALIZE, OrchestrationState.COMPLETED, reason));
    }

    private void fail(OrchestrationContext ctx, String reason) {
        ctx.setFailureReason(reason);
        transition(ctx, OrchestrationState.FAILED);
        ctx.addDecision(OrchestrationDecision.of(FAIL, OrchestrationState.FAILED, reason));
    }

    private void transition(OrchestrationContext ctx, OrchestrationState target) {
        if (target == null) {
            target = OrchestrationState.FAILED;
        }
        OrchestrationState from = ctx.currentState();
        if (from != null && !OrchestrationState.canTransition(from, target)) {
            ctx.transition(OrchestrationState.FAILED);
            ctx.addDecision(OrchestrationDecision.of(
                    FAIL, OrchestrationState.FAILED, "Transición inválida: " + from + " → " + target));
            return;
        }
        ctx.transition(target);
    }

    private OrchestrationStrategyPolicy policyFor(OrchestrationStrategy strategy) {
        return strategyPolicies.getOrDefault(strategy, new GracefulDegradationPolicy());
    }

    private static LinkedHashSet<ProviderType> distinctProviderTypes(List<PlannedQuery> queries) {
        var types = new LinkedHashSet<ProviderType>();
        if (queries != null) {
            for (PlannedQuery query : queries) {
                if (query.providerType() != null) {
                    types.add(query.providerType());
                }
            }
        }
        return types;
    }

    private static boolean isLocal(ProviderType type) {
        return type == ProviderType.DOCUMENT || type == ProviderType.VECTOR_RAG;
    }

    private static String names(List<ProviderType> types) {
        return types.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private static String join(List<String> reasons) {
        return String.join("; ", reasons);
    }

    private static Map<OrchestrationStrategy, OrchestrationStrategyPolicy> registerDefaultStrategies() {
        var map = new EnumMap<OrchestrationStrategy, OrchestrationStrategyPolicy>(OrchestrationStrategy.class);
        for (OrchestrationStrategyPolicy policy : List.of(
                new CacheFirstPolicy(),
                new ProviderFirstPolicy(),
                new HybridPolicy(),
                new LocalFirstPolicy(),
                new InternetFirstPolicy(),
                new FailFastPolicy(),
                new GracefulDegradationPolicy(),
                new OfflineModePolicy())) {
            map.put(policy.strategy(), policy);
        }
        return Map.copyOf(map);
    }
}
