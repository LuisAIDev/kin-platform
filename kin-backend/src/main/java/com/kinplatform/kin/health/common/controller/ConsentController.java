package com.kinplatform.kin.health.common.controller;

import com.kinplatform.kin.health.common.dto.CreateUserConsentRequest;
import com.kinplatform.kin.health.common.dto.RevokeConsentRequest;
import com.kinplatform.kin.health.common.dto.UserConsentResponse;
import com.kinplatform.kin.health.common.service.ConsentService;
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
@RequestMapping("/api/v1/health/consents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PATIENT', 'PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "User Consents", description = "Gestión de consentimientos bajo Ley 1581")
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping
    @Operation(summary = "Registrar o actualizar consentimiento", description = "Crea o actualiza un consentimiento del usuario")
    public ResponseEntity<UserConsentResponse> createOrUpdateConsent(
            @Valid @RequestBody CreateUserConsentRequest request) {
        UserConsentResponse response = consentService.createOrUpdateConsent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Listar mis consentimientos", description = "Retorna todos los consentimientos del usuario autenticado")
    public ResponseEntity<List<UserConsentResponse>> getMyConsents() {
        UUID userId = UUID.fromString(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        List<UserConsentResponse> response = consentService.getUserConsents(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/{consentType}")
    @Operation(summary = "Obtener consentimiento activo", description = "Retorna el consentimiento activo (aceptado y no revocado) por tipo")
    public ResponseEntity<UserConsentResponse> getActiveConsent(@PathVariable String consentType) {
        UUID userId = UUID.fromString(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        return consentService.getActiveConsent(userId, consentType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revocar consentimiento", description = "Revoca un consentimiento previamente aceptado")
    public ResponseEntity<UserConsentResponse> revokeConsent(
            @PathVariable UUID id,
            @Valid @RequestBody RevokeConsentRequest request) {
        // Note: id is not used directly, the request body contains the identifiers
        UserConsentResponse response = consentService.revokeConsent(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{consentType}")
    @Operation(summary = "Verificar consentimiento activo", description = "Verifica si el usuario tiene un consentimiento activo por tipo")
    public ResponseEntity<Boolean> checkConsent(@PathVariable String consentType) {
        UUID userId = UUID.fromString(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        boolean hasConsent = consentService.hasActiveConsent(userId, consentType);
        return ResponseEntity.ok(hasConsent);
    }
}