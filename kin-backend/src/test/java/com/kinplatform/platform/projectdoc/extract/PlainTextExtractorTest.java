package com.kinplatform.platform.projectdoc.extract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PlainTextExtractorTest {

    private final PlainTextExtractor extractor = new PlainTextExtractor();

    @Test
    void soportaTxt() {
        assertThat(extractor.supports("text/plain", "notas.txt")).isTrue();
    }

    @Test
    void soportaCsv() {
        assertThat(extractor.supports("text/csv", "ventas.csv")).isTrue();
    }

    @Test
    void noSoportaPdf() {
        assertThat(extractor.supports("application/pdf", "doc.pdf")).isFalse();
    }

    @Test
    void extraeContenidoUtf8() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notas.txt", "text/plain", "Inversión inicial: 80.000.000".getBytes(StandardCharsets.UTF_8));

        String text = extractor.extract(file);

        assertThat(text).contains("Inversión inicial: 80.000.000");
    }
}

