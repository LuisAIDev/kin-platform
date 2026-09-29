package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.CounterReferralRequest;
import com.kinplatform.kin.health.hce.dto.CreateReferralRequest;
import com.kinplatform.kin.health.hce.dto.ReferralResponse;
import com.kinplatform.kin.health.hce.entity.Referral;
import com.kinplatform.kin.health.hce.service.ReferralService;
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
@RequestMapping({"/health/hce", "/medical/hce"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Referrals", description = "Referencias y contrarreferencias (HCE Res 839/1995)")
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping("/encounters/{encounterId}/referrals")
    @Operation(summary = "Crear referencia en un encuentro")
    public ResponseEntity<ReferralResponse> create(
            @PathVariable UUID encounterId,
            @Valid @RequestBody CreateReferralRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(referralService.createReferralForEncounter(encounterId, request));
    }

    @PutMapping("/referrals/{referralId}/counter-referral")
    @Operation(summary = "Registrar contrarreferencia")
    public ResponseEntity<ReferralResponse> counterReferral(
            @PathVariable UUID referralId,
            @Valid @RequestBody CounterReferralRequest request) {
        return ResponseEntity.ok(referralService.counterReferral(referralId, request));
    }

    @GetMapping("/patients/{patientId}/referrals")
    @Operation(summary = "Listar referencias por paciente")
    public ResponseEntity<List<ReferralResponse>> getByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(referralService.getByPatient(patientId));
    }

    @GetMapping("/referrals")
    @Operation(summary = "Listar referencias por estado")
    public ResponseEntity<List<ReferralResponse>> getByStatus(@RequestParam Referral.Status status) {
        return ResponseEntity.ok(referralService.getByStatus(status));
    }
}
