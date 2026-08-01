package com.kinplatform.kin.conversation;

/**
 * Fase del ciclo de conversación entre el usuario y el consultor virtual.
 *
 * <p>Es una decisión 100 % Java (ADR-013): el estado de la conversación se
 * resuelve en el dominio y nunca se infiere desde texto del LLM.</p>
 */
public enum ConversationPhase {
    EXPLORATION,
    REPORTING,
    CLOSED
}
