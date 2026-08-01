package com.kinplatform.kin.ai;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;

/**
 * Entrada unificada para {@link PromptAssembler}.
 *
 * <p>Según {@link PromptType}:
 * <ul>
 *   <li>{@code CONVERSATION}: requiere {@code context} y {@code decision}; {@code consultingReport} debe ser {@code null} y {@code directive} es opcional (ADR-013)</li>
 *   <li>{@code REPORT}: requiere {@code consultingReport}; {@code context}, {@code decision} y {@code directive} deben ser {@code null}</li>
 * </ul>
 */
public record PromptRequest(
    ConsultingReport consultingReport,
    PromptType type,
    ProjectContext context,
    ConversationDecision decision,
    TurnDirective directive
) {

    public PromptRequest(ConsultingReport consultingReport, PromptType type,
                         ProjectContext context, ConversationDecision decision) {
        this(consultingReport, type, context, decision, null);
    }

    public PromptRequest {
        if (type == null) {
            throw new IllegalArgumentException("type no puede ser null");
        }
        if (type == PromptType.REPORT && consultingReport == null) {
            throw new IllegalArgumentException("consultingReport es obligatorio para REPORT");
        }
        if (type == PromptType.REPORT && (context != null || decision != null)) {
            throw new IllegalArgumentException("context y decision deben ser null para REPORT");
        }
        if (type == PromptType.REPORT && directive != null) {
            throw new IllegalArgumentException("directive debe ser null para REPORT");
        }
        if (type == PromptType.CONVERSATION) {
            if (consultingReport != null) {
                throw new IllegalArgumentException("consultingReport debe ser null para CONVERSATION");
            }
            if (context == null) {
                throw new IllegalArgumentException("context es obligatorio para CONVERSATION");
            }
            if (decision == null) {
                throw new IllegalArgumentException("decision es obligatorio para CONVERSATION");
            }
        }
    }

    public static PromptRequest forConversation(ProjectContext context, ConversationDecision decision) {
        return new PromptRequest(null, PromptType.CONVERSATION, context, decision, null);
    }

    public static PromptRequest forConversation(ProjectContext context, ConversationDecision decision,
                                                TurnDirective directive) {
        return new PromptRequest(null, PromptType.CONVERSATION, context, decision, directive);
    }

    public static PromptRequest forReport(ConsultingReport consultingReport) {
        return new PromptRequest(consultingReport, PromptType.REPORT, null, null, null);
    }
}
