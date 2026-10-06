package com.kinplatform.platform.projectdoc.extract;

import java.io.IOException;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Extrae el texto de documentos PDF mediante Apache PDFBox. */
@Component
public class PdfTextExtractor implements DocumentExtractor {

    private static final String PDF_MIME = "application/pdf";

    @Override
    public boolean supports(String mimeType, String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf") || PDF_MIME.equalsIgnoreCase(mimeType);
    }

    @Override
    public String extract(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}

