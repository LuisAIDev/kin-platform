package com.kinplatform.kin.knowledge.citation;

/**
 * Formateador oculto: las citas se rastrean pero no se muestran (referencias
 * vacías).
 */
public final class HiddenCitationFormatter implements CitationFormatter {

    @Override
    public CitationStyle style() {
        return CitationStyle.HIDDEN;
    }

    @Override
    public String format(CitationEntry entry, int index) {
        return "";
    }
}
