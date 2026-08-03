package com.kinplatform.kin.enterprise.renderer;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RendererTextTest {

    @Test
    void lines_deberiaDividirPorSaltosDeLinea() {
        assertEquals(List.of("a", "b"), RendererText.lines("a\nb"));
        assertEquals(List.of("a", "b"), RendererText.lines("a\r\nb"));
        assertEquals(List.of("a", "b"), RendererText.lines("a\rb"));
        assertTrue(RendererText.lines(null).isEmpty());
    }

    @Test
    void title_deberiaGenerarUnTituloLegible() {
        assertEquals("LEAN CANVAS", RendererText.title(DocumentType.LEAN_CANVAS));
        assertEquals("", RendererText.title(null));
    }

    @Test
    void xmlEscape_deberiaEscaparLosCaracteresEspeciales() {
        assertEquals("&lt;a&gt;&amp;&quot;b&quot;&apos;",
            RendererText.xmlEscape("<a>&\"b\"'"));
        assertEquals("", RendererText.xmlEscape(null));
    }

    @Test
    void wrap_deberiaAjustarLineasLargas() {
        assertEquals(List.of("corta"), RendererText.wrap("corta", 90));
        List<String> wrapped = RendererText.wrap("palabra larga ".repeat(5), 20);
        assertTrue(wrapped.size() > 1);
        assertTrue(wrapped.stream().allMatch(line -> line.length() <= 20));
    }

    @Test
    void wrap_conLineaSinEspacios_deberiaCortarPorAncho() {
        List<String> wrapped = RendererText.wrap("abcdefghijklmnopqrstuvwxyz", 10);
        assertTrue(wrapped.size() >= 2);
        assertEquals("abcdefghij", wrapped.get(0));
    }

    @Test
    void wrap_cuandoElRestoQuedaVacio_deberiaRetornar() {
        assertEquals(List.of("abc"), RendererText.wrap("abc ", 3));
    }

    @Test
    void wrap_conLineaNula_deberiaDevolverListaVacia() {
        assertTrue(RendererText.wrap(null, 90).isEmpty());
    }

    @Test
    void writeZipEntry_deberiaEscribirLaEntradaConUtf8() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            RendererText.writeZipEntry(zip, "parte.xml", "contenido");
        }
        String content = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("parte.xml"));
    }
}
