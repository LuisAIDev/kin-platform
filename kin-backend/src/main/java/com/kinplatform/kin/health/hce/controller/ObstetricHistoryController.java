package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.CreateObstetricHistoryRequest;
import com.kinplatform.kin.health.hce.dto.ObstetricHistoryResponse;
import com.kinplatform.kin.health.hce.service.ObstetricHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping({"/health/hce", "/medical/hce"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Obstetric History", description = "Historia obstétrica: antecedentes de gestación, parto y lactancia")
public class ObstetricHistoryController {

    private final ObstetricHistoryService obstetricHistoryService;

    @PostMapping("/patients/{patientId}/obstetric-history")
    @Operation(summary = "Crear o actualizar historia obstétrica", description = "Registra o actualiza la historia obstétrica de una paciente (upsert)")
    public ResponseEntity<ObstetricHistoryResponse> upsertHistory(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreateObstetricHistoryRequest request) {
        request.setPatientId(patientId);
        ObstetricHistoryResponse response = obstetricHistoryService.upsertHistory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/obstetric-history/{id}")
    @Operation(summary = "Actualizar historia obstétrica", description = "Actualiza la historia obstétrica existente por ID")
    public ResponseEntity<ObstetricHistoryResponse> updateHistory(
            @PathVariable UUID id,
            @Valid @RequestBody CreateObstetricHistoryRequest request) {
        ObstetricHistoryResponse response = obstetricHistoryService.upsertHistory(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/obstetric-history")
    @Operation(summary = "Obtener historia obstétrica", description = "Retorna la historia obstétrica completa de una paciente")
    public ResponseEntity<ObstetricHistoryResponse> getByPatientId(@PathVariable UUID patientId) {
        Optional<ObstetricHistoryResponse> response = obstetricHistoryService.getByPatientId(patientId);
        return response.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}