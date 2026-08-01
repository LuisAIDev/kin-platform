package com.kinplatform.kin.conversation.policy;

import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.TurnDirective;
import com.kinplatform.kin.decision.ConversationDecision;

/**
 * Contrato de la política de turno del Conversation Orchestrator (ADR-013).
 *
 * <p>Decide la directiva de comunicación (fase, modo, restricciones) a partir
 * del contexto persistido del proyecto y de la decisión previa de la
 * conversación. Es una decisión 100 % Java: el LLM únicamente comunica dentro
 * de la directiva.</p>
 */
public interface TurnPolicy {

    TurnDirective decide(ProjectContext context, ConversationDecision previousDecision);
}
