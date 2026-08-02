package com.kinplatform.kin.ai;

import com.kinplatform.kin.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.kin.ai.prompt.ReportPromptBuilder;
import com.kinplatform.kin.interview.InterviewResult;

/**
 * Fachada única para ensamblado de prompts.
 *
 * <p>Delega a {@link ConversationPromptBuilder} o {@link ReportPromptBuilder}
 * según {@link PromptType}. No contiene lógica de negocio ni formateo de secciones.
 *
 * <p>Cambio aditivo sancionado por ADR-015 (Etapa E6): {@link #assemble(PromptRequest, InterviewResult)}
 * propaga el {@link InterviewResult} al {@link ConversationPromptBuilder} para la
 * sección {@code ## ENTREVISTA ESTRAT\u00C9GICA}; el modo REPORT queda intacto
 * (frontera ADR-012).
 */
public class PromptAssembler {

    private final ConversationPromptBuilder conversationBuilder;
    private final ReportPromptBuilder reportBuilder;

    public PromptAssembler(ConversationPromptBuilder conversationBuilder,
                           ReportPromptBuilder reportBuilder) {
        this.conversationBuilder = conversationBuilder;
        this.reportBuilder = reportBuilder;
    }

    public String assemble(PromptRequest request) {
        return switch (request.type()) {
            case CONVERSATION -> conversationBuilder.build(request);
            case REPORT -> reportBuilder.build(request);
        };
    }

    /**
     * Ensambla el prompt propagando el resultado de la entrevista estratégica
     * (ADR-015, Etapa E6). En modo REPORT el resultado de entrevista se ignora.
     */
    public String assemble(PromptRequest request, InterviewResult interviewResult) {
        return switch (request.type()) {
            case CONVERSATION -> conversationBuilder.build(request, interviewResult);
            case REPORT -> reportBuilder.build(request);
        };
    }
}
