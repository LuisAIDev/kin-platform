package com.kinplatform.kin.health.triage.port;

import com.kinplatform.kin.health.triage.domain.CatalogUpdate;
import com.kinplatform.kin.health.triage.domain.CatalogUpdateResult;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;

/**
 * Puerto de conocimiento del triaje (ADR-028).
 *
 * <p>Expone el catálogo completo (síntomas, condiciones y relaciones) al motor
 * de triaje y permite la actualización masiva desde fuentes externas
 * (ADR-028, fase profesional). El dominio define el puerto; la infraestructura
 * lo implementa con JPA/BD (opción A) o un archivo de configuración (opción B
 * futura).</p>
 */
public interface TriageKnowledgeRepository {

    /**
     * Carga el snapshot inmutable del catálogo de conocimiento de triaje.
     */
    TriageCatalog loadCatalog();

    /**
     * Aplica una actualización masiva del catálogo (upsert idempotente) y
     * devuelve el conteo de elementos añadidos/actualizados.
     *
     * <p>Debe ser seguro re-aplicarla: los elementos existentes se actualizan
     * (nunca se duplican). Una actualización vacía o fallida de la fuente externa
     * no debe romper el catálogo local (degradación elegante).</p>
     */
    CatalogUpdateResult applyUpdate(CatalogUpdate update);
}
