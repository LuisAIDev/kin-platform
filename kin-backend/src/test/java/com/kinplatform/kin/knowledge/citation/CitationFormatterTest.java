package com.kinplatform.kin.knowledge.citation;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationFormatterTest {

    private CitationEntry entry() {
        return CitationEntry.of("src-1", "https://example.com/report", "MERCADO",
            OffsetDateTime.parse("2026-01-15T10:00:00-05:00"), 1.0, "ok");
    }

    private CitationEntry entrySinFecha() {
        return CitationEntry.of("src-2", "https://example.com/x", "", null, 0.7, "ok");
    }

    @Test
    void inline_deberiaFormatearEnLinea() {
        var formatter = new InlineCitationFormatter();

        assertEquals("(src-1, 2026)", formatter.format(entry(), 1));
        assertEquals(CitationStyle.INLINE, formatter.style());
    }

    @Test
    void footnote_deberiaNumerar() {
        var formatter = new FootnoteCitationFormatter();

        assertEquals("[1] https://example.com/report (src-1, 2026)", formatter.format(entry(), 1));
        assertEquals("[2] https://example.com/report (src-1, 2026)", formatter.format(entry(), 2));
    }

    @Test
    void appendix_deberiaIncluirFecha() {
        var formatter = new AppendixCitationFormatter();

        assertEquals("[1] https://example.com/report — src-1 — 2026-01-15", formatter.format(entry(), 1));
        assertEquals("[1] https://example.com/x — src-2 — s.f.", formatter.format(entrySinFecha(), 1));
    }

    @Test
    void hidden_y_disabled_deberianNoGenerarReferencias() {
        assertEquals("", new HiddenCitationFormatter().format(entry(), 1));
        assertEquals("", new DisabledCitationFormatter().format(entry(), 1));
        assertEquals(CitationStyle.HIDDEN, new HiddenCitationFormatter().style());
        assertEquals(CitationStyle.DISABLED, new DisabledCitationFormatter().style());
    }

    @Test
    void registry_deberiaResolverPorEstilo() {
        var registry = CitationFormatterRegistry.defaults();

        assertEquals(CitationStyle.INLINE, registry.formatterFor(CitationStyle.INLINE).style());
        assertEquals(CitationStyle.FOOTNOTE, registry.formatterFor(CitationStyle.FOOTNOTE).style());
        assertEquals(CitationStyle.APPENDIX, registry.formatterFor(CitationStyle.APPENDIX).style());
        assertEquals(CitationStyle.HIDDEN, registry.formatterFor(CitationStyle.HIDDEN).style());
        assertEquals(CitationStyle.DISABLED, registry.formatterFor(CitationStyle.DISABLED).style());
        assertEquals(CitationStyle.HIDDEN, registry.formatterFor(null).style());
        assertEquals(CitationStyle.HIDDEN, new CitationFormatterRegistry(null).formatterFor(CitationStyle.APPENDIX).style());
    }

    @Test
    void registry_deberiaPermitirRegistrarEstrategias() {
        var registry = new CitationFormatterRegistry(
            java.util.List.of(new InlineCitationFormatter(), new AppendixCitationFormatter()));

        assertEquals(CitationStyle.INLINE, registry.formatterFor(CitationStyle.INLINE).style());
        assertEquals(CitationStyle.APPENDIX, registry.formatterFor(CitationStyle.APPENDIX).style());
        assertEquals(CitationStyle.HIDDEN, registry.formatterFor(CitationStyle.FOOTNOTE).style());
    }
}
