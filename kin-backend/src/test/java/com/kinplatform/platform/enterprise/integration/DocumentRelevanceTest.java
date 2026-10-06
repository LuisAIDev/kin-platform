package com.kinplatform.platform.enterprise.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentRelevanceTest {

    @Test
    void documentoRelevanteDevuelveAfirmacionesConInformacion() {
        String text = "El plan considera una inversión inicial de 80 millones. "
                + "La venta estimada anual es de 12.000 unidades. "
                + "Los competidores principales son dos firmas locales.";

        List<String> relevant = DocumentRelevance.findRelevant(text);

        assertThat(relevant).isNotEmpty();
        assertThat(relevant.get(0)).contains("inversión");
    }

    @Test
    void documentoIrrelevanteNoDevuelveAfirmaciones() {
        String text = "Hola mundo, esto es un texto de prueba sin datos de negocio.";

        List<String> relevant = DocumentRelevance.findRelevant(text);

        assertThat(relevant).isEmpty();
    }

    @Test
    void documentoVacioOCorruptoSeManeja() {
        assertThat(DocumentRelevance.findRelevant(null)).isEmpty();
        assertThat(DocumentRelevance.findRelevant("")).isEmpty();
        assertThat(DocumentRelevance.summarize(null, 100)).isNull();
        assertThat(DocumentRelevance.summarize("", 100)).isNull();
    }

    @Test
    void resumenControladoAcotaElTexto() {
        String longText = "a ".repeat(1000);

        String summary = DocumentRelevance.summarize(longText, 300);

        assertThat(summary.length()).isLessThanOrEqualTo(301);
        assertThat(summary).endsWith("…");
    }
}

