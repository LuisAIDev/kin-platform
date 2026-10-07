package com.kinplatform.kin.medical.billing.authorization;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MipresClient {

    record AuthorizationData(
        String authorizationNumber,
        String cupsCode,
        int qtyApproved,
        BigDecimal unitPriceCop,
        String diagnosisCie10
    ) {}

    record ConsumptionResult(
        boolean success,
        String errorMessage
    ) {}

    Optional<AuthorizationData> consultarAutorizacion(String authorizationNumber, UUID contractId);

    ConsumptionResult reportarUso(String authorizationNumber, UUID contractId, String cupsCode, int quantity, BigDecimal value);

    List<String> listarAutorizacionesVigentes(UUID contractId, UUID patientId);
}
