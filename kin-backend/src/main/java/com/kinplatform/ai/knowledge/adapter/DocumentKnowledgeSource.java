package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de infraestructura de documentos (ADR-014, §6.5): implementa el
 * puerto {@link KnowledgeSource} sobre la ingestión de documentos.
 *
 * <p>Estructura preparada: no parsea documentos reales. La ingestión se delega
 * en una {@link DocumentParser} (interfaz/stub) inyectada y las rutas a ingerir
 * se configuran en el constructor. El adaptador únicamente envuelve cada
 * documento crudo en un {@link KnowledgeCandidate}; nunca valida, nunca decide
 * ni interpreta negocio (offline-first: ante error o nulo degrada a lista
 * vacía).</p>
 */
public class DocumentKnowledgeSource implements KnowledgeSource {

    /** Parser de documentos de infraestructura (stub; sin parser funcional). */
    public interface DocumentParser {
        List<Document> parse(String path);
    }

    /** Documento crudo devuelto por el parser. */
    public record Document(String content, String title, String url, OffsetDateTime publishedAt) {

        public Document {
            content = content == null ? "" : content;
            title = title == null ? "" : title;
            url = url == null ? "" : url;
        }
    }

    private final String sourceId;
    private final String sourceName;
    private final DocumentParser parser;
    private final List<String> documentPaths;

    public DocumentKnowledgeSource(String sourceId, String sourceName, DocumentParser parser,
                                   List<String> documentPaths) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.parser = parser;
        this.documentPaths = documentPaths == null ? List.of() : List.copyOf(documentPaths);
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null) {
            return List.of();
        }
        try {
            var candidates = new ArrayList<KnowledgeCandidate>();
            for (var path : documentPaths) {
                List<Document> documents = parser.parse(path);
                if (documents == null) {
                    continue;
                }
                for (var document : documents) {
                    if (document == null) {
                        continue;
                    }
                    candidates.add(new KnowledgeCandidate(
                        document.content(), sourceId, sourceName, document.url(), document.publishedAt(),
                        "application/pdf", Map.of()));
                }
            }
            return List.copyOf(candidates);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }
}
