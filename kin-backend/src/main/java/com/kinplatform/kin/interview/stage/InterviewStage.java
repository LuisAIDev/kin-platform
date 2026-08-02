package com.kinplatform.kin.interview.stage;

import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.interview.InterviewAnswer;
import com.kinplatform.kin.interview.InterviewContext;
import com.kinplatform.kin.interview.InterviewDirective;
import com.kinplatform.kin.interview.InterviewInput;
import com.kinplatform.kin.interview.InterviewRepository;
import com.kinplatform.kin.interview.InterviewRequest;
import com.kinplatform.kin.interview.InterviewResult;
import com.kinplatform.kin.interview.InterviewState;
import com.kinplatform.kin.interview.engine.InterviewEngine;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.pipeline.stage.EngineStage;

import java.util.Optional;
import java.util.UUID;

/**
 * Etapa de entrevista del pipeline (ADR-015, Etapa E6): ejecuta el
 * {@link InterviewEngine} y cierra el flujo completo de la entrevista.
 *
 * <p>Composición pura sobre {@link EngineStage} (mismo patrón que
 * {@code KnowledgeStage}/{@code ScoringStage}). En cada turno recupera el
 * {@link InterviewState} previo del proyecto vía {@link InterviewRepository}
 * (durable entre turnos), construye el {@link InterviewAnswer} a partir de la
 * pregunta pendiente y del mensaje del turno, invoca el motor y persiste el
 * estado resultante al final del turno. El resultado queda registrado en
 * {@code PipelineContext.interviewResult} (campo aditivo sancionado por
 * ADR-015) y en {@code PipelineContext.engineResults()} vía {@link EngineStage}.</p>
 *
 * <p>La etapa es además la autoridad del gating de REPORT (ADR-015 §Integración,
 * punto 2; FASE7 §5.3): cuando la entrevista está incompleta la decisión efectiva
 * del turno es {@code ASK} (las etapas de análisis se omiten por predicado y el
 * {@code ConsultingReport} queda bloqueado); cuando está completa la decisión es
 * {@code REPORT} y las etapas de análisis se ejecutan en el mismo turno. La
 * decisión efectiva se persiste en el {@code ProjectContext} para que el
 * siguiente turno enmarque la comunicación según el estado de la entrevista.</p>
 *
 * <p>Sin entrevista activa (resultado vacío) la etapa no modifica la decisión del
 * estratega: modo seguro compatible con el flujo previo.</p>
 */
public class InterviewStage implements PipelineStage {

    private static final int ASK_PRIORITY = 9;
    private static final String REPORT_REASON = "Entrevista estratégica completa.";

    private final EngineStage<InterviewInput, InterviewResult> delegate;
    private final InterviewRepository interviewRepository;

    /**
     * Constructor compatible (modo sin persistencia): el estado se inicia vacío
     * por turno y nunca se persiste. Mantiene el contrato previo a E6.
     */
    public InterviewStage(InterviewEngine interviewEngine) {
        this(interviewEngine, new NoOpInterviewRepository());
    }

    /**
     * @param interviewEngine      motor canonizado de la entrevista
     * @param interviewRepository  puerto de persistencia del estado (obligatorio)
     */
    public InterviewStage(InterviewEngine interviewEngine, InterviewRepository interviewRepository) {
        if (interviewEngine == null) {
            throw new IllegalArgumentException("interviewEngine no puede ser null");
        }
        if (interviewRepository == null) {
            throw new IllegalArgumentException("interviewRepository no puede ser null");
        }
        this.interviewRepository = interviewRepository;
        this.delegate = new EngineStage<>(
            "Entrevista",
            interviewEngine,
            context -> context != null && context.projectContext() != null,
            context -> new InterviewInput(buildRequest(context), context.userMessage()),
            PipelineContext::interviewResult
        );
    }

    private InterviewRequest buildRequest(PipelineContext context) {
        var project = context.projectContext();
        InterviewState previousState = interviewRepository.findOrCreate(context.projectId());
        var interviewContext = InterviewContext.of(
            context.projectId(),
            context.projectTitle(),
            context.projectCategory(),
            project.coveredDimensions());
        InterviewAnswer answer = null;
        if (previousState != null && previousState.current() != null) {
            answer = InterviewAnswer.of(previousState.current(), context.userMessage());
        }
        return InterviewRequest.of(interviewContext, answer, previousState);
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public boolean supports(PipelineContext context) {
        return delegate.supports(context);
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        var result = delegate.execute(context);
        InterviewResult interview = result.interviewResult();
        if (interview != null && !interview.isEmpty() && interview.decision() != null) {
            ConversationDecision effective = effectiveDecision(interview);
            if (effective != null) {
                result.decision(effective);
                if (result.projectContext() != null) {
                    result.projectContext().attachDecision(effective);
                }
            }
            if (interview.decision().isReport() && result.projectContext() != null) {
                result.projectContext().markReportGenerated();
            }
            if (interview.state() != null) {
                interviewRepository.save(interview.state());
            }
        }
        return result;
    }

    private ConversationDecision effectiveDecision(InterviewResult interview) {
        if (interview.decision().isReport()) {
            return ConversationDecision.generateReport(REPORT_REASON);
        }
        InterviewDirective directive = interview.directive();
        if (directive != null) {
            return ConversationDecision.ask(directive.dimension(), ASK_PRIORITY,
                "Entrevista estratégica: " + directive.topic());
        }
        return null;
    }

    private static final class NoOpInterviewRepository implements InterviewRepository {

        @Override
        public Optional<InterviewState> find(UUID projectId) {
            return Optional.empty();
        }

        @Override
        public void save(InterviewState state) {
            // sin persistencia (modo compatible previo a E6)
        }
    }
}
