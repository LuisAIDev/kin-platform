package com.kinplatform.kin.health.differential.port;

import com.kinplatform.kin.health.differential.domain.CatalogUpdate;
import com.kinplatform.kin.health.differential.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.differential.domain.DifferentialCatalog;

/**
 * Puerto de conocimiento del diagnóstico diferencial (ADR-029).
 *
 * <p>Expone el catálogo de factores de riesgo y pruebas recomendadas al motor y
 * permite la actualización masiva desde fuentes externas (KnowledgeEngine).
 * El dominio define el puerto; la infraestructura lo implementa con JPA/BD.</p>
 */
public interface DifferentialKnowledgeRepository {

    /**
     * Carga el snapshot inmutable del catálogo de conocimiento diferencial.
     */
    DifferentialCatalog loadCatalog();

    /**
     * Aplica una actualización masiva (upsert idempotente) y devuelve el conteo.
     * Una fuente fallida no debe romper el catálogo local (degradación elegante).
     */
    CatalogUpdateResult applyUpdate(CatalogUpdate update);
}
