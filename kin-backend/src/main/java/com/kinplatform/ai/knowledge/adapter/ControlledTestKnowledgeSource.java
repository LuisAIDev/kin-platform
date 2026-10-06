package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.common.knowledge.KnowledgeCandidate;
import com.kinplatform.common.knowledge.KnowledgeQuery;
import com.kinplatform.common.knowledge.KnowledgeSource;
import com.kinplatform.common.knowledge.engine.SourceValidator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fuente de conocimiento controlada y determinista (ADR-021).
 *
 * <p>Permite ejercitar el pipeline completo de adquisición
 * (fuente → candidatos → {@code SourceValidator} → {@code KnowledgeGateway} →
 * {@code EnrichmentEngine}) sin depender de Internet, en dev/test y en
 * pruebas E2E controladas. Activa únicamente con
 * {@code kin.knowledge.test-source.enabled=true} (default {@code false}).</p>
 *
 * <p>Los candidatos se construyen con URLs https y metadata trazable
 * ({@code http_status}, {@code source_type}, {@code category}); para que pasen
 * la validación, el operador debe incluir su dominio en
 * {@code kin.knowledge.allowed-domains} (Java decide la confiabilidad, nunca el
 * LLM).</p>
 */
public class ControlledTestKnowledgeSource implements KnowledgeSource {

    private final String sourceId;
    private final String sourceName;
    private final List<KinKnowledgeProperties.TestCandidate> candidates;

    public ControlledTestKnowledgeSource(
            String sourceId, String sourceName, List<KinKnowledgeProperties.TestCandidate> candidates) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null || candidates.isEmpty()) {
            return List.of();
        }
        var out = new ArrayList<KnowledgeCandidate>();
        for (var candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String url = candidate.getUrl() == null ? "" : candidate.getUrl();
            String category = candidate.getCategory() == null ? "" : candidate.getCategory();
            String sourceType = candidate.getSourceType() == null ? "official_public" : candidate.getSourceType();
            OffsetDateTime publishedAt = parsePublishedAt(candidate.getPublishedAt());
            out.add(new KnowledgeCandidate(
                    candidate.getContent(),
                    sourceId,
                    sourceName,
                    url,
                    publishedAt,
                    "application/json",
                    Map.of(
                            SourceValidator.META_HTTP_STATUS, "200",
                            SourceValidator.META_SOURCE_TYPE, sourceType,
                            SourceValidator.META_CATEGORY, category)));
        }
        return List.copyOf(out);
    }

    private OffsetDateTime parsePublishedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return OffsetDateTime.now();
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (RuntimeException ex) {
            return OffsetDateTime.now();
        }
    }
}

