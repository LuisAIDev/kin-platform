package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de infraestructura que compone varios {@link KnowledgeSource}
 * (ADR-014, §6.3): implementa el puerto {@link KnowledgeSource} consultando cada
 * fuente en orden y concatenando sus candidatos.
 *
 * <p>Estructura preparada: no decide ni interpreta negocio. Ignora fuentes
 * nulas y resultados nulos, preserva el orden de registro (comportamiento
 * determinista) y nunca ordena ni filtra los candidatos (eso vive en el
 * dominio, en {@code SourceValidator}).</p>
 */
public class CompositeKnowledgeSource implements KnowledgeSource {

    private final List<KnowledgeSource> sources;

    public CompositeKnowledgeSource(List<KnowledgeSource> sources) {
        var safe = new ArrayList<KnowledgeSource>();
        if (sources != null) {
            for (var source : sources) {
                if (source != null) {
                    safe.add(source);
                }
            }
        }
        this.sources = List.copyOf(safe);
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        if (query == null) {
            return List.of();
        }
        var candidates = new ArrayList<KnowledgeCandidate>();
        for (var source : sources) {
            var fetched = source.fetch(query);
            if (fetched != null) {
                candidates.addAll(fetched);
            }
        }
        return List.copyOf(candidates);
    }
}
