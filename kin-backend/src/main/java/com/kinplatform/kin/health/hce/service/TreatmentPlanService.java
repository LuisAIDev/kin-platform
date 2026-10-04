package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateTreatmentPlanRequest;
import com.kinplatform.kin.health.hce.dto.TreatmentPlanResponse;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TreatmentPlanService {

    private final TreatmentPlanRepository treatmentPlanRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;

    @Transactional
    public TreatmentPlanResponse createPlan(CreateTreatmentPlanRequest request) {
        Encounter encounter = encounterRepository.findById(request.getEncounterId())
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        TreatmentPlan plan = TreatmentPlan.builder()
                .encounterId(encounter.getId())
                .patientId(encounter.getPatientId())
                .physicianId(currentUser.getId())
                .conduct(request.getConduct())
                .therapeuticGoals(request.getTherapeuticGoals())
                .followupPlan(request.getFollowupPlan())
                .reevaluationCriteria(request.getReevaluationCriteria())
                .prognosis(request.getPrognosis())
                .estimatedDuration(request.getEstimatedDuration())
                .build();

        TreatmentPlan saved = treatmentPlanRepository.saveAndFlush(plan);
        return toResponse(saved);
    }

    @Transactional
    public TreatmentPlanResponse updatePlan(UUID planId, CreateTreatmentPlanRequest request) {
        TreatmentPlan plan = treatmentPlanRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Treatment plan not found"));

        checkAccessByPlan(plan);

        if (request.getConduct() != null) {
            plan.setConduct(request.getConduct());
        }
        if (request.getTherapeuticGoals() != null) {
            plan.setTherapeuticGoals(request.getTherapeuticGoals());
        }
        if (request.getFollowupPlan() != null) {
            plan.setFollowupPlan(request.getFollowupPlan());
        }
        if (request.getReevaluationCriteria() != null) {
            plan.setReevaluationCriteria(request.getReevaluationCriteria());
        }
        if (request.getPrognosis() != null) {
            plan.setPrognosis(request.getPrognosis());
        }
        if (request.getEstimatedDuration() != null) {
            plan.setEstimatedDuration(request.getEstimatedDuration());
        }

        TreatmentPlan updated = treatmentPlanRepository.saveAndFlush(plan);
        return toResponse(updated);
    }

    /**
     * Upsert del plan de tratamiento por encounterId.
     *
     * COMPORTAMIENTO:
     * - Si NO existe plan para el encounter -> crear (delega a createPlan)
     * - Si YA existe -> actualizar (delega a updatePlan)
     *
     * Esto resuelve el bug del frontend que envía POST tanto para create como update.
     */
    @Transactional
    public TreatmentPlanResponse upsertPlan(UUID encounterId, CreateTreatmentPlanRequest request) {
        Optional<TreatmentPlan> existing = treatmentPlanRepository.findByEncounterId(encounterId);
        if (existing.isPresent()) {
            return updatePlan(existing.get().getId(), request);
        }
        return createPlan(request);
    }

    @Transactional(readOnly = true)
    public List<TreatmentPlanResponse> getByEncounter(UUID encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        return treatmentPlanRepository.findByEncounterId(encounterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void checkAccess(Encounter encounter) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = encounter.getPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this encounter");
        }
    }

    private void checkAccessByPlan(TreatmentPlan plan) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = plan.getPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this treatment plan");
        }
    }

    private TreatmentPlanResponse toResponse(TreatmentPlan p) {
        return TreatmentPlanResponse.builder()
                .id(p.getId())
                .encounterId(p.getEncounterId())
                .patientId(p.getPatientId())
                .physicianId(p.getPhysicianId())
                .conduct(p.getConduct())
                .therapeuticGoals(p.getTherapeuticGoals())
                .followupPlan(p.getFollowupPlan())
                .reevaluationCriteria(p.getReevaluationCriteria())
                .prognosis(p.getPrognosis())
                .estimatedDuration(p.getEstimatedDuration())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
