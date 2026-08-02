package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.ai.PromptRequest;
import com.kinplatform.kin.conversation.CommunicationMode;
import com.kinplatform.kin.conversation.ConversationPhase;
import com.kinplatform.kin.conversation.TurnConstraints;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.interview.InterviewResult;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;
import reactor.core.publisher.Flux;

/**
 * Etapa de consultoría: pide la respuesta de IA al puerto {@link AIResponder}.
 *
 * <p>Depende del puerto de dominio (no del {@code AiEngineService} concreto),
 * por lo que respeta la dirección de dependencias de la arquitectura limpia.
 * La construcción del prompt la delega al {@link PromptAssembler}.</p>
 *
 * <p>En modo streaming no bloquea: deja el {@code Flux} en el contexto para que
 * {@code KinMethod.executeStream} lo devuelva al orquestador SSE. Además, cuando
 * hay directiva de turno (ADR-013), enmarca el prompt CONVERSATION con la
 * directiva y valida la respuesta streamed con el {@link ResponseGuard},
 * dejando el {@code ResponseValidation} en el contexto (el orquestador es
 * autoritativo en el flujo bloqueante).</p>
 *
 * <p>Cambio aditivo sancionado por ADR-015 (Etapa E6): lee
 * {@code PipelineContext.interviewResult} para enmarcar el prompt. Si la
 * entrevista está incompleta (pregunta pendiente), el LLM formula la pregunta de
 * la entrevista ({@code ## ENTREVISTA ESTRAT\u00C9GICA}) en lugar de un reporte,
 * incluso si la decisión del turno fuese REPORT (gating de {@code InterviewStage}).
 * Si la entrevista está completa, el flujo REPORT habitual queda intacto.</p>
 */
public class ConsultorStage implements PipelineStage {

    private final AIResponder aiResponder;
    private final PromptAssembler promptAssembler;
    private final ResponseGuard responseGuard;

    public ConsultorStage(AIResponder aiResponder, PromptAssembler promptAssembler) {
        this(aiResponder, promptAssembler, new ResponseGuard());
    }

    public ConsultorStage(AIResponder aiResponder, PromptAssembler promptAssembler,
                          ResponseGuard responseGuard) {
        if (aiResponder == null) {
            throw new IllegalArgumentException("aiResponder no puede ser null");
        }
        if (promptAssembler == null) {
            throw new IllegalArgumentException("promptAssembler no puede ser null");
        }
        if (responseGuard == null) {
            throw new IllegalArgumentException("responseGuard no puede ser null");
        }
        this.aiResponder = aiResponder;
        this.promptAssembler = promptAssembler;
        this.responseGuard = responseGuard;
    }

    @Override
    public String name() {
        return "Consultor";
    }

    @Override
    public boolean supports(PipelineContext context) {
        return true;
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        PromptRequest promptRequest;
        ConversationDecision decision = context.decision();
        InterviewResult interview = context.interviewResult();

        if (interview != null && !interview.isEmpty()
                && interview.decision() != null && interview.decision().isAsk()) {
            promptRequest = PromptRequest.forConversation(
                    context.projectContext(), decision, context.turnDirective());
        } else if (decision != null && decision.shouldGenerateReport()) {
            ConsultingReport report = context.consultingReport();
            if (report == null) {
                throw new IllegalStateException("consultingReport es obligatorio para responder en modo REPORT");
            }
            promptRequest = PromptRequest.forReport(report);
        } else {
            promptRequest = PromptRequest.forConversation(
                    context.projectContext(), decision, context.turnDirective());
        }

        var systemPrompt = promptAssembler.assemble(promptRequest, context.interviewResult());
        var request = new AIRequest(context.history(), context.userMessage(), systemPrompt);
        if (context.streaming()) {
            context.aiResponseFlux(attachStreamGuard(context, aiResponder.respondStream(request)));
        } else {
            context.aiResponse(aiResponder.respond(request));
        }
        return context;
    }

    private Flux<String> attachStreamGuard(PipelineContext context, Flux<String> flux) {
        TurnDirective directive = resolveDirective(context, context.decision());
        if (directive == null || flux == null) {
            return flux;
        }
        StringBuilder accumulated = new StringBuilder();
        return flux
                .doOnNext(accumulated::append)
                .doOnComplete(() -> context.responseValidation(
                        responseGuard.validate(accumulated.toString(), directive)));
    }

    private TurnDirective resolveDirective(PipelineContext context, ConversationDecision decision) {
        TurnDirective directive = context.turnDirective();
        if (directive != null) {
            return directive;
        }
        if (decision != null && decision.shouldGenerateReport()) {
            return new TurnDirective(
                    ConversationPhase.REPORTING,
                    decision.action(),
                    decision.dimension(),
                    CommunicationMode.EXPLAIN_REPORT,
                    TurnConstraints.reportExplanation());
        }
        return null;
    }
}
