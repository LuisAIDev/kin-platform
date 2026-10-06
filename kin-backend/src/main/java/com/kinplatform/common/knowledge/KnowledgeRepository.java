package com.kinplatform.common.knowledge;

import java.time.Duration;
import java.util.Optional;

/**
 * Puerto de caché/persistencia de conocimiento verificado (ADR-014, extendido
 * aditivamente por ADR-021).
 *
 * <p>Permite reutilizar hechos frescos sin repetir llamadas a fuentes externas
 * en cada turno. El adaptador vive en infraestructura (nunca en el dominio) y
 * puede ser en memoria o persistente; {@code ttl} define la frescura máxima.</p>
 *
 * <p><strong>ADR-021 (aditivo):</strong> el contrato congelado de ADR-014
 * {@code save(KnowledgeResult, Duration)} no recibe la consulta que originó el
 * resultado, por lo que un adaptador de caché no puede derivar la misma clave
 * usada en {@link #find(KnowledgeQuery)} (hit/miss cruzado). Se añade el método
 * <em>default</em> {@link #save(KnowledgeQuery, KnowledgeResult, Duration)} que
 * transporta la {@link KnowledgeQuery}; su implementación por defecto delega en
 * {@link #save(KnowledgeResult, Duration)}, por lo que los adaptadores existentes
 * no cambian su comportamiento. El dominio sigue siendo 100 % POJO.</p>
 */
public interface KnowledgeRepository {

    Optional<KnowledgeResult> find(KnowledgeQuery query);

    void save(KnowledgeResult result, Duration ttl);

    /**
     * Guarda un resultado junto a la consulta que lo originó (ADR-021). Permite
     * a los adaptadores derivar una clave determinista por consulta y alinear el
     * hit/miss con {@link #find(KnowledgeQuery)}.
     *
     * @param query  consulta que originó el resultado (nunca {@code null} en el
     *               flujo del {@code KnowledgeOrchestrator})
     * @param result resultado validado a cachear
     * @param ttl    frescura máxima
     */
    default void save(KnowledgeQuery query, KnowledgeResult result, Duration ttl) {
        save(result, ttl);
    }
}

