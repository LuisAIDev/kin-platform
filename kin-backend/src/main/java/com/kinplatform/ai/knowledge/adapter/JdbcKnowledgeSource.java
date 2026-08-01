package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptador de infraestructura de base de datos (ADR-014, §6.2): implementa el
 * puerto {@link KnowledgeSource} sobre un almacén de conocimiento separado.
 *
 * <p>Estructura preparada: no toca bases de datos reales. El acceso se delega
 * en una {@link KnowledgeDb} (interfaz/stub) inyectada; el adaptador únicamente
 * envuelve cada fila cruda en un {@link KnowledgeCandidate}. Nunca valida, nunca
 * decide ni interpreta negocio (offline-first: ante error o nulo degrada a
 * lista vacía).</p>
 */
public class JdbcKnowledgeSource implements KnowledgeSource {

    /** Acceso a la base de datos de conocimiento (stub; sin JDBC funcional). */
    public interface KnowledgeDb {
        List<Row> query(String queryText, List<String> params);
    }

    /** Fila cruda devuelta por la base de datos de conocimiento. */
    public record Row(String content, String url, OffsetDateTime publishedAt) {

        public Row {
            content = content == null ? "" : content;
            url = url == null ? "" : url;
        }
    }

    private final String sourceId;
    private final String sourceName;
    private final String queryText;
    private final KnowledgeDb db;

    public JdbcKnowledgeSource(String sourceId, String sourceName, String queryText, KnowledgeDb db) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.queryText = queryText == null ? "" : queryText;
        this.db = db;
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null) {
            return List.of();
        }
        try {
            List<Row> rows = db.query(queryText, List.of(query.topic(), String.join(" ", query.keywords())));
            if (rows == null) {
                return List.of();
            }
            var candidates = new ArrayList<KnowledgeCandidate>();
            for (var row : rows) {
                if (row == null) {
                    continue;
                }
                candidates.add(new KnowledgeCandidate(
                    row.content(), sourceId, sourceName, row.url(), row.publishedAt(),
                    "application/json", Map.of()));
            }
            return List.copyOf(candidates);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }
}
