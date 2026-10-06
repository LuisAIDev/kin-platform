package com.kinplatform.kin.enterprise.integration;

import com.kinplatform.common.context.ProjectContext;
import java.util.UUID;

/**
 * Proveedor de datos de integración Enterprise: combina
 * {@code project_structured_info} y {@code project_documents} con el
 * {@code ProjectContext} (sin mutarlo) para alimentar la vista y el análisis
 * Enterprise. El adaptador de infraestructura resuelve la persistencia.
 */
public interface EnterpriseDataProvider {

    EnterpriseIntegrationData load(UUID projectId, ProjectContext context);
}

