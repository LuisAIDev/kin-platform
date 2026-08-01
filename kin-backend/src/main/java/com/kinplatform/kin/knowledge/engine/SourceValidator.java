package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.SourceValidation;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validador determinista de candidatos de conocimiento (ADR-014, §6.7).
 *
 * <p>Aplica en Java las reglas de validación en orden: protocolo HTTPS,
 * allowlist de dominios, estado HTTP (2xx), tipo de contenido, formato,
 * frescura (ventana TTL), deduplicación por {@code (sourceId, url)} y nivel de
 * confianza derivado del tipo de fuente. Nunca consulta al LLM.</p>
 *
 * <p>Servicio de dominio puro: stateless y reentrante (la deduplicación vive
 * dentro de {@link #validateAll}, no en el estado del servicio). La
 * configuración de reglas se inyecta por constructor como valores inmutables;
 * no hay Spring ni infraestructura.</p>
 */
public class SourceValidator {

    /** Clave en {@code meta} del candidato con el tipo de fuente. */
    public static final String META_SOURCE_TYPE = "source_type";
    /** Clave en {@code meta} del candidato con el código de estado HTTP. */
    public static final String META_HTTP_STATUS = "http_status";
    /** Clave en {@code meta} del candidato con la categoría del hecho. */
    public static final String META_CATEGORY = "category";

    private static final String REASON_INVALID_URL = "URL inválida";
    private static final String REASON_PROTOCOL = "Protocolo HTTPS obligatorio";
    private static final String REASON_DOMAIN = "Dominio no permitido";
    private static final String REASON_HTTP_STATUS = "Estado HTTP no es 2xx";
    private static final String REASON_HTTP_STATUS_INVALID = "Estado HTTP inválido";
    private static final String REASON_NO_CONTENT_TYPE = "Formato inválido: tipo de contenido ausente";
    private static final String REASON_CONTENT_TYPE = "Tipo de contenido no permitido";
    private static final String REASON_EMPTY_CONTENT = "Formato inválido: contenido vacío";
    private static final String REASON_NO_DATE = "Publicado sin fecha (frescura no verificable)";
    private static final String REASON_STALE = "Fuera de la ventana de frescura";
    private static final String REASON_DUPLICATE = "Fuente duplicada (sourceId, url)";

    private final Set<String> allowedDomains;
    private final Duration maxAge;
    private final Set<String> allowedContentTypes;

    /**
     * @param allowedDomains       dominios/hosts permitidos (allowlist); vacío
     *                             rechaza todo candidato
     * @param maxAge               ventana máxima de frescura ({@code publishedAt});
     *                             si es {@code null} se usa
     *                             {@link KnowledgeRequest#DEFAULT_TIME_WINDOW}
     * @param allowedContentTypes  tipos de contenido permitidos; vacío acepta
     *                             cualquier tipo no vacío
     */
    public SourceValidator(Set<String> allowedDomains, Duration maxAge, Set<String> allowedContentTypes) {
        this.allowedDomains = lowerCaseCopy(allowedDomains);
        this.maxAge = maxAge == null ? KnowledgeRequest.DEFAULT_TIME_WINDOW : maxAge;
        this.allowedContentTypes = normalizeContentTypes(allowedContentTypes);
    }

    /**
     * Validador estricto por defecto: sin dominios permitidos (offline-first) y
     * ventana de frescura anual.
     */
    public static SourceValidator strict() {
        return new SourceValidator(Set.of(), Duration.ofDays(365), Set.of());
    }

    /**
     * Valida un candidato sin aplicar deduplicación (contrato ADR-014 §7).
     */
    public SourceValidation validate(KnowledgeCandidate candidate) {
        return validateSingle(candidate, null);
    }

    /**
     * Valida una lista de candidatos con deduplicación por {@code (sourceId, url)}
     * aplicada en orden dentro de esta misma llamada. Devuelve una validación por
     * candidato, en el mismo orden.
     */
    public List<SourceValidation> validateAll(List<KnowledgeCandidate> candidates) {
        if (candidates == null) {
            return List.of();
        }
        var seenKeys = new LinkedHashSet<String>();
        var result = new ArrayList<SourceValidation>(candidates.size());
        for (var candidate : candidates) {
            result.add(validateSingle(candidate, seenKeys));
        }
        return List.copyOf(result);
    }

    private SourceValidation validateSingle(KnowledgeCandidate candidate, Set<String> seenKeys) {
        if (candidate == null) {
            return SourceValidation.rejected("Candidato nulo");
        }
        var reasons = new ArrayList<String>();
        validateUrl(candidate.url(), reasons);
        validateHttpStatus(candidate.meta(), reasons);
        validateContentType(candidate.contentType(), reasons);
        validateContent(candidate.content(), reasons);
        validateFreshness(candidate.publishedAt(), reasons);
        if (seenKeys != null && !seenKeys.add(dedupKey(candidate))) {
            reasons.add(REASON_DUPLICATE);
        }
        if (!reasons.isEmpty()) {
            return new SourceValidation(false, SourceTrust.UNVERIFIED, List.copyOf(reasons));
        }
        return SourceValidation.accepted(deriveTrust(candidate.meta()));
    }

    private void validateUrl(String url, List<String> reasons) {
        URI uri;
        try {
            uri = URI.create(url == null ? "" : url);
        } catch (RuntimeException ex) {
            reasons.add(REASON_INVALID_URL);
            return;
        }
        if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
            reasons.add(REASON_PROTOCOL);
        }
        String host = uri.getHost();
        if (host == null || !allowedDomains.contains(host.toLowerCase(Locale.ROOT))) {
            reasons.add(REASON_DOMAIN);
        }
    }

    private void validateHttpStatus(Map<String, String> meta, List<String> reasons) {
        String raw = meta.get(META_HTTP_STATUS);
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            int status = Integer.parseInt(raw.trim());
            if (status < 200 || status > 299) {
                reasons.add(REASON_HTTP_STATUS);
            }
        } catch (NumberFormatException ex) {
            reasons.add(REASON_HTTP_STATUS_INVALID);
        }
    }

    private void validateContentType(String contentType, List<String> reasons) {
        if (contentType == null || contentType.isBlank()) {
            reasons.add(REASON_NO_CONTENT_TYPE);
            return;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (!allowedContentTypes.isEmpty() && !allowedContentTypes.contains(normalized)) {
            reasons.add(REASON_CONTENT_TYPE);
        }
    }

    private void validateContent(String content, List<String> reasons) {
        if (content == null || content.isBlank()) {
            reasons.add(REASON_EMPTY_CONTENT);
        }
    }

    private void validateFreshness(OffsetDateTime publishedAt, List<String> reasons) {
        if (publishedAt == null) {
            reasons.add(REASON_NO_DATE);
            return;
        }
        if (publishedAt.isBefore(OffsetDateTime.now().minus(maxAge))) {
            reasons.add(REASON_STALE);
        }
    }

    private SourceTrust deriveTrust(Map<String, String> meta) {
        String type = meta.get(META_SOURCE_TYPE);
        if (type == null) {
            return SourceTrust.UNVERIFIED;
        }
        switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "official":
            case "official_public":
                return SourceTrust.OFFICIAL_PUBLIC;
            case "secondary":
                return SourceTrust.SECONDARY;
            default:
                return SourceTrust.UNVERIFIED;
        }
    }

    private String dedupKey(KnowledgeCandidate candidate) {
        return candidate.sourceId() + "|" + candidate.url();
    }

    private Set<String> lowerCaseCopy(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        var out = new LinkedHashSet<String>();
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(out);
    }

    private Set<String> normalizeContentTypes(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        var out = new LinkedHashSet<String>();
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(out);
    }
}
