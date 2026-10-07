package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateMedicalOrderRequest;
import com.kinplatform.kin.health.hce.dto.MedicalOrderResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.MedicalOrder;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.MedicalOrderRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicalOrderService {

    private final MedicalOrderRepository medicalOrderRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final UserRepository userRepository;
    private final EncounterRepository encounterRepository;

    @Transactional
    public MedicalOrderResponse addOrder(CreateMedicalOrderRequest request) {
        TreatmentPlan treatmentPlan = treatmentPlanRepository.findById(request.getTreatmentPlanId())
                .orElseThrow(() -> new EntityNotFoundException("Treatment plan not found"));

        checkAccessByTreatmentPlan(treatmentPlan);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        MedicalOrder order = MedicalOrder.builder()
                .treatmentPlanId(treatmentPlan.getId())
                .encounterId(treatmentPlan.getEncounterId())
                .patientId(treatmentPlan.getPatientId())
                .physicianId(currentUser.getId())
                .orderType(request.getOrderType())
                .drugName(request.getDrugName())
                .dose(request.getDose())
                .doseUnit(request.getDoseUnit())
                .route(request.getRoute())
                .frequency(request.getFrequency())
                .durationDays(request.getDurationDays())
                .cupsCode(request.getCupsCode())
                .cupsDescription(request.getCupsDescription())
                .bodySite(request.getBodySite())
                .priority(request.getPriority() != null ? request.getPriority() : MedicalOrder.Priority.ROUTINE)
                .status(MedicalOrder.Status.ORDERED)
                .instructions(request.getInstructions())
                .orderedAt(Instant.now())
                .build();

        MedicalOrder saved = medicalOrderRepository.saveAndFlush(order);
        return toResponse(saved);
    }

    @Transactional
    public MedicalOrderResponse addOrderForEncounter(UUID encounterId, CreateMedicalOrderRequest request) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        TreatmentPlan treatmentPlan = treatmentPlanRepository.findByEncounterId(encounter.getId())
                .orElseThrow(() -> new EntityNotFoundException("No treatment plan found for encounter"));

        request.setTreatmentPlanId(treatmentPlan.getId());
        return addOrder(request);
    }

    @Transactional
    public MedicalOrderResponse executeOrder(UUID orderId, UUID executedBy) {
        MedicalOrder order = medicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Medical order not found"));

        checkAccessByOrder(order);

        if (order.getStatus() == MedicalOrder.Status.EXECUTED) {
            throw new IllegalStateException("Order already executed");
        }
        if (order.getStatus() == MedicalOrder.Status.CANCELLED) {
            throw new IllegalStateException("Cannot execute cancelled order");
        }

        order.setStatus(MedicalOrder.Status.EXECUTED);
        order.setExecutedAt(Instant.now());
        order.setExecutedBy(executedBy);

        MedicalOrder saved = medicalOrderRepository.saveAndFlush(order);
        return toResponse(saved);
    }

    @Transactional
    public MedicalOrderResponse cancelOrder(UUID orderId, String reason) {
        MedicalOrder order = medicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Medical order not found"));

        checkAccessByOrder(order);

        if (order.getStatus() == MedicalOrder.Status.EXECUTED) {
            throw new IllegalStateException("Cannot cancel executed order");
        }

        order.setStatus(MedicalOrder.Status.CANCELLED);
        order.setExecutionNotes(reason != null ? reason : "Cancelled without reason");
        order.setExecutedAt(Instant.now());

        MedicalOrder saved = medicalOrderRepository.saveAndFlush(order);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MedicalOrderResponse> getByTreatmentPlan(UUID treatmentPlanId) {
        TreatmentPlan treatmentPlan = treatmentPlanRepository.findById(treatmentPlanId)
                .orElseThrow(() -> new EntityNotFoundException("Treatment plan not found"));

        checkAccessByTreatmentPlan(treatmentPlan);

        return medicalOrderRepository.findByTreatmentPlanIdOrderByOrderedAtDesc(treatmentPlanId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalOrderResponse> getByEncounter(UUID encounterId) {
        return medicalOrderRepository.findByEncounterIdOrderByOrderedAtDescList(encounterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void checkAccessByTreatmentPlan(TreatmentPlan treatmentPlan) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = treatmentPlan.getPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this treatment plan");
        }
    }

    private void checkAccessByOrder(MedicalOrder order) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = order.getPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this medical order");
        }
    }

    /**
     * Resuelve el treatmentPlanId si el request no lo incluye.
     * Busca el plan de tratamiento del encounter.
     * Si no hay plan, retorna null (la orden queda sin plan asociado).
     */
    private UUID resolveTreatmentPlanId(UUID encounterId, UUID providedPlanId) {
        if (providedPlanId != null) {
            return providedPlanId;
        }
        return treatmentPlanRepository
                .findByEncounterId(encounterId)
                .map(TreatmentPlan::getId)
                .orElse(null);
    }

    /**
     * Actualiza una orden médica existente.
     * Si el request no trae treatmentPlanId, se resuelve del encounter de la orden existente.
     * Si no hay plan asociado, la orden queda sin plan (treatmentPlanId = null).
     *
     * Reglas de validación CUPS:
     * - PROCEDURE, LAB_EXAM, IMAGING requieren cupsCode obligatorio
     * - Se valida después de aplicar los cambios al objeto
     */
    @Transactional
    public MedicalOrderResponse updateOrder(UUID id, CreateMedicalOrderRequest request) {
        MedicalOrder existing = medicalOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medical order not found with id: " + id));

        // Validar acceso usando la orden existente
        checkAccessByOrder(existing);

        // Si el request no trae treatmentPlanId, resolverlo del encounter de la orden existente
        UUID resolvedPlanId = resolveTreatmentPlanId(existing.getEncounterId(), request.getTreatmentPlanId());

        // Actualizar SOLO campos de negocio (preservar id, encounterId, patientId, physicianId, orderedAt, createdAt)
        existing.setTreatmentPlanId(resolveTreatmentPlanId(existing.getEncounterId(), request.getTreatmentPlanId()));
        if (request.getOrderType() != null) existing.setOrderType(request.getOrderType());
        if (request.getPriority() != null) existing.setPriority(request.getPriority());
        existing.setDrugName(request.getDrugName());
        existing.setDose(request.getDose());
        existing.setDoseUnit(request.getDoseUnit());
        existing.setRoute(request.getRoute());
        existing.setFrequency(request.getFrequency());
        existing.setDurationDays(request.getDurationDays());
        existing.setCupsCode(request.getCupsCode());
        existing.setCupsDescription(request.getCupsDescription());
        existing.setBodySite(request.getBodySite());
        existing.setInstructions(request.getInstructions());

        // Validar CUPS obligatorio para PROCEDURE, LAB_EXAM, IMAGING
        if (existing.getOrderType() == MedicalOrder.OrderType.PROCEDURE
                || existing.getOrderType() == MedicalOrder.OrderType.LAB_EXAM
                || existing.getOrderType() == MedicalOrder.OrderType.IMAGING) {
            if (existing.getCupsCode() == null || existing.getCupsCode().trim().isEmpty()) {
                throw new IllegalArgumentException("cupsCode es obligatorio para PROCEDURE, LAB_EXAM e IMAGING");
            }
        }

        MedicalOrder saved = medicalOrderRepository.saveAndFlush(existing);
        return toResponse(saved);
    }

    /**
     * Elimina una orden m�dica por ID.
     * No permite borrar �rdenes ejecutadas (status EXECUTED).
     */
    @Transactional
    public void deleteOrder(UUID id) {
        MedicalOrder existing = medicalOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medical order not found with id: " + id));

        checkAccessByOrder(existing);

        // No permitir borrar �rdenes ya ejecutadas
        if (existing.getStatus() == MedicalOrder.Status.EXECUTED) {
            throw new IllegalStateException(
                    "Cannot delete an EXECUTED medical order. Cancel it first if needed."
            );
        }

        medicalOrderRepository.deleteById(id);
    }

    private MedicalOrderResponse toResponse(MedicalOrder m) {
        return MedicalOrderResponse.builder()
                .id(m.getId())
                .treatmentPlanId(m.getTreatmentPlanId())
                .encounterId(m.getEncounterId())
                .patientId(m.getPatientId())
                .physicianId(m.getPhysicianId())
                .orderType(m.getOrderType())
                .drugName(m.getDrugName())
                .dose(m.getDose())
                .doseUnit(m.getDoseUnit())
                .route(m.getRoute())
                .frequency(m.getFrequency())
                .durationDays(m.getDurationDays())
                .cupsCode(m.getCupsCode())
                .cupsDescription(m.getCupsDescription())
                .bodySite(m.getBodySite())
                .priority(m.getPriority())
                .status(m.getStatus())
                .instructions(m.getInstructions())
                .orderedAt(m.getOrderedAt())
                .executedAt(m.getExecutedAt())
                .executedBy(m.getExecutedBy())
                .executionNotes(m.getExecutionNotes())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}

