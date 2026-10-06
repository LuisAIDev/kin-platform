package com.kinplatform.platform.projectdoc.extract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class OfficeTextExtractorTest {

    private final OfficeTextExtractor extractor = new OfficeTextExtractor();

    @Test
    void soportaDocx() {
        assertThat(extractor.supports(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "propuesta.docx"))
                .isTrue();
    }

    @Test
    void soportaXlsx() {
        assertThat(extractor.supports(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "finanzas.xlsx"))
                .isTrue();
    }

    @Test
    void noSoportaPdf() {
        assertThat(extractor.supports("application/pdf", "doc.pdf")).isFalse();
    }

    @Test
    void extraeTextoDeDocxReal() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText("Contenido DOCX de KIN");
            document.write(out);
        }
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "propuesta.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                out.toByteArray());

        String text = extractor.extract(file);

        assertThat(text).contains("Contenido DOCX de KIN");
    }

    @Test
    void extraeTextoDeXlsxReal() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Finanzas");
            sheet.createRow(0).createCell(0).setCellValue("Inversión inicial");
            sheet.createRow(1).createCell(0).setCellValue(50000.0);
            workbook.write(out);
        }
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "finanzas.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());

        String text = extractor.extract(file);

        assertThat(text).contains("Finanzas");
        assertThat(text).contains("Inversión inicial");
    }
}

