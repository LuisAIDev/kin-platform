package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.ports.DocumentRenderer;
import com.kinplatform.kin.enterprise.renderer.DocxDocumentRenderer;
import com.kinplatform.kin.enterprise.renderer.PdfDocumentRenderer;
import com.kinplatform.kin.enterprise.renderer.PptxDocumentRenderer;
import com.kinplatform.kin.enterprise.valueobjects.RenderFormat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnterpriseRendererFactoryTest {

    @Test
    void constructorPorDefecto_deberiaSoportarLosTresFormatos() {
        var factory = new EnterpriseRendererFactory();

        assertEquals(Set.of(RenderFormat.PDF, RenderFormat.DOCX, RenderFormat.PPTX),
            factory.supportedFormats());
        assertTrue(factory.supports(RenderFormat.PDF));
        assertTrue(factory.supports(RenderFormat.DOCX));
        assertTrue(factory.supports(RenderFormat.PPTX));
    }

    @Test
    void rendererFor_deberiaResolverPorFormato() {
        var factory = new EnterpriseRendererFactory();

        assertSame(RenderFormat.PDF, factory.rendererFor(RenderFormat.PDF).format());
        assertSame(RenderFormat.DOCX, factory.rendererFor(RenderFormat.DOCX).format());
        assertSame(RenderFormat.PPTX, factory.rendererFor(RenderFormat.PPTX).format());
        assertTrue(factory.rendererFor(RenderFormat.PDF) instanceof PdfDocumentRenderer);
        assertTrue(factory.rendererFor(RenderFormat.DOCX) instanceof DocxDocumentRenderer);
        assertTrue(factory.rendererFor(RenderFormat.PPTX) instanceof PptxDocumentRenderer);
    }

    @Test
    void rendererFor_conFormatoNoSoportado_deberiaLanzar() {
        var factory = new EnterpriseRendererFactory(List.of(new PdfDocumentRenderer()));

        assertFalse(factory.supports(RenderFormat.DOCX));
        assertEquals(Set.of(RenderFormat.PDF), factory.supportedFormats());
        assertThrows(IllegalArgumentException.class,
            () -> factory.rendererFor(RenderFormat.DOCX));
    }

    @Test
    void rendererFor_conFormatoNulo_deberiaLanzar() {
        var factory = new EnterpriseRendererFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.rendererFor(null));
    }

    @Test
    void supports_conFormatoNulo_deberiaDevolverFalso() {
        var factory = new EnterpriseRendererFactory();
        assertFalse(factory.supports(null));
    }

    @Test
    void constructor_conListaNula_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseRendererFactory(null));
    }

    @Test
    void constructor_conElementoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseRendererFactory(java.util.Arrays.asList(new PdfDocumentRenderer(), null)));
    }

    @Test
    void constructor_conFormatoDuplicado_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseRendererFactory(List.of(
                new PdfDocumentRenderer(), new PdfDocumentRenderer())));
    }

    @Test
    void constructor_conRenderizadorSinFormato_deberiaLanzar() {
        var sinFormato = mock(DocumentRenderer.class);
        when(sinFormato.format()).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseRendererFactory(List.of(sinFormato)));
    }

    @Test
    void supportedFormats_deberiaSerInmutable() {
        var factory = new EnterpriseRendererFactory();
        assertThrows(UnsupportedOperationException.class,
            () -> factory.supportedFormats().add(RenderFormat.PDF));
        assertNotNull(factory.rendererFor(RenderFormat.PDF));
    }
}
