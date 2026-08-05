package com.kinplatform.kin.knowledge.citation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de formateadores (Fase 5 — Citation Engine, Factory Pattern):
 * resuelve un {@link CitationStyle} en su formateador. Registrable (OCP):
 * agregar un estilo/forma (APA, IEEE, HTML, JSON…) no modifica el motor.
 */
public class CitationFormatterRegistry {

    private final Map<CitationStyle, CitationFormatter> formatters;

    public CitationFormatterRegistry(List<CitationFormatter> formatters) {
        var map = new EnumMap<CitationStyle, CitationFormatter>(CitationStyle.class);
        if (formatters != null) {
            for (var formatter : formatters) {
                if (formatter != null && formatter.style() != null) {
                    map.put(formatter.style(), formatter);
                }
            }
        }
        this.formatters = Map.copyOf(map);
    }

    public static CitationFormatterRegistry defaults() {
        return new CitationFormatterRegistry(List.of(
            new InlineCitationFormatter(), new FootnoteCitationFormatter(),
            new AppendixCitationFormatter(), new HiddenCitationFormatter(),
            new DisabledCitationFormatter()));
    }

    public CitationFormatter formatterFor(CitationStyle style) {
        if (style == null) {
            return new HiddenCitationFormatter();
        }
        return formatters.getOrDefault(style, new HiddenCitationFormatter());
    }
}
