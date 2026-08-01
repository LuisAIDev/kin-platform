package com.kinplatform.kin.knowledge;

import java.time.Duration;
import java.util.Optional;

/**
 * Puerto de caché/persistencia de conocimiento verificado (ADR-014).
 *
 * <p>Permite reutilizar hechos frescos sin repetir llamadas a fuentes externas
 * en cada turno. El adaptador vive en infraestructura (nunca en el dominio) y
 * puede ser en memoria o persistente; {@code ttl} define la frescura máxima.</p>
 */
public interface KnowledgeRepository {

    Optional<KnowledgeResult> find(KnowledgeQuery query);

    void save(KnowledgeResult result, Duration ttl);
}
