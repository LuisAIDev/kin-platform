package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreatePatientHistoryRequest;
import com.kinplatform.kin.health.hce.dto.PatientHistoryResponse;
import com.kinplatform.kin.health.hce.dto.response.HistoryAggregateResponse;
import com.kinplatform.kin.health.hce.mapper.HistoryMapper;
import com.kinplatform.kin.health.hce.service.PatientHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping({"/health/hce", "/medical/hce"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Patient History", description = "Historia clínica del paciente: alergias, cirugías, medicamentos, vacunas, antecedentes familiares, tóxicos, gineco-obstétricos")
public class HistoryController {

    private final PatientHistoryService patientHistoryService;
    private final HistoryMapper historyMapper;
    private final com.kinplatform.user.UserRepository userRepository;

    private static final Set<String> VALID_HISTORY_TYPES = Set.of(
            "ALLERGY",
            "SURGERY",
            "MEDICATION",
            "VACCINE",
            "FAMILY_HISTORY",
            "TOXICOLOGICAL",
            "GYNECO_OBSTETRIC"
    );

    @GetMapping("/patients/{patientId}/history")
    @Operation(summary = "Obtener historia clínica completa", description = "Retorna todos los antecedentes del paciente agrupados por tipo (7 arrays)")
    public ResponseEntity<HistoryAggregateResponse> getPatientHistory(
            @PathVariable UUID patientId,
            Authentication authentication) {

        com.kinplatform.user.User user = AuthenticatedUsers.require(userRepository, authentication);
        patientHistoryService.validatePatientAccess(patientId, user);

        List<PatientHistoryResponse> responses = patientHistoryService.getAllByPatient(patientId);
        HistoryAggregateResponse response = historyMapper.toAggregateResponse(responses);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/patients/{patientId}/history")
    @Operation(summary = "Crear item de historia clínica", description = "Registra un antecedente individual (alergia, cirugía, medicamento, etc.)")
    public ResponseEntity<Map<String, Object>> createHistoryItem(
            @PathVariable UUID patientId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String type = (String) request.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        if (type == null || !VALID_HISTORY_TYPES.contains(type)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de historia inválido: " + type));
        }

        com.kinplatform.user.User user = AuthenticatedUsers.require(userRepository, authentication);
        patientHistoryService.validatePatientAccess(patientId, user);

        UUID recordedBy = user.getId();
        CreatePatientHistoryRequest createRequest = historyMapper.toCreateRequest(type, data, patientId, recordedBy);
        PatientHistoryResponse savedResponse = patientHistoryService.addHistory(createRequest);

        Map<String, Object> frontendItem = historyMapper.toFrontendItem(savedResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(frontendItem);
    }

    @PutMapping("/history/{historyId}")
    @Operation(summary = "Actualizar item de historia clínica", description = "Actualiza un antecedente existente por ID")
    public ResponseEntity<Map<String, Object>> updateHistoryItem(
            @PathVariable UUID historyId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String type = (String) request.get("type");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) request.get("data");

        if (type == null || !VALID_HISTORY_TYPES.contains(type)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo de historia inválido: " + type));
        }

        var existingOpt = patientHistoryService.findById(historyId);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var existingResponse = existingOpt.get();

        com.kinplatform.user.User user = AuthenticatedUsers.require(userRepository, authentication);
        patientHistoryService.validatePatientAccess(existingResponse.getPatientId(), user);

        UUID recordedBy = user.getId();
        var updatedEntity = historyMapper.toEntity(type, data, existingResponse.getPatientId(), recordedBy, historyId);

        PatientHistoryResponse updatedResponse = patientHistoryService.updateHistory(historyId,
                CreatePatientHistoryRequest.builder()
                        .patientId(updatedEntity.getPatientId())
                        .historyType(updatedEntity.getHistoryType())
                        .description(updatedEntity.getDescription())
                        .onsetDate(updatedEntity.getOnsetDate())
                        .resolutionDate(updatedEntity.getResolutionDate())
                        .status(updatedEntity.getStatus())
                        .severity(updatedEntity.getSeverity())
                        .notes(updatedEntity.getNotes())
                        .recordedBy(updatedEntity.getRecordedBy())
                        .details(updatedEntity.getDetails())
                        .build());

        Map<String, Object> frontendItem = historyMapper.toFrontendItem(updatedResponse);
        return ResponseEntity.ok(frontendItem);
    }

    @DeleteMapping("/history/{historyId}")
    @Operation(summary = "Eliminar item de historia clínica", description = "Borra un antecedente por ID")
    public ResponseEntity<Void> deleteHistoryItem(
            @PathVariable UUID historyId,
            Authentication authentication) {

        var existingOpt = patientHistoryService.findById(historyId);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var existingResponse = existingOpt.get();

        com.kinplatform.user.User user = AuthenticatedUsers.require(userRepository, authentication);
        patientHistoryService.validatePatientAccess(existingResponse.getPatientId(), user);

        patientHistoryService.deleteById(historyId);
        return ResponseEntity.noContent().build();
    }
}