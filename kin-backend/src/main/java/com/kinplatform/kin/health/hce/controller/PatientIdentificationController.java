package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.CreatePatientIdentificationRequest;
import com.kinplatform.kin.health.hce.dto.PatientIdentificationResponse;
import com.kinplatform.kin.health.hce.service.PatientIdentificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping({"/health/hce/patients", "/medical/hce/patients"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN', 'PATIENT')")
@Tag(name = "Patient Identification", description = "Identificación del paciente (HCE Res 839/1995)")
public class PatientIdentificationController {

    private final PatientIdentificationService service;

    @PostMapping("/{patientId}/identification")
    @Operation(summary = "Crear o actualizar identificación del paciente")
    public ResponseEntity<PatientIdentificationResponse> upsert(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientIdentificationRequest request) {
        PatientIdentificationResponse response = service.upsertIdentification(patientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{patientId}/identification")
    @Operation(summary = "Obtener identificación del paciente por userId")
    public ResponseEntity<PatientIdentificationResponse> getByPatient(@PathVariable UUID patientId) {
        PatientIdentificationResponse response = service.getByUserId(patientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/identification/by-document")
    @Operation(summary = "Buscar identificación por tipo y número de documento")
    public ResponseEntity<PatientIdentificationResponse> findByDocument(
            @RequestParam String documentType,
            @RequestParam String documentNumber) {
        Optional<PatientIdentificationResponse> response = service.findByDocumentNumber(documentType, documentNumber);
        return response.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/identification/by-eps")
    @Operation(summary = "Listar pacientes por código EPS")
    public ResponseEntity<List<PatientIdentificationResponse>> findByEps(@RequestParam String epsCode) {
        return ResponseEntity.ok(service.findByEpsCode(epsCode));
    }

    @GetMapping("/identification/by-regimen")
    @Operation(summary = "Listar pacientes por régimen")
    public ResponseEntity<List<PatientIdentificationResponse>> findByRegimen(@RequestParam String regimen) {
        return ResponseEntity.ok(service.findByRegimen(regimen));
    }
}
