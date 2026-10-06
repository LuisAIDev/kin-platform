package com.kinplatform.platform.projectdoc.extract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DocumentExtractionServiceTest {

    private final DocumentExtractionService service = new DocumentExtractionService(
            List.of(new PdfTextExtractor(), new OfficeTextExtractor(), new PlainTextExtractor()));

    @Test
    void extraeTxtConElExtractorDeTextoPlano() {
        MockMultipartFile file =
                new MockMultipartFile("file", "notas.txt", "text/plain", "hola kin".getBytes(StandardCharsets.UTF_8));

        String text = service.extract(file, "text/plain");

        assertThat(text).contains("hola kin");
    }

    @Test
    void seleccionaPdfPorSuExtractor() throws Exception {
        byte[] pdf = PdfDocumentFixtures.singlePagePdf("documento");
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdf);

        String text = service.extract(file, "application/pdf");

        assertThat(text).contains("documento");
    }

    @Test
    void formatoNoSoportadoLanzaExcepcion() {
        MockMultipartFile file =
                new MockMultipartFile("file", "datos.rar", "application/x-rar-compressed", new byte[] {1});

        assertThatThrownBy(() -> service.extract(file, "application/x-rar-compressed"))
                .isInstanceOf(TextExtractionException.class)
                .hasMessageContaining("no soportado");
    }
}

