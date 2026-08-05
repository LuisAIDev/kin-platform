package com.kinplatform.kin.knowledge.citation;

/**
 * Formateador de nota al pie: {@code [n] url (fuente, año)}.
 */
public final class FootnoteCitationFormatter implements CitationFormatter {

    @Override
    public CitationStyle style() {
        return CitationStyle.FOOTNOTE;
    }

    @Override
    public String format(CitationEntry entry, int index) {
        return "[" + index + "] " + entry.url() + " (" + entry.sourceId() + ", " + entry.year() + ")";
    }
}
