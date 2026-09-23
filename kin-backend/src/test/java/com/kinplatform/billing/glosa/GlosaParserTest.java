package com.kinplatform.billing.glosa;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GlosaParserTest {

    private final GlosaParser parser = new GlosaParser();

    @Test
    void parse_colsanitasPipeFormat() {
        String content = "# glosas Colsanitas\n"
                + "G-001|11111111-1111-1111-1111-111111111111|22222222-2222-2222-2222-222222222222|"
                + "VALOR|TA0201|Tarifa no contratada|150000|45000\n";

        List<Glosa> glosas = parser.parse(content);

        assertEquals(1, glosas.size());
        Glosa glosa = glosas.get(0);
        assertEquals("G-001", glosa.getEpsGlosaNumber());
        assertEquals(Glosa.GlosaType.VALOR, glosa.getGlosaType());
        assertEquals("TA0201", glosa.getGlosaCode());
        assertEquals(new BigDecimal("150000"), glosa.getOriginalValueCop());
        assertEquals(new BigDecimal("45000"), glosa.getGlosaValueCop());
        assertEquals(Glosa.GlosaStatus.RECEIVED, glosa.getStatus());
    }

    @Test
    void parse_suraSemicolonFormat() {
        String content = "G-100;;;CODIGO;CUPS99;Facturado no realizado;80000;80000";

        List<Glosa> glosas = parser.parse(content);

        assertEquals(1, glosas.size());
        assertEquals(Glosa.GlosaType.CODIGO, glosas.get(0).getGlosaType());
        assertNull(glosas.get(0).getRipsBatchId());
    }

    @Test
    void parse_skipsBlankAndMalformedLines() {
        String content = "\n"
                + "# header\n"
                + "solo|dos\n"
                + "G-200|||VALOR|X|desc|1000|1000\n";

        List<Glosa> glosas = parser.parse(content);

        assertEquals(1, glosas.size());
        assertEquals("G-200", glosas.get(0).getEpsGlosaNumber());
    }

    @Test
    void parse_returnsEmptyForNullOrBlank() {
        assertTrue(parser.parse(null).isEmpty());
        assertTrue(parser.parse("   ").isEmpty());
    }
}
