package com.kinplatform.kin.health.hce.controller;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CancelOrderRequest;
import com.kinplatform.kin.health.hce.dto.CreateMedicalOrderRequest;
import com.kinplatform.kin.health.hce.dto.MedicalOrderResponse;
import com.kinplatform.kin.health.hce.service.MedicalOrderService;
import com.kinplatform.common.user.UserRepository;
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
import java.util.UUID;

@RestController
@RequestMapping({"/health/hce", "/medical/hce"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PHYSICIAN', 'IPS_ADMIN', 'ADMIN')")
@Tag(name = "Medical Orders", description = "Órdenes médicas (HCE Res 839/1995)")
public class MedicalOrderController {

    private final MedicalOrderService medicalOrderService;
    private final UserRepository userRepository;

    @PostMapping("/encounters/{encounterId}/orders")
    @Operation(summary = "Crear orden médica en un encuentro")
    public ResponseEntity<MedicalOrderResponse> create(
            @PathVariable UUID encounterId,
            @Valid @RequestBody CreateMedicalOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalOrderService.addOrderForEncounter(encounterId, request));
    }

    @PutMapping("/orders/{orderId}/execute")
    @Operation(summary = "Ejecutar orden médica", description = "El ejecutor se toma del usuario autenticado")
    public ResponseEntity<MedicalOrderResponse> execute(
            Authentication authentication,
            @PathVariable UUID orderId) {
        UUID executedBy = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(medicalOrderService.executeOrder(orderId, executedBy));
    }

@PutMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancelar orden m�dica")
    public ResponseEntity<MedicalOrderResponse> cancel(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        return ResponseEntity.ok(medicalOrderService.cancelOrder(orderId, request.getReason()));
    }

    @PutMapping("/orders/{orderId}")
    @Operation(summary = "Actualizar orden m�dica", 
               description = "Actualiza una orden existente. Si no se env�a treatmentPlanId, se resuelve del encounter.")
    public ResponseEntity<MedicalOrderResponse> update(
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateMedicalOrderRequest request) {
        MedicalOrderResponse response = medicalOrderService.updateOrder(orderId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/encounters/{encounterId}/orders")
    @Operation(summary = "Listar órdenes por encuentro")
    public ResponseEntity<List<MedicalOrderResponse>> getByEncounter(@PathVariable UUID encounterId) {
        return ResponseEntity.ok(medicalOrderService.getByEncounter(encounterId));
    }

@GetMapping("/treatment-plans/{planId}/orders")
    @Operation(summary = "Listar �rdenes por plan de tratamiento")
    public ResponseEntity<List<MedicalOrderResponse>> getByTreatmentPlan(@PathVariable UUID planId) {
        return ResponseEntity.ok(medicalOrderService.getByTreatmentPlan(planId));
    }

    @DeleteMapping("/orders/{orderId}")
    @Operation(summary = "Eliminar orden m�dica", description = "Borra una orden por ID. No permite borrar �rdenes ejecutadas.")
    public ResponseEntity<Void> delete(@PathVariable UUID orderId) {
        medicalOrderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}

