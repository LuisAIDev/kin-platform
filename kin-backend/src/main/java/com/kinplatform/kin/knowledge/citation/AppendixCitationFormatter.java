package com.kinplatform.kin.knowledge.citation;

/**
 * Formateador de apéndice: {@code [n] url — fuente — fecha}.
 */
public final class AppendixCitationFormatter implements CitationFormatter {

    @Override
    public CitationStyle style() {
        return CitationStyle.APPENDIX;
    }

    @Override
    public String format(CitationEntry entry, int index) {
        String date = entry.publishedAt() == null ? "s.f." : entry.publishedAt().toLocalDate().toString();
        return "[" + index + "] " + entry.url() + " — " + entry.sourceId() + " — " + date;
    }
}
