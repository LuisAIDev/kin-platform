package com.kinplatform.platform.projectdoc.extract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void soportaPdf() {
        assertThat(extractor.supports("application/pdf", "plan.pdf")).isTrue();
    }

    @Test
    void noSoportaTxt() {
        assertThat(extractor.supports("text/plain", "notas.txt")).isFalse();
    }

    @Test
    void extraeTextoDePdfReal() throws Exception {
        byte[] pdf = buildPdf("Plan de negocio KIN 2026");

        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", pdf);

        String text = extractor.extract(file);

        assertThat(text).contains("Plan de negocio KIN 2026");
    }

    private byte[] buildPdf(String text) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(text);
                stream.endText();
            }
            document.save(out);
        }
        return out.toByteArray();
    }
}

