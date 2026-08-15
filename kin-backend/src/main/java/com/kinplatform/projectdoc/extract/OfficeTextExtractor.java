package com.kinplatform.projectdoc.extract;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Extrae el texto de documentos Office (DOCX y XLSX) mediante Apache POI.
 * NO ejecuta macros ni fórmulas: lee únicamente el contenido textual.
 */
@Component
public class OfficeTextExtractor implements DocumentExtractor {

    private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    public boolean supports(String mimeType, String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return name.endsWith(".docx")
                || name.endsWith(".xlsx")
                || DOCX_MIME.equalsIgnoreCase(mimeType)
                || XLSX_MIME.equalsIgnoreCase(mimeType);
    }

    @Override
    public String extract(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String name = "";
        if (originalFilename != null) {
            name = originalFilename.toLowerCase(Locale.ROOT);
        }
        String contentType = file.getContentType();
        if (name.endsWith(".xlsx") || Objects.equals(XLSX_MIME, contentType)) {
            return extractXlsx(file);
        }
        return extractDocx(file);
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    private String extractXlsx(MultipartFile file) throws IOException {
        DataFormatter formatter = new DataFormatter();
        StringBuilder builder = new StringBuilder();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                builder.append("=== Hoja: ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet) {
                    boolean first = true;
                    for (Cell cell : row) {
                        if (!first) {
                            builder.append('\t');
                        }
                        builder.append(cellText(formatter, cell));
                        first = false;
                    }
                    builder.append('\n');
                }
            }
        }
        return builder.toString();
    }

    private String cellText(DataFormatter formatter, Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return formatter.formatCellValue(cell);
        }
        return formatter.formatCellValue(cell);
    }
}
