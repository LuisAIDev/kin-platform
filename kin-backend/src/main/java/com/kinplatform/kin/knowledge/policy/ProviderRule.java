package com.kinplatform.kin.knowledge.policy;

/**
 * Contrato tipado de una regla de política de proveedores (Strategy Pattern):
 * evalúa un tipo de fuente abstracto (cadena de dominio) contra la
 * configuración. Nunca conoce adaptadores ni infraestructura.
 */
public interface ProviderRule extends PolicyRule {

    @Override
    default PolicyCategory category() {
        return PolicyCategory.PROVIDER;
    }

    PolicyDecision evaluate(String sourceType, ProviderPolicyConfig config);
}
