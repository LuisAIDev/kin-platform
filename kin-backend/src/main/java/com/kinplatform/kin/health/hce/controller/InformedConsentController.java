package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.CreateInformedConsentRequest;
import com.kinplatform.kin.health.hce.dto.InformedConsentResponse;
import com.kinplatform.kin.health.hce.dto.RevokeConsentRequest;
import com.kinplatform.kin.health.hce.service.InformedConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/hce")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Informed Consents", description = "Consentimientos informados (HCE Res 839/1995)")
public class InformedConsentController {

    private final InformedConsentService informedConsentService;

    @PostMapping("/encounters/{encounterId}/consents")
    @Operation(summary = "Crear consentimiento informado en un encuentro")
    public ResponseEntity<InformedConsentResponse> create(
            @PathVariable UUID encounterId,
            @Valid @RequestBody CreateInformedConsentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(informedConsentService.createConsentForEncounter(encounterId, request));
    }

    @PutMapping("/consents/{consentId}/revoke")
    @Operation(summary = "Revocar consentimiento informado")
    public ResponseEntity<InformedConsentResponse> revoke(
            @PathVariable UUID consentId,
            @Valid @RequestBody RevokeConsentRequest request) {
        return ResponseEntity.ok(informedConsentService.revokeConsent(consentId, request.getReason()));
    }

    @GetMapping("/patients/{patientId}/consents")
    @Operation(summary = "Listar consentimientos por paciente")
    public ResponseEntity<List<InformedConsentResponse>> getByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(informedConsentService.getByPatient(patientId));
    }
}
