package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.CreateTreatmentPlanRequest;
import com.kinplatform.kin.health.hce.dto.TreatmentPlanResponse;
import com.kinplatform.kin.health.hce.dto.request.TreatmentPlanRequest;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.service.TreatmentPlanService;
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
@Tag(name = "Treatment Plans", description = "Planes de tratamiento / conducta médica")
public class TreatmentPlanController {

    private final TreatmentPlanService treatmentPlanService;

    @PostMapping("/encounters/{encounterId}/treatment-plan")
    @Operation(summary = "Crear plan de tratamiento", description = "Registra el plan de tratamiento/conducta asociado al encuentro")
    public ResponseEntity<TreatmentPlanResponse> create(
            @PathVariable UUID encounterId,
            @Valid @RequestBody TreatmentPlanRequest request) {
        CreateTreatmentPlanRequest internalRequest = CreateTreatmentPlanRequest.builder()
                .encounterId(encounterId)
                .conduct(TreatmentPlan.Conduct.valueOf(request.conduct()))
                .therapeuticGoals(request.therapeuticGoals() != null
                        ? request.therapeuticGoals().toArray(new String[0])
                        : new String[0])
                .followupPlan(null)
                .reevaluationCriteria(null)
                .prognosis(request.prognosis() != null
                        ? TreatmentPlan.Prognosis.valueOf(request.prognosis())
                        : null)
                .estimatedDuration(null)
                .build();
        TreatmentPlanResponse response = treatmentPlanService.upsertPlan(encounterId, internalRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/encounters/{encounterId}/treatment-plan")
    @Operation(summary = "Listar planes de tratamiento por encuentro", description = "Retorna todos los planes de tratamiento del encuentro")
    public ResponseEntity<List<TreatmentPlanResponse>> list(
            @PathVariable UUID encounterId) {
        List<TreatmentPlanResponse> responses = treatmentPlanService.getByEncounter(encounterId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/treatment-plans/{planId}")
    @Operation(summary = "Actualizar plan de tratamiento", description = "Actualiza campos del plan de tratamiento")
    public ResponseEntity<TreatmentPlanResponse> update(
            @PathVariable UUID planId,
            @Valid @RequestBody TreatmentPlanRequest request) {
        CreateTreatmentPlanRequest internalRequest = CreateTreatmentPlanRequest.builder()
                .encounterId(null)
                .conduct(request.conduct() != null
                        ? TreatmentPlan.Conduct.valueOf(request.conduct())
                        : null)
                .therapeuticGoals(request.therapeuticGoals() != null
                        ? request.therapeuticGoals().toArray(new String[0])
                        : null)
                .followupPlan(null)
                .reevaluationCriteria(null)
                .prognosis(request.prognosis() != null
                        ? TreatmentPlan.Prognosis.valueOf(request.prognosis())
                        : null)
                .estimatedDuration(null)
                .build();
        TreatmentPlanResponse response = treatmentPlanService.updatePlan(planId, internalRequest);
        return ResponseEntity.ok(response);
    }
}