package com.kinplatform.kin.ai;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.decision.ConversationDecision;
import com.kinplatform.kin.reporting.report.model.ConsultingReport;

/**
 * Entrada unificada para {@link PromptAssembler}.
 *
 * <p>Según {@link PromptType}:
 * <ul>
 *   <li>{@code CONVERSATION}: requiere {@code context} y {@code decision}; {@code consultingReport} debe ser {@code null}</li>
 *   <li>{@code REPORT}: requiere {@code consultingReport}; {@code context} y {@code decision} deben ser {@code null}</li>
 * </ul>
 */
public record PromptRequest(
    ConsultingReport consultingReport,
    PromptType type,
    ProjectContext context,
    ConversationDecision decision
) {

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
        return new PromptRequest(null, PromptType.CONVERSATION, context, decision);
    }

    public static PromptRequest forReport(ConsultingReport consultingReport) {
        return new PromptRequest(consultingReport, PromptType.REPORT, null, null);
    }
}
