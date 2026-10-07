package com.kinplatform.kin.medical.billing.authorization;

import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final AuthorizationRepository repository;
    private final MipresClient mipresClient;

    public Page<Authorization> findAll(Pageable pageable) {
        return repository.findByOrganizationId(TenantContext.get(), pageable);
    }

    public List<Authorization> findByContract(UUID contractId) {
        return repository.findByContractIdAndStatus(contractId, Authorization.AuthorizationStatus.APPROVED);
    }

    public Authorization findById(UUID id) {
        return repository.findByIdAndOrganizationId(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Authorization not found: " + id));
    }

    @Transactional
    public Authorization create(CreateAuthorizationRequest request) {
        validateCreate(request);

        Authorization auth = Authorization.builder()
                .organizationId(TenantContext.get())
                .contractId(request.contractId())
                .patientId(request.patientId())
                .authorizationNumber(request.authorizationNumber().trim().toUpperCase())
                .authorizationType(Authorization.AuthorizationType.valueOf(request.authorizationType()))
                .status(Authorization.AuthorizationStatus.PENDING)
                .cupsCodes(request.cupsCodes())
                .diagnosisCie10(request.diagnosisCie10())
                .requestedDate(request.requestedDate())
                .approvedValueCop(request.approvedValueCop())
                .usedValueCop(BigDecimal.ZERO)
                .notes(request.notes())
                .build();

        if (request.authorizationType().equals("MIPRES")) {
            var mipresData = mipresClient.consultarAutorizacion(request.authorizationNumber(), request.contractId());
            if (mipresData.isEmpty()) {
                throw new IllegalArgumentException("Autorización no encontrada en MIPRES: " + request.authorizationNumber());
            }
            auth.setStatus(Authorization.AuthorizationStatus.APPROVED);
            auth.setApprovedDate(OffsetDateTime.now());
        }

        return repository.save(auth);
    }

    @Transactional
    public Authorization approve(UUID id, ApproveAuthorizationRequest request) {
        Authorization auth = findById(id);
        auth.setStatus(Authorization.AuthorizationStatus.APPROVED);
        auth.setApprovedDate(OffsetDateTime.now());
        auth.setExpiryDate(request.expiryDate());
        auth.setApprovedValueCop(request.approvedValueCop());
        return repository.save(auth);
    }

    @Transactional
    public Authorization consume(UUID authId, String cupsCode, int quantity, BigDecimal unitValue) {
        Authorization auth = findById(authId);

        if (auth.getStatus() != Authorization.AuthorizationStatus.APPROVED
                && auth.getStatus() != Authorization.AuthorizationStatus.PARTIAL) {
            throw new IllegalStateException("Autorización no está en estado válido para consumo: " + auth.getStatus());
        }

        if (auth.getExpiryDate() != null && auth.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Autorización vencida");
        }

        if (auth.getAuthorizationType() == Authorization.AuthorizationType.MIPRES) {
            var result = mipresClient.reportarUso(auth.getAuthorizationNumber(), auth.getContractId(), cupsCode, quantity, unitValue);
            if (!result.success()) {
                throw new IllegalStateException("MIPRES rechazó consumo: " + result.errorMessage());
            }
        }

        BigDecimal consumed = unitValue.multiply(BigDecimal.valueOf(quantity));
        auth.setUsedValueCop(auth.getUsedValueCop().add(consumed));

        if (auth.getApprovedValueCop() != null
                && auth.getUsedValueCop().compareTo(auth.getApprovedValueCop()) >= 0) {
            auth.setStatus(Authorization.AuthorizationStatus.USED);
        } else {
            auth.setStatus(Authorization.AuthorizationStatus.PARTIAL);
        }

        return repository.save(auth);
    }

    @Transactional(readOnly = true)
    public List<Authorization> findExpiringSoon(Duration window) {
        OffsetDateTime threshold = OffsetDateTime.now().plus(window);
        return repository.findExpiringSoon(TenantContext.get(), threshold);
    }

    @Transactional(readOnly = true)
    public List<Authorization> findValidForEncounter(UUID patientId, UUID contractId) {
        return repository.findValidForPatient(contractId, patientId);
    }

    @Transactional(readOnly = true)
    public boolean validateForEncounter(UUID patientId, String cupsCode, UUID contractId) {
        List<Authorization> validAuths = findValidForEncounter(patientId, contractId);
        return validAuths.stream().anyMatch(auth ->
                auth.getCupsCodes().contains("\"code\":\"" + cupsCode + "\"")
        );
    }

    private void validateCreate(CreateAuthorizationRequest request) {
        if (repository.existsByContractIdAndAuthorizationNumber(request.contractId(), request.authorizationNumber().trim().toUpperCase())) {
            throw new IllegalArgumentException("Número de autorización duplicado en contrato: " + request.authorizationNumber());
        }
    }

    public record CreateAuthorizationRequest(
        UUID contractId,
        UUID patientId,
        String authorizationNumber,
        String authorizationType,
        String cupsCodes,
        String diagnosisCie10,
        OffsetDateTime requestedDate,
        BigDecimal approvedValueCop,
        String notes
    ) {}

    public record ApproveAuthorizationRequest(
        OffsetDateTime expiryDate,
        BigDecimal approvedValueCop
    ) {}
}
