package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.CreateDiagnosisRequest;
import com.kinplatform.kin.health.hce.dto.DiagnosesResponse;
import com.kinplatform.kin.health.hce.service.DiagnosesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/hce")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Diagnoses", description = "Diagnósticos clínicos (HCE Res 839/1995)")
public class DiagnosesController {

    private final DiagnosesService diagnosesService;

    @PostMapping("/encounters/{encounterId}/diagnoses")
    @Operation(summary = "Crear diagnóstico", description = "Registra un diagnóstico asociado al encuentro")
    public ResponseEntity<DiagnosesResponse> create(
            @PathVariable UUID encounterId,
            @Valid @RequestBody CreateDiagnosisRequest request) {
        request.setEncounterId(encounterId);
        DiagnosesResponse response = diagnosesService.addDiagnosis(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/encounters/{encounterId}/diagnoses")
    @Operation(summary = "Listar diagnósticos por encuentro", description = "Retorna todos los diagnósticos del encuentro")
    public ResponseEntity<List<DiagnosesResponse>> list(
            @PathVariable UUID encounterId) {
        List<DiagnosesResponse> responses = diagnosesService.getAllByEncounter(encounterId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/diagnoses/{diagnosisId}/principal")
    @Operation(summary = "Marcar diagnóstico como principal", description = "Desmarca el principal anterior y marca el nuevo como principal")
    public ResponseEntity<DiagnosesResponse> setPrincipal(
            @PathVariable UUID diagnosisId) {
        DiagnosesResponse response = diagnosesService.setPrincipal(diagnosisId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/encounters/{encounterId}/diagnoses/principal")
    @Operation(summary = "Obtener diagnóstico principal", description = "Retorna el diagnóstico principal del encuentro")
    public ResponseEntity<DiagnosesResponse> getPrincipal(
            @PathVariable UUID encounterId) {
        return diagnosesService.getPrincipal(encounterId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No principal diagnosis found for this encounter"));
    }
}
