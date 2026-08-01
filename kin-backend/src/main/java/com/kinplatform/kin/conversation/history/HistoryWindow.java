package com.kinplatform.kin.conversation.history;

import com.kinplatform.kin.context.Message;

import java.util.List;

/**
 * Ventana de contexto determinista del Conversation Orchestrator (ADR-013).
 *
 * <p>Selecciona el fragmento del historial que el LLM verá en el turno,
 * aplicando un presupuesto por número de mensajes (independiente del
 * tokenizador del proveedor). Conserva los últimos {@code maxMessages} mensajes
 * en orden cronológico; el mensaje del usuario del turno actual —el último del
 * historial— siempre queda incluido. Nunca modifica el contenido de los
 * mensajes ni filtra selectivamente por rol o contenido.</p>
 */
public final class HistoryWindow {

    /** Presupuesto por defecto: últimos 20 mensajes (ADR-013 §7.4). */
    public static final int DEFAULT_MAX_MESSAGES = 20;

    /**
     * Aplica la ventana deslizante sobre el historial.
     *
     * @param history     historial completo en orden cronológico
     * @param maxMessages número máximo de mensajes a conservar (al menos 1)
     * @return lista inmutable con los últimos {@code maxMessages} mensajes
     * @throws IllegalArgumentException si {@code history} es {@code null} o
     *                                  {@code maxMessages} es menor que 1
     */
    public List<Message> window(List<Message> history, int maxMessages) {
        if (history == null) {
            throw new IllegalArgumentException("history no puede ser null");
        }
        if (maxMessages < 1) {
            throw new IllegalArgumentException("maxMessages debe ser al menos 1");
        }
        if (history.size() <= maxMessages) {
            return List.copyOf(history);
        }
        return List.copyOf(history.subList(history.size() - maxMessages, history.size()));
    }
}
