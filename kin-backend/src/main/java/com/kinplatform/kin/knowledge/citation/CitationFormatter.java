package com.kinplatform.kin.knowledge.citation;

/**
 * Contrato de un formateador de citas (Fase 5 — Citation Engine, Strategy
 * Pattern): produce la referencia textual de una cita para un estilo concreto.
 * Nuevos formatos (APA, IEEE, MLA, HTML, JSON…) son estrategias registrables.
 */
public interface CitationFormatter {

    CitationStyle style();

    String format(CitationEntry entry, int index);
}
