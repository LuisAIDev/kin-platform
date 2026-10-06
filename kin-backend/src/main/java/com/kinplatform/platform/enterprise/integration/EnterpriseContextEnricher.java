package com.kinplatform.platform.enterprise.integration;

import com.kinplatform.common.context.ProjectContext;
import java.util.UUID;

/**
 * Enriquece el {@code ProjectContext} de un proyecto con la información
 * estructurada disponible (sin mutar el contexto persistido ni el contrato del
 * pipeline).
 *
 * <p>Devuelve una NUEVA instancia de {@code ProjectContext} que rellena las
 * dimensiones pendientes con los valores resueltos de
 * {@code project_structured_info}; las dimensiones ya confirmadas no se
 * sobrescriben. Si no hay información adicional, devuelve el contexto original
 * sin cambios.</p>
 */
public interface EnterpriseContextEnricher {

    ProjectContext enrich(UUID projectId, ProjectContext context);
}


