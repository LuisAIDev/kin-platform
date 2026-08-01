package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adaptador de infraestructura RAG (ADR-014, §6.4): implementa el puerto
 * {@link KnowledgeSource} contra un índice vectorial.
 *
 * <p>Estructura preparada: no consume ninguna base vectorial real. La
 * recuperación se delega en una {@link VectorIndex} (interfaz/stub) inyectada;
 * el adaptador únicamente envuelve cada hit crudo en un {@link KnowledgeCandidate}
 * preservando el orden del índice (sin ordenar ni filtrar: la selección la hace
 * {@code SourceValidator} en el dominio). Nunca decide ni interpreta negocio
 * (offline-first: ante error o nulo degrada a lista vacía).</p>
 */
public class RagKnowledgeSource implements KnowledgeSource {

    /** Índice vectorial de infraestructura (stub; sin embeddings ni vectores). */
    public interface VectorIndex {
        List<Hit> search(String text, int limit);
    }

    /** Hit crudo devuelto por el índice vectorial. */
    public record Hit(String content, String url, OffsetDateTime publishedAt, double similarity) {

        public Hit {
            content = content == null ? "" : content;
            url = url == null ? "" : url;
        }
    }

    private final String sourceId;
    private final String sourceName;
    private final VectorIndex index;

    public RagKnowledgeSource(String sourceId, String sourceName, VectorIndex index) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.index = index;
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null) {
            return List.of();
        }
        try {
            List<Hit> hits = index.search(query.topic(), query.limit());
            if (hits == null) {
                return List.of();
            }
            var candidates = new ArrayList<KnowledgeCandidate>();
            for (var hit : hits) {
                if (hit == null) {
                    continue;
                }
                candidates.add(new KnowledgeCandidate(
                    hit.content(), sourceId, sourceName, hit.url(), hit.publishedAt(),
                    "application/json",
                    Map.of("similarity", String.format(Locale.ROOT, "%.2f", hit.similarity()))));
            }
            return List.copyOf(candidates);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }
}
