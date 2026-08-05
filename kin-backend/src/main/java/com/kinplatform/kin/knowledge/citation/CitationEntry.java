package com.kinplatform.kin.knowledge.citation;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Cita verificada inmutable (Fase 5 — Citation Engine). Conserva la trazabilidad
 * de un hecho: fuente, URL, tipo, fecha, confianza y explicación.
 *
 * <p>Nunca se inventa información: los campos que el hecho congelado no expone
 * (título, licencia, idioma) quedan vacíos ("no declarado").</p>
 */
public record CitationEntry(
    String sourceId,
    String url,
    String title,
    String sourceType,
    OffsetDateTime publishedAt,
    double confidence,
    String license,
    String language,
    String explanation
) {

    public CitationEntry {
        sourceId = sourceId == null ? "" : sourceId;
        url = url == null ? "" : url;
        title = title == null ? "" : title;
        sourceType = sourceType == null ? "" : sourceType;
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        license = license == null ? "" : license;
        language = language == null ? "" : language;
        explanation = explanation == null ? "" : explanation;
    }

    public static CitationEntry of(String sourceId, String url, String sourceType,
                                   OffsetDateTime publishedAt, double confidence, String explanation) {
        return new CitationEntry(sourceId, url, "", sourceType, publishedAt, confidence, "", "", explanation);
    }

    public String year() {
        return publishedAt == null ? "s.f." : String.valueOf(publishedAt.getYear());
    }

    public String confidencePercent() {
        return String.format(Locale.ROOT, "%.0f", confidence * 100);
    }
}
