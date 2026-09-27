package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.PhysicalExamResponse;
import com.kinplatform.kin.health.hce.dto.CreatePhysicalExamRequest;
import com.kinplatform.kin.health.hce.service.PhysicalExamService;
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
@RequestMapping("/api/v1/health/hce")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Physical Exam", description = "Exploración física del paciente")
public class PhysicalExamController {

    private final PhysicalExamService physicalExamService;

    @PutMapping("/encounters/{encounterId}/physical-exam")
    @Operation(summary = "Registrar examen físico", description = "Registra la exploración física asociada al encuentro")
    public ResponseEntity<PhysicalExamResponse> record(
            @PathVariable UUID encounterId,
            @Valid @RequestBody CreatePhysicalExamRequest request) {
        request.setEncounterId(encounterId);
        PhysicalExamResponse response = physicalExamService.recordExam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/encounters/{encounterId}/physical-exam")
    @Operation(summary = "Obtener examen físico por encuentro", description = "Retorna el examen físico asociado al encuentro")
    public ResponseEntity<PhysicalExamResponse> getByEncounter(
            @PathVariable UUID encounterId) {
        PhysicalExamResponse response = physicalExamService.getByEncounter(encounterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patients/{patientId}/physical-exam/latest")
    @Operation(summary = "Obtener último examen físico del paciente", description = "Retorna el examen físico más reciente del paciente")
    public ResponseEntity<PhysicalExamResponse> getLatestByPatient(
            @PathVariable UUID patientId) {
        PhysicalExamResponse response = physicalExamService.getLatestByPatient(patientId);
        return ResponseEntity.ok(response);
    }
}