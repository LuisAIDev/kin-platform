package com.kinplatform.ai.knowledge.adapter;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeQuery;
import com.kinplatform.kin.knowledge.KnowledgeSource;

import java.util.List;

/**
 * Conector de fuentes públicas oficiales (ADR-014, §6.3, OCP-compliant):
 * implementa el puerto {@link KnowledgeSource} agrupando adaptadores por fuente
 * oficial (estadísticas, marcos regulatorios, referencias sectoriales).
 *
 * <p>Estructura preparada: no consume APIs públicas reales. Es una composición
 * sobre {@link CompositeKnowledgeSource}: agregar una fuente nueva = agregarla a
 * la lista (OCP), el conector no cambia. No decide ni interpreta negocio; solo
 * propaga la consulta y concatena candidatos en orden determinista.</p>
 */
public class PublicApiConnector implements KnowledgeSource {

    private final String name;
    private final CompositeKnowledgeSource composite;

    public PublicApiConnector(List<KnowledgeSource> sources) {
        this("PublicApiConnector", sources);
    }

    public PublicApiConnector(String name, List<KnowledgeSource> sources) {
        this.name = name == null ? "" : name;
        this.composite = new CompositeKnowledgeSource(sources);
    }

    public String name() {
        return name;
    }

    @Override
    public List<KnowledgeCandidate> fetch(KnowledgeQuery query) {
        return composite.fetch(query);
    }
}
