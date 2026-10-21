package com.kinplatform.common.conversation.history;

import com.kinplatform.common.context.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Ventana de contexto por presupuesto de tokens (ADR-013, mejora post-E6).
 *
 * <p>Recorta el historial preservando los mensajes más recientes hasta caber
 * en {@code maxInputTokens - systemPromptTokens - userMessageTokens}.
 * Determinista, sin LLM, compatible con streaming.</p>
 */
public final class TokenBudgetWindow {

    private final TokenEstimator estimator;
    private final int maxInputTokens;
    private final int systemPromptTokenBaseline;

    public TokenBudgetWindow(TokenEstimator estimator, int maxInputTokens, int systemPromptTokenBaseline) {
        if (estimator == null) {
            throw new IllegalArgumentException("estimator no puede ser null");
        }
        if (maxInputTokens <= 0) {
            throw new IllegalArgumentException("maxInputTokens debe ser > 0");
        }
        if (systemPromptTokenBaseline < 0) {
            throw new IllegalArgumentException("systemPromptTokenBaseline no puede ser negativo");
        }
        this.estimator = estimator;
        this.maxInputTokens = maxInputTokens;
        this.systemPromptTokenBaseline = systemPromptTokenBaseline;
    }

    /**
     * Aplica el presupuesto de tokens al historial.
     *
     * @param history       historial completo en orden cronológico (más antiguo primero)
     * @param systemPrompt  system prompt completo que se enviará al LLM
     * @param userMessage   mensaje del usuario del turno actual
     * @return sublista inmutable del historial que cabe en el presupuesto
     */
    public List<Message> apply(List<Message> history, String systemPrompt, String userMessage) {
        if (history == null) {
            throw new IllegalArgumentException("history no puede ser null");
        }
        if (systemPrompt == null) {
            throw new IllegalArgumentException("systemPrompt no puede ser null");
        }
        if (userMessage == null) {
            throw new IllegalArgumentException("userMessage no puede ser null");
        }

        int systemTokens = Math.max(estimator.estimate(systemPrompt), systemPromptTokenBaseline);
        int userTokens = estimator.estimate(userMessage);
        int budget = maxInputTokens - systemTokens - userTokens;

        if (budget <= 0) {
            return List.of();
        }

        int accumulated = 0;
        int startIndex = history.size();

        for (int i = history.size() - 1; i >= 0; i--) {
            int msgTokens = estimator.estimate(history.get(i).content());
            if (accumulated + msgTokens > budget) {
                break;
            }
            accumulated += msgTokens;
            startIndex = i;
        }

        if (startIndex >= history.size()) {
            return List.of();
        }
        return List.copyOf(history.subList(startIndex, history.size()));
    }

    /**
     * Estimador de tokens. Implementación por defecto heurística (4 chars ≈ 1 token).
     * Sustituible por wrapper tiktoken en configuración de producción.
     */
    public interface TokenEstimator {
        int estimate(String text);

        TokenEstimator HEURISTIC = text -> text == null ? 0 : (text.length() + 3) / 4;
    }
}