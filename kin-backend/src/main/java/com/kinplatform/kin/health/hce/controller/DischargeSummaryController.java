package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.common.user.UserRepository;
import com.kinplatform.kin.health.hce.dto.CreateDischargeSummaryRequest;
import com.kinplatform.kin.health.hce.dto.DischargeSummaryResponse;
import com.kinplatform.kin.health.hce.service.DischargeSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/health/hce", "/medical/hce"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Discharge Summaries", description = "Resúmenes de alta / Epicrisis (HCE Res 839/1995)")
public class DischargeSummaryController {

    private final DischargeSummaryService dischargeSummaryService;
    private final UserRepository userRepository;

    @PostMapping("/encounters/{encounterId}/discharge")
    @Operation(
            summary = "Crear resumen de alta / Epicrisis",
            description = "Crea un resumen de alta / epicrisis asociado a un encuentro")
    public ResponseEntity<DischargeSummaryResponse> create(
            @PathVariable UUID encounterId, @Valid @RequestBody CreateDischargeSummaryRequest request) {
        request.setEncounterId(encounterId);
        DischargeSummaryResponse response = dischargeSummaryService.createDischargeSummary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/encounters/{encounterId}/discharge")
    @Operation(
            summary = "Obtener resumen de alta por encuentro",
            description = "Retorna el resumen de alta asociado al encuentro")
    public ResponseEntity<DischargeSummaryResponse> getByEncounter(@PathVariable UUID encounterId) {
        List<DischargeSummaryResponse> summaries = dischargeSummaryService.getByEncounter(encounterId);
        if (summaries.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summaries.get(0));
    }

    @PutMapping("/discharge-summaries/{summaryId}/sign")
    @Operation(summary = "Firmar resumen de alta", description = "Firma el resumen de alta por el médico tratante")
    public ResponseEntity<DischargeSummaryResponse> sign(Authentication authentication, @PathVariable UUID summaryId) {
        UUID physicianId = com.kinplatform.common.security.AuthenticatedUsers.require(userRepository, authentication)
                .getId();
        try {
            DischargeSummaryResponse response = dischargeSummaryService.signDischargeSummary(summaryId, physicianId);
            return ResponseEntity.ok(response);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/admissions/{admissionId}/discharge")
    @Operation(
            summary = "Obtener resumen de alta por admisión",
            description = "Retorna el resumen de alta asociado a la admisión")
    public ResponseEntity<DischargeSummaryResponse> getByAdmission(@PathVariable UUID admissionId) {
        Optional<DischargeSummaryResponse> summary = dischargeSummaryService.getByAdmission(admissionId);
        return summary.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
