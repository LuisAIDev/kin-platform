package com.kinplatform.kin.export.intent;

import com.kinplatform.kin.export.model.ExportFormat;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Detector determinista de intención de exportación en un mensaje del chat.
 *
 * <p>Reglas de palabras clave (v1, sin convertirse en un sistema complejo de
 * comandos): requiere una palabra de acción (descargar/exportar) y un objeto
 * (proyecto/documento); el formato se infiere por {@code word|pdf|markdown}
 * (por defecto DOCX); si se menciona plantilla/estructura/documento subido, se
 * adjunta el documento de referencia (proporcionado por el llamador). Sin
 * intención clara devuelve {@code null}.</p>
 */
public final class ExportIntentDetector {

    private static final List<String> ACTION_WORDS =
            List.of("descargar", "descargame", "descarga", "exportar", "exporta", "exportame", "bajar");
    private static final List<String> OBJECT_WORDS = List.of("proyecto", "documento");
    private static final List<String> TEMPLATE_WORDS = List.of(
            "plantilla",
            "estructura",
            "documento que subi",
            "documento cargado",
            "documento que cargue",
            "documento de referencia");
    private static final List<String> WORD_WORDS = List.of("word", "docx");
    private static final List<String> PDF_WORDS = List.of("pdf");
    private static final List<String> MARKDOWN_WORDS = List.of("markdown", " .md", " md");

    public ExportAction detect(String message, UUID mostRecentProcessedDocumentId) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String normalized = normalize(message);
        if (!containsAny(normalized, ACTION_WORDS) || !containsAny(normalized, OBJECT_WORDS)) {
            return null;
        }
        ExportFormat format = formatOf(normalized);
        UUID templateDocumentId = null;
        if (containsAny(normalized, TEMPLATE_WORDS)) {
            templateDocumentId = mostRecentProcessedDocumentId;
        }
        return ExportAction.of(format, templateDocumentId, null);
    }

    private ExportFormat formatOf(String normalized) {
        if (containsAny(normalized, PDF_WORDS)) {
            return ExportFormat.PDF;
        }
        if (containsAny(normalized, MARKDOWN_WORDS)) {
            return ExportFormat.MARKDOWN;
        }
        return ExportFormat.DOCX;
    }

    private static boolean containsAny(String text, List<String> words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
