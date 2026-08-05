package com.kinplatform.kin.knowledge.citation;

/**
 * Formateador en línea: {@code (fuente, año)}.
 */
public final class InlineCitationFormatter implements CitationFormatter {

    @Override
    public CitationStyle style() {
        return CitationStyle.INLINE;
    }

    @Override
    public String format(CitationEntry entry, int index) {
        return "(" + entry.sourceId() + ", " + entry.year() + ")";
    }
}
