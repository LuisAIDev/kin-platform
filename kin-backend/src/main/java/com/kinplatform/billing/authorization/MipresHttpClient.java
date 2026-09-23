package com.kinplatform.billing.authorization;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacion productiva del cliente MIPRES.
 *
 * PENDIENTE DE IMPLEMENTACION: requiere convenio habilitado, certificado y
 * endpoint del Ministerio de Salud. No incluir credenciales en el repo.
 *
 * Provee el bean requerido por AuthorizationService para que el perfil "prod"
 * arranque. Si se invoca antes de la integracion real, falla de forma explicita.
 */
@Component
@Profile("prod")
public class MipresHttpClient implements MipresClient {

    private static final String NOT_IMPLEMENTED =
            "Integracion MIPRES productiva no implementada. Configurar cliente real.";

    @Override
    public Optional<AuthorizationData> consultarAutorizacion(String authorizationNumber, UUID contractId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public ConsumptionResult reportarUso(String authorizationNumber, UUID contractId,
                                         String cupsCode, int quantity, BigDecimal value) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public List<String> listarAutorizacionesVigentes(UUID contractId, UUID patientId) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
