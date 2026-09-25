package com.kinplatform.billing.authorization;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.security.TenantContext;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MipresService {

    private final MipresClient mipresClient;
    private final MipresPrescriptionRepository prescriptionRepository;
    private final MipresSupplyRepository supplyRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final com.kinplatform.billing.authorization.MipresAuthorizationService authService;

    /**
     * Crea una nueva prescripción MIPRES.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public MipresPrescription createPrescription(Authentication auth, CreatePrescriptionRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, auth).getId();
        UUID orgId = TenantContext.get();

        // Validar autorización en MIPRES
        Optional<MipresClient.AuthorizationData> authData = mipresClient.consultarAutorizacion(
                request.authorizationNumber(), request.contractId());

        if (authData.isEmpty()) {
            throw new IllegalArgumentException("Autorización MIPRES no encontrada: " + request.authorizationNumber());
        }

        MipresPrescription prescription = MipresPrescription.builder()
                .organizationId(orgId)
                .contractId(request.contractId())
                .patientId(request.patientId())
                .prescriptionNumber(authData.get().authorizationNumber())
                .nit(authService.getOrganizationNit(orgId))
                .prescriptionDate(LocalDate.now())
                .status(MipresPrescription.PrescriptionStatus.AUTHORIZED)
                .cupsCode(authData.get().cupsCode())
                .diagnosisCie10(authData.get().diagnosisCie10())
                .qtyApproved(authData.get().qtyApproved())
                .unitPriceCop(authData.get().unitPriceCop())
                .build();

        prescription = prescriptionRepository.save(prescription);

        auditService.logAccess(userId, AuditAction.MIPRES_PRESCRIPTION_CREATE,
                AuditResourceType.MIPRES_PRESCRIPTION, prescription.getId(), orgId,
                Map.of("authorizationNumber", request.authorizationNumber(),
                        "cupsCode", prescription.getCupsCode()));

        log.info("MipresService: Prescripción MIPRES creada: {} para org {}", prescription.getId(), orgId);
        return prescription;
    }

    /**
     * Reporta un suministro a MIPRES.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public MipresSupply reportSupply(Authentication auth, ReportSupplyRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, auth).getId();
        UUID orgId = TenantContext.get();

        // Verificar que la prescripción existe y pertenece a la org
        MipresPrescription prescription = prescriptionRepository.findByPrescriptionNumber(request.authorizationNumber())
                .orElseThrow(() -> new IllegalArgumentException("Prescripción no encontrada: " + request.authorizationNumber()));

        if (!prescription.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("La prescripción no pertenece a su organización");
        }

        // Reportar a MIPRES
        MipresClient.ConsumptionResult result = mipresClient.reportarUso(
                request.authorizationNumber(),
                prescription.getContractId(),
                request.cupsCode(),
                request.quantity(),
                request.value());

        if (!result.success()) {
            throw new IllegalStateException("Error reportando uso a MIPRES: " + result.errorMessage());
        }

        // Generar supply_id fake para stub, real vendría de MIPRES
        String supplyId = "SUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MipresSupply supply = MipresSupply.builder()
                .prescriptionId(prescription.getId())
                .organizationId(orgId)
                .supplyId(supplyId)
                .prescriptionNumber(request.authorizationNumber())
                .supplyDate(LocalDate.now())
                .cupsCode(request.cupsCode())
                .quantity(request.quantity())
                .unitValueCop(request.unitValueCop())
                .totalValueCop(request.value().multiply(BigDecimal.valueOf(request.quantity())))
                .batchNumber(request.batchNumber())
                .expirationDate(request.expirationDate())
                .status(MipresSupply.SupplyStatus.REPORTED)
                .rawRequest(Map.of(
                        "authorizationNumber", request.authorizationNumber(),
                        "cupsCode", request.cupsCode(),
                        "quantity", request.quantity(),
                        "value", request.value().toString()))
                .rawResponse(Map.of(
                        "success", true,
                        "supplyId", supplyId))
                .build();

        supply = supplyRepository.save(supply);

        // Actualizar estado de prescripción si se consumió todo
        if (prescription.getStatus() == MipresPrescription.PrescriptionStatus.AUTHORIZED) {
            prescriptionRepository.save(prescription.withStatus(MipresPrescription.PrescriptionStatus.CONSUMED));
        }

        auditService.logAccess(userId, AuditAction.MIPRES_SUPPLY_REPORT,
                AuditResourceType.MIPRES_SUPPLY, supply.getId(), orgId,
                Map.of("authorizationNumber", request.authorizationNumber(),
                        "cupsCode", request.cupsCode(),
                        "quantity", request.quantity(),
                        "supplyId", supplyId));

        log.info("MipresService: Suministro MIPRES reportado: {} para auth {}", supply.getId(), request.authorizationNumber());
        return supply;
    }

    /**
     * Anula un suministro reportado.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public MipresSupply anularSupply(Authentication auth, UUID supplyId, String motivo) {
        UUID userId = AuthenticatedUsers.require(userRepository, auth).getId();
        UUID orgId = TenantContext.get();

        MipresSupply supply = supplyRepository.findById(supplyId)
                .orElseThrow(() -> new IllegalArgumentException("Suministro no encontrado: " + supplyId));

        if (!supply.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("El suministro no pertenece a su organización");
        }

        if (supply.getStatus() == MipresSupply.SupplyStatus.ANULLED) {
            throw new IllegalStateException("El suministro ya está anulado");
        }

        // TODO: Llamar a endpoint MIPRES de anulación cuando esté implementado
        // mipresClient.anularSuministro(supply.getSupplyId(), motivo);

        supply = supplyRepository.save(supply.withStatus(MipresSupply.SupplyStatus.ANULLED));

        auditService.logAccess(userId, AuditAction.MIPRES_SUPPLY_ANULLED,
                AuditResourceType.MIPRES_SUPPLY, supply.getId(), orgId,
                Map.of("motivo", motivo));

        log.info("MipresService: Suministro MIPRES anulado: {} motivo: {}", supplyId, motivo);
        return supply;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public Optional<MipresPrescription> getPrescription(UUID id) {
        UUID orgId = TenantContext.get();
        return prescriptionRepository.findById(id)
                .filter(p -> p.getOrganizationId().equals(orgId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public List<MipresPrescription> getPrescriptions(UUID contractId, UUID patientId, MipresPrescription.PrescriptionStatus status) {
        UUID orgId = TenantContext.get();

        if (contractId != null) {
            return prescriptionRepository.findByOrganizationIdAndStatus(orgId, status != null ? status : MipresPrescription.PrescriptionStatus.AUTHORIZED);
        }
        if (patientId != null) {
            return prescriptionRepository.findByPatientId(patientId);
        }
        return prescriptionRepository.findByOrganizationId(orgId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public List<MipresSupply> getSupplies(UUID contractId, UUID prescriptionId, LocalDate startDate, LocalDate endDate) {
        UUID orgId = TenantContext.get();

        if (prescriptionId != null) {
            return supplyRepository.findByPrescriptionId(prescriptionId);
        }
        if (startDate != null && endDate != null) {
            return supplyRepository.findByOrganizationIdAndDateRange(orgId, startDate, endDate);
        }
        return supplyRepository.findByOrganizationId(orgId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public Optional<MipresSupply> getSupply(UUID id) {
        UUID orgId = TenantContext.get();
        return supplyRepository.findById(id)
                .filter(s -> s.getOrganizationId().equals(orgId));
    }

    // DTOs
    public record CreatePrescriptionRequest(
            String authorizationNumber,
            UUID contractId,
            UUID patientId
    ) {}

    public record ReportSupplyRequest(
            String authorizationNumber,
            String cupsCode,
            int quantity,
            BigDecimal value,
            BigDecimal unitValueCop,
            String batchNumber,
            LocalDate expirationDate
    ) {}

    // Helper para update de prescripción
    @Transactional
    public void updatePrescriptionStatus(UUID prescriptionId, MipresPrescription.PrescriptionStatus status) {
        prescriptionRepository.findById(prescriptionId).ifPresent(p -> {
            prescriptionRepository.save(p.withStatus(status));
        });
    }
}