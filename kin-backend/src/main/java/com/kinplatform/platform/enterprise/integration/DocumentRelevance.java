package com.kinplatform.platform.enterprise.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Relevancia documental determinista y sin LLM: identifica afirmaciones
 * relevantes para el análisis del proyecto a partir del texto extraído de un
 * documento. NUNCA se envía el texto completo a la IA ni al chat.
 */
public final class DocumentRelevance {

    private static final Set<String> KEYWORDS = Set.of(
            "inversión",
            "inversion",
            "costo",
            "costos",
            "costo fijo",
            "costo variable",
            "precio",
            "venta",
            "ventas",
            "ingreso",
            "ingresos",
            "margen",
            "mercado",
            "competidor",
            "competidores",
            "competencia",
            "cliente",
            "clientes",
            "público objetivo",
            "publico objetivo",
            "proveedor",
            "proveedores",
            "empleado",
            "empleados",
            "equipo",
            "equipos",
            "socio",
            "socios",
            "canal",
            "impacto",
            "riesgo",
            "riesgos",
            "proyección",
            "proyeccion",
            "unidades",
            "$",
            "%");

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int DEFAULT_MAX_FACTS = 12;

    private DocumentRelevance() {}

    /** Resumen controlado del texto extraído (sin exponer el documento completo). */
    public static String summarize(String extractedText, int maxChars) {
        if (extractedText == null || extractedText.isBlank()) {
            return null;
        }
        String cleaned = WHITESPACE
                .matcher(extractedText.replace('\r', ' '))
                .replaceAll(" ")
                .trim();
        if (cleaned.length() <= maxChars) {
            return cleaned;
        }
        return cleaned.substring(0, maxChars).trim() + "…";
    }

    /**
     * Afirmaciones del documento con información potencialmente relevante.
     * Cada hecho conserva su procedencia documental (el adaptador lo marca como
     * {@code IMPORTED_DOCUMENT}); nunca se convierte automáticamente en
     * {@code USER_INPUT}.
     */
    public static List<String> findRelevant(String extractedText, int maxFacts) {
        if (extractedText == null || extractedText.isBlank()) {
            return List.of();
        }
        List<String> relevant = new ArrayList<>();
        for (String sentence : SENTENCE_SPLIT.split(extractedText)) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (KEYWORDS.stream().anyMatch(lower::contains)) {
                relevant.add(trimmed);
                if (relevant.size() >= Math.max(1, maxFacts)) {
                    break;
                }
            }
        }
        return List.copyOf(relevant);
    }

    public static List<String> findRelevant(String extractedText) {
        return findRelevant(extractedText, DEFAULT_MAX_FACTS);
    }
}

