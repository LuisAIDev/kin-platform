package com.kinplatform.kin.medical.billing.contract;

import com.kinplatform.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractValidator {

    private final EpsContractRepository contractRepository;

    public void validateCreate(ContractService.CreateContractRequest request) {
        if (request.epsNit() == null || request.epsNit().trim().isEmpty()) {
            throw new IllegalArgumentException("NIT EPS es obligatorio");
        }
        if (request.epsName() == null || request.epsName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre EPS es obligatorio");
        }
        if (request.regimen() == null) {
            throw new IllegalArgumentException("Régimen es obligatorio");
        }
        if (request.startDate() == null) {
            throw new IllegalArgumentException("Fecha inicio es obligatoria");
        }
        if (request.paymentTermsDays() != null && request.paymentTermsDays() < 0) {
            throw new IllegalArgumentException("Días de pago no puede ser negativo");
        }

        if (contractRepository.existsByOrganizationIdAndEpsNit(
                TenantContext.get(), request.epsNit().trim().toUpperCase())) {
            throw new IllegalArgumentException("Ya existe un contrato con este NIT para la organización");
        }
    }

    public void validateUpdate(EpsContract contract, ContractService.UpdateContractRequest request) {
        if (request.paymentTermsDays() != null && request.paymentTermsDays() < 0) {
            throw new IllegalArgumentException("Días de pago no puede ser negativo");
        }
        if (request.endDate() != null
                && request.startDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("Fecha fin no puede ser anterior a fecha inicio");
        }
        if (request.endDate() != null
                && contract.getStartDate() != null
                && request.endDate().isBefore(contract.getStartDate())) {
            throw new IllegalArgumentException("Fecha fin no puede ser anterior a fecha inicio");
        }
    }
}
