package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.AnamnesisResponse;
import com.kinplatform.kin.health.hce.dto.CreateAnamnesisRequest;
import com.kinplatform.kin.health.hce.dto.UpdateAnamnesisRequest;
import com.kinplatform.kin.health.hce.service.AnamnesisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/health/hce/encounters", "/medical/hce/encounters"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Anamnesis", description = "Historia clínica del paciente (HCE Res 839/1995)")
public class AnamnesisController {

    private final AnamnesisService anamnesisService;

    @PutMapping("/{encounterId}/anamnesis")
    @Operation(summary = "Crear o actualizar anamnesis", description = "Registra la historia clínica del paciente en un encuentro")
    public ResponseEntity<AnamnesisResponse> upsert(
            @PathVariable UUID encounterId,
            @Valid @RequestBody CreateAnamnesisRequest request) {
        request.setEncounterId(encounterId);
        AnamnesisResponse response = anamnesisService.createAnamnesis(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{encounterId}/anamnesis")
    @Operation(summary = "Obtener anamnesis por encuentro", description = "Retorna la anamnesis asociada al encuentro")
    public ResponseEntity<AnamnesisResponse> get(
            @PathVariable UUID encounterId) {
        AnamnesisResponse response = anamnesisService.getByEncounterId(encounterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{encounterId}/anamnesis/update")
    @Operation(summary = "Actualizar anamnesis", description = "Actualiza campos de la anamnesis existente")
    public ResponseEntity<AnamnesisResponse> update(
            @PathVariable UUID encounterId,
            @Valid @RequestBody UpdateAnamnesisRequest request) {
        AnamnesisResponse response = anamnesisService.updateAnamnesis(encounterId, request);
        return ResponseEntity.ok(response);
    }
}