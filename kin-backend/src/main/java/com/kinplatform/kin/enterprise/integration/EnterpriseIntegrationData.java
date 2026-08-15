package com.kinplatform.kin.enterprise.integration;

import java.util.List;

/**
 * Resultado de la integración Enterprise: vista de contexto resuelta, datos
 * suplementarios y documentos del proyecto.
 */
public record EnterpriseIntegrationData(
        ResolvedContext resolvedContext, SupplementalData supplementalData, List<DocumentDescriptor> documents) {

    public EnterpriseIntegrationData {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }
}
