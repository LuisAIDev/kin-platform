package com.kinplatform.kin.conversation;

/**
 * Qué debe comunicar el LLM en el turno, según la directiva de comunicación.
 *
 * <p>El LLM únicamente comunica dentro de un modo; el modo es una decisión
 * determinista de Java (ADR-013).</p>
 */
public enum CommunicationMode {
    QUESTION,
    EXPLAIN_REPORT,
    SUMMARY,
    FAREWELL
}
