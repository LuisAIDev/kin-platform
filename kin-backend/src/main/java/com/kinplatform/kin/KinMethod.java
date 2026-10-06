package com.kinplatform.kin;

import com.kinplatform.common.context.ContextRepository;
import com.kinplatform.common.context.ProjectContextSyncPort;
import com.kinplatform.kin.conversation.ResponseFallback;
import com.kinplatform.kin.conversation.ResponseValidation;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.common.decision.ConversationDecision;
import com.kinplatform.kin.enterprise.application.EnterprisePipelineResultStore;
import com.kinplatform.kin.enterprise.application.EnterpriseTurnResults;
import com.kinplatform.common.pipeline.Pipeline;
import com.kinplatform.common.pipeline.PipelineContext;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Punto de entrada único del runtime de KIN (consolidación de la Fase 5.2.1).
 *
 * <p>Todo el procesamiento —bloqueante ({@link #execute}) y streaming
 * ({@link #executeStream})— pasa por el mismo {@link Pipeline} y por el mismo
 * {@link ContextRepository}: no existen flujos paralelos ni lógica de negocio
 * fuera del pipeline.</p>
 *
 * <p>El contexto del proyecto se carga desde el repositorio (durable) y se
 * re-persiste tras la ejecución, con lo que todas las etapas (Analizador,
 * Evaluador, Estratega, Consultor, Scoring, Recomendaciones, Riesgos,
 * Eventos) reciben siempre un {@code projectContext} no nulo.</p>
 *
 * <p><strong>Responsabilidad de eventos:</strong> el pipeline produce los
 * {@link com.kinplatform.kin.event.DomainEvent} y KinMethod los entrega como
 * parte del resultado ({@link KinMethodResult#events()} /
 * {@link StreamingMethodOutcome}). KinMethod NO persiste eventos en el outbox:
 * esa publicación transaccional la realiza la capa de I/O
 * ({@code ChatTurnFinalizationService}) DENTRO de una transacción Spring corta
 * de finalización del turno, para respetar el contrato del outbox transaccional
 * (ADR-026).</p>
 */
public class KinMethod {

    private static final Logger log = LoggerFactory.getLogger(KinMethod.class);

    private final Pipeline pipeline;
    private final ContextRepository contextRepository;
    private final ResponseFallback responseFallback;
    private final ProjectContextSyncPort contextSync;
    private final EnterprisePipelineResultStore pipelineResultStore;

    private static final ProjectContextSyncPort NO_OP_SYNC = (projectId, context) -> {};
    private static final EnterprisePipelineResultStore NO_OP_RESULT_STORE = new EnterprisePipelineResultStore() {
        @Override
        public void store(EnterpriseTurnResults results) {}

        @Override
        public java.util.Optional<EnterpriseTurnResults> consume(UUID projectId) {
            return java.util.Optional.empty();
        }
    };

    public KinMethod(Pipeline pipeline, ContextRepository contextRepository) {
        this(
                pipeline,
                contextRepository,
                new ResponseFallback(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), 0),
                NO_OP_SYNC,
                NO_OP_RESULT_STORE);
    }

    /**
     * Constructor aditivo (ADR-017, Etapa E5): inyecta el
     * {@link ResponseFallback} que garantiza la respuesta segura final en el
     * flujo streaming.
     */
    public KinMethod(Pipeline pipeline, ContextRepository contextRepository, ResponseFallback responseFallback) {
        this(pipeline, contextRepository, responseFallback, NO_OP_SYNC, NO_OP_RESULT_STORE);
    }

    /**
     * Constructor aditivo: inyecta el {@link ProjectContextSyncPort} que
     * mantiene sincronizado el agregado {@code Project} con el
     * {@code ProjectContext} (única fuente coherente para el Dashboard).
     */
    public KinMethod(Pipeline pipeline, ContextRepository contextRepository, ProjectContextSyncPort contextSync) {
        this(
                pipeline,
                contextRepository,
                new ResponseFallback(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), 0),
                contextSync,
                NO_OP_RESULT_STORE);
    }

    public KinMethod(
            Pipeline pipeline,
            ContextRepository contextRepository,
            ResponseFallback responseFallback,
            ProjectContextSyncPort contextSync) {
        this(pipeline, contextRepository, responseFallback, contextSync, NO_OP_RESULT_STORE);
    }

    /**
     * Constructor aditivo (Fase 10, Milestone 3C): inyecta el
     * {@link EnterprisePipelineResultStore} que recibe los resultados reales
     * del pipeline cuando un turno completa {@code REPORT}.
     */
    public KinMethod(
            Pipeline pipeline,
            ContextRepository contextRepository,
            ProjectContextSyncPort contextSync,
            EnterprisePipelineResultStore pipelineResultStore) {
        this(
                pipeline,
                contextRepository,
                new ResponseFallback(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), 0),
                contextSync,
                pipelineResultStore);
    }

    /**
     * Constructor completo del runtime. No recibe bus de eventos ni publicador
     * outbox: KinMethod solo produce y devuelve los eventos; la capa de I/O los
     * persiste transaccionalmente.
     */
    public KinMethod(
            Pipeline pipeline,
            ContextRepository contextRepository,
            ResponseFallback responseFallback,
            ProjectContextSyncPort contextSync,
            EnterprisePipelineResultStore pipelineResultStore) {
        this.pipeline = pipeline;
        this.contextRepository = contextRepository;
        this.responseFallback = responseFallback == null
                ? new ResponseFallback(List.of(ResponseFallback.DEFAULT_CANNED_RESPONSE), 0)
                : responseFallback;
        this.contextSync = contextSync == null ? NO_OP_SYNC : contextSync;
        this.pipelineResultStore = pipelineResultStore == null ? NO_OP_RESULT_STORE : pipelineResultStore;
    }

    public KinMethodResult execute(KinMethodCommand command) {
        log.info("KinMethod executing for project={}, userId={}", command.projectId(), command.userId());

        var ctx = prepare(command);
        var result = pipeline.execute(ctx);
        contextRepository.save(command.projectId(), result.projectContext());
        contextSync.sync(command.projectId(), result.projectContext());
        capturePipelineResults(command.projectId(), result);

        return new KinMethodResult(
                result.projectContext(),
                result.evaluation(),
                result.decision(),
                result.aiResponse(),
                result.scoreResult(),
                result.events(),
                result.consultingReport());
    }

    /**
     * Variante streaming: ejecuta el pipeline completo de forma síncrona
     * (todas las etapas deterministas), pero la etapa Consultor deja el
     * {@code Flux} de tokens en el contexto en lugar de bloquear. Devuelve ese
     * flux para que el orquestador SSE lo suscriba.
     */
    public Flux<String> executeStream(KinMethodCommand command) {
        StreamingMethodOutcome outcome = executeStreamWithOutcome(command);
        return outcome == null ? null : outcome.safeFlux();
    }

    /**
     * Variante streaming que, además del {@link Flux} de tokens, entrega el
     * {@link KinMethodResult} completo del turno (aditivo).
     *
     * <p>Igual que {@link #executeStream} pero devuelve el resultado del
     * pipeline para que la capa de I/O persista el {@code ConsultingReport}
     * cuando el turno completa {@code REPORT}, sin re-ejecutar ningún motor.
     * El safety-net de respuesta segura se aplica exactamente igual que en
     * {@link #executeStream}.</p>
     */
    public StreamingMethodOutcome executeStreamWithOutcome(KinMethodCommand command) {
        log.info("KinMethod streaming for project={}, userId={}", command.projectId(), command.userId());

        var ctx = prepare(command);
        ctx.streaming(true);
        var result = pipeline.execute(ctx);
        contextRepository.save(command.projectId(), result.projectContext());
        contextSync.sync(command.projectId(), result.projectContext());

        Flux<String> flux = result.aiResponseFlux();
        if (flux == null) {
            return null;
        }
        // Safety net (ADR-017, E5): si la validación final de la respuesta
        // streamed fue rechazada con un rechazo duro, anexa la respuesta segura
        // determinista. Un rechazo blando (response.too_long) NO anexa nada: los
        // tokens ya entregados son contenido útil y se conservan íntegros.
        Flux<String> safeFlux = flux.concatWith(Flux.defer(() -> {
            ResponseValidation validation = result.responseValidation();
            if (validation != null && ResponseGuard.requiresFallback(validation)) {
                return Flux.just(responseFallback.cannedResponse(validation));
            }
            return Flux.empty();
        }));
        return new StreamingMethodOutcome(
                safeFlux,
                new KinMethodResult(
                        result.projectContext(),
                        result.evaluation(),
                        result.decision(),
                        result.aiResponse(),
                        result.scoreResult(),
                        result.events(),
                        result.consultingReport()));
    }

    private PipelineContext prepare(KinMethodCommand command) {
        var ctx = new PipelineContext(
                command.projectId(),
                command.userId(),
                command.userMessage(),
                command.history(),
                command.projectTitle(),
                command.projectDescription(),
                command.projectCategory());
        var projectContext = contextRepository.findOrCreate(
                command.projectId(), command.projectTitle(), command.projectDescription(), command.projectCategory());
        ctx.projectContext(projectContext);
        ctx.turnDirective(command.directive());
        return ctx;
    }

    /**
     * Entrega los resultados reales del pipeline a la generación Enterprise
     * (Fase 10, Milestone 3C): cuando un turno completa {@code REPORT} con
     * informe presente, publica los cuatro resultados deterministas
     * (recomendaciones, oportunidades, conocimiento y riesgo) en la
     * {@link EnterprisePipelineResultStore}. Los resultados se reutilizan
     * exactamente como los produjo el pipeline: no se recalcular ni se vuelven
     * a ejecutar motores. Sin store o fuera de REPORT no hay efecto.
     */
    private void capturePipelineResults(UUID projectId, PipelineContext result) {
        if (result.decision() == null
                || result.decision().action() != ConversationDecision.Action.REPORT
                || result.consultingReport() == null) {
            return;
        }
        pipelineResultStore.store(new EnterpriseTurnResults(
                projectId,
                result.recommendationResult(),
                result.opportunityResult(),
                result.knowledgeResult(),
                result.riskResult()));
    }
}


