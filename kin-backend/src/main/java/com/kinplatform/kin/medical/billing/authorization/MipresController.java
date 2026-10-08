package com.kinplatform.kin.medical.billing.authorization;

import com.kinplatform.common.security.TenantContext;
import com.kinplatform.common.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing/mipres")
@RequiredArgsConstructor
@Slf4j
public class MipresController {

    private final MipresService mipresService;
    private final UserRepository userRepository;

    // ========== PRESCRIPCIONES ==========

    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public ResponseEntity<MipresPrescriptionResponse> createPrescription(
            Authentication auth, @Valid @RequestBody CreatePrescriptionRequest request) {

        UUID orgId = TenantContext.get();
        var prescription = mipresService.createPrescription(
                auth,
                new MipresService.CreatePrescriptionRequest(
                        request.authorizationNumber(), request.contractId(), request.patientId()));

        return ResponseEntity.status(201).body(MipresPrescriptionResponse.from(prescription));
    }

    @GetMapping("/prescriptions/{id}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public ResponseEntity<MipresPrescriptionResponse> getPrescription(Authentication auth, @PathVariable UUID id) {

        return mipresService
                .getPrescription(id)
                .map(p -> ResponseEntity.ok(MipresPrescriptionResponse.from(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public ResponseEntity<List<MipresPrescriptionResponse>> listPrescriptions(
            Authentication auth,
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) MipresPrescription.PrescriptionStatus status) {

        var prescriptions = mipresService.getPrescriptions(contractId, patientId, status);
        var response =
                prescriptions.stream().map(MipresPrescriptionResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    // ========== SUMINISTROS ==========

    @PostMapping("/supplies")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<MipresSupplyResponse> reportSupply(
            Authentication auth, @Valid @RequestBody ReportSupplyRequest request) {

        var supply = mipresService.reportSupply(
                auth,
                new MipresService.ReportSupplyRequest(
                        request.authorizationNumber(),
                        request.cupsCode(),
                        request.quantity(),
                        request.value(),
                        request.unitValueCop(),
                        request.batchNumber(),
                        request.expirationDate()));

        return ResponseEntity.status(201).body(MipresSupplyResponse.from(supply));
    }

    @PutMapping("/supplies/{id}/anular")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<MipresSupplyResponse> anularSupply(
            Authentication auth, @PathVariable UUID id, @RequestParam @Size(max = 500) String motivo) {

        var supply = mipresService.anularSupply(auth, id, motivo);
        return ResponseEntity.ok(MipresSupplyResponse.from(supply));
    }

    @GetMapping("/supplies")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public ResponseEntity<List<MipresSupplyResponse>> listSupplies(
            Authentication auth,
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) UUID prescriptionId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        var supplies = mipresService.getSupplies(contractId, prescriptionId, startDate, endDate);
        var response = supplies.stream().map(MipresSupplyResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/supplies/{id}")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")
    public ResponseEntity<MipresSupplyResponse> getSupply(Authentication auth, @PathVariable UUID id) {

        return mipresService
                .getSupply(id)
                .map(s -> ResponseEntity.ok(MipresSupplyResponse.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== REPORTES ==========

    @GetMapping("/reports/consolidated")
    @PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getConsolidatedReport(
            Authentication auth,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        UUID orgId = TenantContext.get();
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        var supplies = mipresService.getSupplies(null, null, start, end);

        long totalSupplies = supplies.size();
        long totalQuantity =
                supplies.stream().mapToLong(MipresSupply::getQuantity).sum();
        BigDecimal totalValue =
                supplies.stream().map(MipresSupply::getTotalValueCop).reduce(BigDecimal.ZERO, BigDecimal::add);

        var byStatus = supplies.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        MipresSupply::getStatus, java.util.stream.Collectors.counting()));

        var byCups = supplies.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        MipresSupply::getCupsCode, java.util.stream.Collectors.counting()));

        return ResponseEntity.ok(Map.of(
                "periodStart", start,
                "periodEnd", end,
                "totalSupplies", totalSupplies,
                "totalQuantity", totalQuantity,
                "totalValueCop", totalValue,
                "byStatus", byStatus,
                "byCups", byCups));
    }

    // ========== DTOs Response ==========

    public record MipresPrescriptionResponse(
            UUID id,
            UUID organizationId,
            UUID contractId,
            UUID patientId,
            String prescriptionNumber,
            String nit,
            LocalDate prescriptionDate,
            MipresPrescription.PrescriptionStatus status,
            String cupsCode,
            String diagnosisCie10,
            Integer qtyApproved,
            BigDecimal unitPriceCop,
            OffsetDateTime createdAt) {
        static MipresPrescriptionResponse from(MipresPrescription p) {
            return new MipresPrescriptionResponse(
                    p.getId(),
                    p.getOrganizationId(),
                    p.getContractId(),
                    p.getPatientId(),
                    p.getPrescriptionNumber(),
                    p.getNit(),
                    p.getPrescriptionDate(),
                    p.getStatus(),
                    p.getCupsCode(),
                    p.getDiagnosisCie10(),
                    p.getQtyApproved(),
                    p.getUnitPriceCop(),
                    p.getCreatedAt());
        }
    }

    public record MipresSupplyResponse(
            UUID id,
            UUID prescriptionId,
            UUID organizationId,
            String supplyId,
            String prescriptionNumber,
            LocalDate supplyDate,
            String cupsCode,
            Integer quantity,
            BigDecimal unitValueCop,
            BigDecimal totalValueCop,
            String batchNumber,
            LocalDate expirationDate,
            MipresSupply.SupplyStatus status,
            OffsetDateTime createdAt) {
        static MipresSupplyResponse from(MipresSupply s) {
            return new MipresSupplyResponse(
                    s.getId(),
                    s.getPrescriptionId(),
                    s.getOrganizationId(),
                    s.getSupplyId(),
                    s.getPrescriptionNumber(),
                    s.getSupplyDate(),
                    s.getCupsCode(),
                    s.getQuantity(),
                    s.getUnitValueCop(),
                    s.getTotalValueCop(),
                    s.getBatchNumber(),
                    s.getExpirationDate(),
                    s.getStatus(),
                    s.getCreatedAt());
        }
    }

    // ========== DTOs Request ==========

    public record CreatePrescriptionRequest(
            @NotNull @Size(max = 20) String authorizationNumber, @NotNull UUID contractId, @NotNull UUID patientId) {}

    public record ReportSupplyRequest(
            @NotNull @Size(max = 20) String authorizationNumber,
            @NotNull @Size(max = 20) String cupsCode,
            @NotNull int quantity,
            @NotNull BigDecimal value,
            @NotNull BigDecimal unitValueCop,
            @Size(max = 50) String batchNumber,
            LocalDate expirationDate) {}
}
