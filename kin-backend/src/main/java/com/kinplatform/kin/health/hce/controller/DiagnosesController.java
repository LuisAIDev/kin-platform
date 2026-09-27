package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.DiagnosisResponse;
import com.kinplatform.kin.health.hce.dto.request.CreateDiagnosisRequest;
import com.kinplatform.kin.health.hce.service.DiagnosisService;
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
@RequestMapping("/api/v1/health/hce/encounters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Diagnoses", description = "Diagnósticos clínicos (HCE Res 839/1995)")
public class DiagnosesController {

    private final DiagnosisService diagnosisService;

    @PostMapping("/{encounterId}/diagnoses")
    @Operation(summary = "Crear diagnóstico", description = "Registra un diagnóstico asociado al encuentro")
    public ResponseEntity<DiagnosisResponse> create(
            @PathVariable UUID encounterId,
            @Valid @RequestBody CreateDiagnosisRequest request) {
        request.setEncounterId(encounterId);
        DiagnosisResponse response = diagnosisService.createDiagnosis(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{encounterId}/diagnoses")
    @Operation(summary = "Listar diagnósticos por encuentro", description = "Retorna todos los diagnósticos del encuentro")
    public ResponseEntity<List<DiagnosisResponse>> list(
            @PathVariable UUID encounterId) {
        List<DiagnosisResponse> responses = diagnosisService.getByEncounter(encounterId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{encounterId}/diagnoses/{diagnosisId}/principal")
    @Operation(summary = "Marcar diagnóstico como principal", description = "Desmarca el principal anterior y marca el nuevo como principal")
    public ResponseEntity<DiagnosisResponse> setPrincipal(
            @PathVariable UUID encounterId,
            @PathVariable UUID diagnosisId) {
        DiagnosisResponse response = diagnosisService.setPrincipal(encounterId, diagnosisId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{encounterId}/diagnoses/principal")
    @Operation(summary = "Obtener diagnóstico principal", description = "Retorna el diagnóstico principal del encuentro")
    public ResponseEntity<DiagnosisResponse> getPrincipal(
            @PathVariable UUID encounterId) {
        DiagnosisResponse response = diagnosisService.getPrincipal(encounterId);
        return ResponseEntity.ok(response);
    }
}