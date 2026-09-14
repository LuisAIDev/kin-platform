package com.kinplatform.kin.health.documents.infrastructure;

import com.kinplatform.kin.health.documents.application.GoogleVisionOcrService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Extrae el texto plano de un documento clínico ya almacenado (byte[]) para
 * alimentar el análisis con IA.
 *
 * <p>Port de la extracción de documentos del módulo Empresa ({@code projectdoc})
 * al dominio de salud, pero sobre {@code byte[]} (el archivo vive en el
 * filesystem local, no en un {@code MultipartFile}). Formatos soportados:
 * PDF (PDFBox), DOCX/XLSX (Apache POI), TXT/CSV (UTF-8) e imágenes
 * (JPG/PNG/HEIC/WebP) vía Google Cloud Vision OCR.</p>
 *
 * <p>Comportamiento tolerante: un PDF escaneado (sin capa de texto), un formato
 * no soportado o un archivo corrupto devuelven texto vacío — nunca una excepción
 * que rompa la subida. El flujo de IA decide entonces cómo comunicar que el
 * documento no es analizable de forma automática.</p>
 */
@Component
public class ClinicalDocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(ClinicalDocumentTextExtractor.class);

    private static final String MIME_PDF = "application/pdf";
    private static final String MIME_DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String MIME_XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final GoogleVisionOcrService googleVisionOcrService;

    public ClinicalDocumentTextExtractor(GoogleVisionOcrService googleVisionOcrService) {
        this.googleVisionOcrService = googleVisionOcrService;
    }

    /**
     * Extrae el texto del documento.
     *
     * @return texto extraído (posiblemente vacío), nunca {@code null}.
     */
    public String extract(byte[] content, String fileName, String mimeType) {
        if (content == null || content.length == 0) {
            return "";
        }
        String ext = extension(fileName);
        String mime = mimeType == null ? "" : mimeType;
        
        // Detectar si es imagen y usar OCR
        if (mime.startsWith("image/")) {
            try {
                String ocrText = googleVisionOcrService.extractText(content);
                if (ocrText.isBlank()) {
                    throw new RuntimeException(
                        "No se detectó texto en la imagen. Intenta con mejor iluminación o sube un PDF."
                    );
                }
                return ocrText;
            } catch (IOException e) {
                throw new RuntimeException("Error al procesar la imagen con OCR", e);
            }
        }

        try {
            if ("pdf".equals(ext) || MIME_PDF.equalsIgnoreCase(mime)) {
                return extractPdf(content);
            }
            if ("xlsx".equals(ext) || MIME_XLSX.equalsIgnoreCase(mime)) {
                return extractXlsx(content);
            }
            if ("docx".equals(ext) || MIME_DOCX.equalsIgnoreCase(mime)) {
                return extractDocx(content);
            }
            if ("txt".equals(ext) || "csv".equals(ext)) {
                return new String(content, StandardCharsets.UTF_8);
            }
            return "";
        } catch (Exception e) {
            log.warn("ClinicalDocumentTextExtractor: no se pudo extraer el texto de '{}': {}",
                    fileName, e.getMessage());
            return "";
        }
    }

    private String extractPdf(byte[] content) throws IOException {
        try (PDDocument document = PDDocument.load(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    private String extractXlsx(byte[] content) throws IOException {
        DataFormatter formatter = new DataFormatter();
        StringBuilder builder = new StringBuilder();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                builder.append("=== Hoja: ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet) {
                    boolean first = true;
                    for (var cell : row) {
                        if (!first) {
                            builder.append('\t');
                        }
                        builder.append(formatter.formatCellValue(cell));
                        first = false;
                    }
                    builder.append('\n');
                }
            }
        }
        return builder.toString();
    }

    private String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }
}
