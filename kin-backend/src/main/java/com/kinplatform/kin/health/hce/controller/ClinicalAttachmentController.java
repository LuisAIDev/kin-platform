package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.ClinicalAttachmentResponse;
import com.kinplatform.kin.health.hce.dto.CreateClinicalAttachmentRequest;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment;
import com.kinplatform.kin.health.hce.service.ClinicalAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health/hce")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Clinical Attachments", description = "Anexos clínicos: laboratorio, imágenes, patología, etc.")
public class ClinicalAttachmentController {

    private final ClinicalAttachmentService clinicalAttachmentService;

    @PostMapping("/encounters/{encounterId}/attachments")
    @Operation(summary = "Subir anexo clínico", description = "Registra un anexo clínico (laboratorio, imagen, patología, etc.) asociado a un encuentro")
    public ResponseEntity<ClinicalAttachmentResponse> upload(
            @PathVariable UUID encounterId,
            @Valid @RequestBody CreateClinicalAttachmentRequest request) {
        request.setEncounterId(encounterId);
        ClinicalAttachmentResponse response = clinicalAttachmentService.uploadAttachment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/encounters/{encounterId}/attachments")
    @Operation(summary = "Listar anexos por encuentro", description = "Retorna todos los anexos clínicos asociados a un encuentro, ordenados por fecha descendente")
    public ResponseEntity<List<ClinicalAttachmentResponse>> getByEncounter(@PathVariable UUID encounterId) {
        List<ClinicalAttachmentResponse> responses = clinicalAttachmentService.getByEncounter(encounterId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/patients/{patientId}/attachments")
    @Operation(summary = "Listar anexos de un paciente", description = "Retorna anexos del paciente, opcionalmente filtrados por tipo")
    public ResponseEntity<List<ClinicalAttachmentResponse>> getByPatient(
            @PathVariable UUID patientId,
            @RequestParam(required = false) ClinicalAttachment.AttachmentType type) {
        List<ClinicalAttachmentResponse> responses;
        if (type != null) {
            responses = clinicalAttachmentService.getByPatientAndType(patientId, type);
        } else {
            responses = clinicalAttachmentService.getByPatient(patientId);
        }
        return ResponseEntity.ok(responses);
    }
}