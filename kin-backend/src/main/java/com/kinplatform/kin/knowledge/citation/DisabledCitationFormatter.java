package com.kinplatform.kin.knowledge.citation;

/**
 * Formateador deshabilitado: no genera referencias (el motor corta antes).
 */
public final class DisabledCitationFormatter implements CitationFormatter {

    @Override
    public CitationStyle style() {
        return CitationStyle.DISABLED;
    }

    @Override
    public String format(CitationEntry entry, int index) {
        return "";
    }
}
