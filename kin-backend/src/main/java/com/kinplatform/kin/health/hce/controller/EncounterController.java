package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.kin.health.hce.dto.CreateEncounterRequest;
import com.kinplatform.kin.health.hce.dto.UpdateEncounterRequest;
import com.kinplatform.kin.health.hce.dto.EncounterResponse;
import com.kinplatform.kin.health.hce.service.EncounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/health/hce/encounters", "/medical/hce/encounters"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Encounters", description = "Gestión de encuentros clínicos (HCE Res 839/1995)")
public class EncounterController {

    private final EncounterService encounterService;

    @PostMapping
    @Operation(summary = "Crear nuevo encuentro", description = "Crea un encuentro en estado IN_PROGRESS")
    public ResponseEntity<EncounterResponse> create(@Valid @RequestBody CreateEncounterRequest request) {
        EncounterResponse response = encounterService.createEncounter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener encuentro por ID")
    public ResponseEntity<EncounterResponse> get(@PathVariable UUID id) {
        EncounterResponse response = encounterService.getEncounter(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar encuentro", description = "Actualiza chiefComplaint y/o encounterType")
    public ResponseEntity<EncounterResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEncounterRequest request) {
        EncounterResponse response = encounterService.updateEncounter(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Cerrar encuentro", description = "Cambia estado a COMPLETED. Requiere diagnóstico principal y plan de manejo")
    public ResponseEntity<EncounterResponse> close(@PathVariable UUID id) {
        EncounterResponse response = encounterService.closeEncounter(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar encuentros por paciente", description = "Ordenados por startedAt descendente")
    public ResponseEntity<List<EncounterResponse>> findByPatient(@RequestParam UUID patientId) {
        return ResponseEntity.ok(encounterService.findByPatientId(patientId));
    }

    @GetMapping(params = "organizationId")
    @Operation(summary = "Listar encuentros por organización", description = "Ordenados por startedAt descendente")
    public ResponseEntity<List<EncounterResponse>> findByOrganization(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(encounterService.findByOrganizationId(organizationId));
    }
}
