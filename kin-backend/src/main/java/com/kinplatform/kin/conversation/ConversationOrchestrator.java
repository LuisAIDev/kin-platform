package com.kinplatform.kin.conversation;

import com.kinplatform.kin.KinMethod;
import com.kinplatform.kin.KinMethodCommand;
import com.kinplatform.kin.KinMethodResult;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.Message;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.TurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.enterprise.application.EnterpriseProjectTrigger;
import com.kinplatform.kin.event.DomainEvent;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fachada de dominio del ciclo de conversación (ADR-013, Etapas 5 y 6).
 *
 * <p>Coordina el ciclo completo de un turno componiendo los componentes del
 * dominio: {@link HistoryWindow} acota el historial que verá el LLM (presupuesto
 * por número de mensajes), {@link ContextRepository} carga/crea el contexto
 * durable del proyecto, {@link TurnPolicy} decide en Java la directiva de
 * comunicación ANTES de ejecutar el pipeline (para que la directiva viaje en el
 * {@link KinMethodCommand} y enmarque el prompt en la etapa Consultor),
 * {@link KinMethod} delega la ejecución del pipeline (contrato congelado) y
 * {@link ResponseGuard} valida la respuesta del LLM contra la directiva. El
 * resultado es un turno tipado {@link TurnResult}.</p>
 *
 * <p>Es una fachada pura de dominio, sin Spring y sin infraestructura: no
 * contiene lógica de negocio, no analiza proyectos, no calcula scoring, no
 * genera prompts ni interpreta respuestas del LLM. La integración aditiva con
 * el pipeline (directiva en {@code KinMethodCommand}, {@code PipelineContext},
 * {@code PromptRequest}) y el modo streaming pertenecen a la Etapa 6
 * (ADR-013 §Cambios aditivos).</p>
 */
public class ConversationOrchestrator {

    private final HistoryWindow historyWindow;
    private final TurnPolicy turnPolicy;
    private final KinMethod kinMethod;
    private final ResponseGuard responseGuard;
    private final ContextRepository contextRepository;
    private final ResponseFallback responseFallback;
    private final EnterpriseProjectTrigger enterpriseTrigger;

    /** Trigger por defecto: sin integración Enterprise, no emite nada. */
    private static final EnterpriseProjectTrigger NO_OP_TRIGGER = projectId -> { };

    public ConversationOrchestrator(HistoryWindow historyWindow,
                                    TurnPolicy turnPolicy,
                                    KinMethod kinMethod,
                                    ResponseGuard responseGuard,
                                    ContextRepository contextRepository) {
        this(historyWindow, turnPolicy, kinMethod, responseGuard, contextRepository,
            new ResponseFallback(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), 0), NO_OP_TRIGGER);
    }

    /**
     * Constructor aditivo (ADR-017, Etapa E5): permite inyectar el
     * {@link ResponseFallback} que consume la {@link ResponseValidation} del
     * turno (reintento acotado o respuesta segura).
     */
    public ConversationOrchestrator(HistoryWindow historyWindow,
                                    TurnPolicy turnPolicy,
                                    KinMethod kinMethod,
                                    ResponseGuard responseGuard,
                                    ContextRepository contextRepository,
                                    ResponseFallback responseFallback) {
        this(historyWindow, turnPolicy, kinMethod, responseGuard, contextRepository,
            responseFallback, NO_OP_TRIGGER);
    }

    /**
     * Constructor aditivo (Fase 10, Milestone 2F): inyecta el
     * {@link EnterpriseProjectTrigger} que emite {@code EnterpriseProjectRequested}
     * cuando el pipeline completa {@code REPORT}. Sin trigger, el orquestador
     * conserva el comportamiento previo intacto.
     */
    public ConversationOrchestrator(HistoryWindow historyWindow,
                                    TurnPolicy turnPolicy,
                                    KinMethod kinMethod,
                                    ResponseGuard responseGuard,
                                    ContextRepository contextRepository,
                                    EnterpriseProjectTrigger enterpriseTrigger) {
        this(historyWindow, turnPolicy, kinMethod, responseGuard, contextRepository,
            new ResponseFallback(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), 0), enterpriseTrigger);
    }

    /**
     * Constructor aditivo completo (Fase 10, Milestone 2F): permite inyectar
     * tanto el {@link ResponseFallback} como el {@link EnterpriseProjectTrigger}.
     */
    public ConversationOrchestrator(HistoryWindow historyWindow,
                                    TurnPolicy turnPolicy,
                                    KinMethod kinMethod,
                                    ResponseGuard responseGuard,
                                    ContextRepository contextRepository,
                                    ResponseFallback responseFallback,
                                    EnterpriseProjectTrigger enterpriseTrigger) {
        if (historyWindow == null) {
            throw new IllegalArgumentException("historyWindow no puede ser null");
        }
        if (turnPolicy == null) {
            throw new IllegalArgumentException("turnPolicy no puede ser null");
        }
        if (kinMethod == null) {
            throw new IllegalArgumentException("kinMethod no puede ser null");
        }
        if (responseGuard == null) {
            throw new IllegalArgumentException("responseGuard no puede ser null");
        }
        if (contextRepository == null) {
            throw new IllegalArgumentException("contextRepository no puede ser null");
        }
        if (responseFallback == null) {
            throw new IllegalArgumentException("responseFallback no puede ser null");
        }
        this.historyWindow = historyWindow;
        this.turnPolicy = turnPolicy;
        this.kinMethod = kinMethod;
        this.responseGuard = responseGuard;
        this.contextRepository = contextRepository;
        this.responseFallback = responseFallback;
        this.enterpriseTrigger = enterpriseTrigger == null ? NO_OP_TRIGGER : enterpriseTrigger;
    }

    /**
     * Ejecuta un turno de conversación de forma bloqueante.
     *
     * <p>Flujo: acota el historial con {@link HistoryWindow}, carga/crea el
     * contexto durable con {@link ContextRepository}, resuelve la directiva con
     * {@link TurnPolicy#decide} a partir del contexto persistido y de la
     * decisión previa, construye el {@link KinMethodCommand} (con la directiva)
     * a partir del {@link ConversationTurn}, delega la ejecución en
     * {@link KinMethod#execute}, valida la respuesta del LLM con
     * {@link ResponseGuard} y devuelve el {@link TurnResult} tipado.</p>
     *
     * @param turn input tipado del turno (obligatorio)
     * @return turno tipado con directiva, respuesta, validación, reporte y eventos
     * @throws IllegalArgumentException si {@code turn} es {@code null} o si el
     *                                  pipeline no produce contexto/decisión
     * @throws IllegalStateException    si {@link KinMethod#execute} devuelve
     *                                  {@code null}
     */
    public TurnResult orchestrate(ConversationTurn turn) {
        if (turn == null) {
            throw new IllegalArgumentException("turn no puede ser null");
        }

        List<Message> windowedHistory = historyWindow.window(
                turn.history(), HistoryWindow.DEFAULT_MAX_MESSAGES);

        ProjectContext projectContext = contextRepository.findOrCreate(
                turn.projectId(),
                turn.projectTitle(),
                turn.projectDescription(),
                turn.projectCategory());

        TurnDirective directive = turnPolicy.decide(
                projectContext, previousDecision(projectContext));

        KinMethodCommand command = new KinMethodCommand(
                turn.projectId(),
                turn.userId(),
                turn.userMessage(),
                windowedHistory,
                turn.projectTitle(),
                turn.projectDescription(),
                turn.projectCategory(),
                directive);

        KinMethodResult result = kinMethod.execute(command);
        if (result == null) {
            throw new IllegalStateException("KinMethod.execute devolvió null");
        }

        ResponseValidation validation = responseGuard.validate(
                result.aiResponse(), directive);
        List<DomainEvent> events = new ArrayList<>(result.events());

        if (!validation.accepted()) {
            int attempt = 0;
            while (!validation.accepted() && responseFallback.shouldRetry(validation, ++attempt)) {
                result = kinMethod.execute(command);
                events.addAll(result.events());
                validation = responseGuard.validate(result.aiResponse(), directive);
            }
        }

        String response = validation.accepted()
                ? result.aiResponse()
                : responseFallback.cannedResponse(validation);

        triggerEnterpriseGeneration(result, turn.projectId());

        return new TurnResult(
                result.projectContext(),
                result.decision(),
                directive,
                response,
                validation,
                result.consultingReport(),
                events);
    }

    /**
     * Ejecuta un turno de conversación en modo streaming (SSE).
     *
     * <p>Igual que {@link #orchestrate} pero delega en
     * {@link KinMethod#executeStream}: la etapa Consultor deja el {@code Flux}
     * de tokens en el contexto y el orquestador lo devuelve para que el
     * consumidor SSE lo suscriba. La directiva viaja igualmente en el
     * {@link KinMethodCommand} y enmarca el prompt; la validación de la
     * respuesta streamed la aplica la etapa Consultor dejando el
     * {@code ResponseValidation} en el {@code PipelineContext} (ADR-013 §7.2).</p>
     *
     * @param turn input tipado del turno (obligatorio)
     * @return flujo reactivo de tokens de la respuesta del LLM
     * @throws IllegalArgumentException si {@code turn} es {@code null}
     * @throws IllegalStateException    si {@link KinMethod#executeStream}
     *                                  devuelve {@code null}
     */
    public Flux<String> orchestrateStream(ConversationTurn turn) {
        if (turn == null) {
            throw new IllegalArgumentException("turn no puede ser null");
        }

        List<Message> windowedHistory = historyWindow.window(
                turn.history(), HistoryWindow.DEFAULT_MAX_MESSAGES);

        ProjectContext projectContext = contextRepository.findOrCreate(
                turn.projectId(),
                turn.projectTitle(),
                turn.projectDescription(),
                turn.projectCategory());

        TurnDirective directive = turnPolicy.decide(
                projectContext, previousDecision(projectContext));

        KinMethodCommand command = new KinMethodCommand(
                turn.projectId(),
                turn.userId(),
                turn.userMessage(),
                windowedHistory,
                turn.projectTitle(),
                turn.projectDescription(),
                turn.projectCategory(),
                directive);

        Flux<String> flux = kinMethod.executeStream(command);
        if (flux == null) {
            throw new IllegalStateException("KinMethod.executeStream devolvió null");
        }

        return flux;
    }

    private ConversationDecision previousDecision(ProjectContext projectContext) {
        return projectContext != null ? projectContext.currentDecision() : null;
    }

    /**
     * Punto mínimo de emisión de la integración Enterprise (Fase 10, M2F):
     * tras completar el turno, si el pipeline generó un {@code REPORT} real
     * (decisión {@code REPORT} con informe de consultoría presente), solicita
     * la generación del proyecto empresarial.
     *
     * <p>La conversación finaliza exactamente igual que hoy (la respuesta ya
     * está calculada); la generación Enterprise comienza después, de forma
     * asíncrona en el listener, nunca antes ni durante. En modo streaming el
     * turno no está completado al devolver el flux, por lo que la emisión solo
     * ocurre en el flujo bloqueante.</p>
     */
    private void triggerEnterpriseGeneration(KinMethodResult result, UUID projectId) {
        if (result.decision() != null
                && result.decision().action() == ConversationDecision.Action.REPORT
                && result.consultingReport() != null) {
            enterpriseTrigger.request(projectId);
        }
    }
}
