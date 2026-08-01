package com.kinplatform.kin.ai;

import com.kinplatform.kin.ai.prompt.ConversationPromptBuilder;
import com.kinplatform.kin.ai.prompt.ReportPromptBuilder;

/**
 * Fachada única para ensamblado de prompts.
 *
 * <p>Delega a {@link ConversationPromptBuilder} o {@link ReportPromptBuilder}
 * según {@link PromptType}. No contiene lógica de negocio ni formateo de secciones.
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
}
