package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.security.TenantContext;
import com.kinplatform.kin.health.hce.dto.CreateEncounterRequest;
import com.kinplatform.kin.health.hce.dto.UpdateEncounterRequest;
import com.kinplatform.kin.health.hce.dto.EncounterResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.mapper.EncounterMapper;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.DiagnosesRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;
    private final DiagnosesRepository diagnosesRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final EncounterMapper encounterMapper;

    @Transactional
    public EncounterResponse createEncounter(CreateEncounterRequest request) {
        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient not found"));

        UUID physicianId = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication()).getId();

        // Validación crítica: el médico no puede abrir una consulta sobre sí mismo.
        if (patient.getId().equals(physicianId)) {
            throw new IllegalStateException("El paciente no puede ser el mismo que el médico");
        }

        // La organización del encuentro es la del paciente (IPS) cuando existe; para
        // pacientes personales sin IPS (organization_id null) se usa la organización
        // del médico autenticado (TenantContext, con fallback a la organización demo).
        UUID organizationId = patient.getOrganizationId() != null
                ? patient.getOrganizationId()
                : TenantContext.getOrDefault();

        Encounter encounter = Encounter.builder()
                .patientId(request.getPatientId())
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType(EncounterType.valueOf(request.getEncounterType()))
                .status(EncounterStatus.IN_PROGRESS)
                .chiefComplaint(request.getChiefComplaint())
                .appointmentId(request.getAppointmentId())
                .build();

        Encounter saved = encounterRepository.saveAndFlush(encounter);
        return encounterMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public EncounterResponse getEncounter(UUID id) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));
        checkAccess(encounter);
        return encounterMapper.toResponse(encounter);
    }

    @Transactional
    public EncounterResponse updateEncounter(UUID id, com.kinplatform.kin.health.hce.dto.UpdateEncounterRequest request) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));
        checkAccess(encounter);

        if (request.getChiefComplaint() != null) {
            encounter.setChiefComplaint(request.getChiefComplaint());
        }
        if (request.getEncounterType() != null) {
            encounter.setEncounterType(com.kinplatform.kin.health.hce.entity.Encounter.EncounterType.valueOf(request.getEncounterType()));
        }

        Encounter updated = encounterRepository.saveAndFlush(encounter);
        return encounterMapper.toResponse(encounter);
    }

    @Transactional
    public EncounterResponse closeEncounter(UUID id) {
        Encounter encounter = encounterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));
        checkAccess(encounter);

        // Validar que tenga diagnóstico PRINCIPAL
        boolean hasPrincipalDiagnosis = diagnosesRepository.existsByEncounterIdAndDiagnosisTypeAndStatus(
                encounter.getId(),
                com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL,
                com.kinplatform.kin.health.hce.entity.Diagnoses.Status.ACTIVE
        );
        if (!hasPrincipalDiagnosis) {
            throw new IllegalStateException("Cannot close encounter: missing principal diagnosis");
        }

        // Validar que tenga plan de manejo
        boolean hasTreatmentPlan = treatmentPlanRepository.findByEncounterId(encounter.getId()).isPresent();
        if (!hasTreatmentPlan) {
            throw new IllegalStateException("Cannot close encounter: missing treatment plan");
        }

        encounter.setStatus(com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.COMPLETED);
        encounter.setClosedAt(java.time.Instant.now());
        Encounter closed = encounterRepository.saveAndFlush(encounter);
        return encounterMapper.toResponse(closed);
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> findByPatientId(UUID patientId) {
        return encounterRepository.findByPatientIdOrderByStartedAtDesc(patientId, Pageable.unpaged())
                .stream()
                .map(encounterMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> findByOrganizationId(UUID organizationId) {
        return encounterRepository.findByOrganizationIdOrderByStartedAtDesc(organizationId, Pageable.unpaged())
                .stream()
                .map(encounterMapper::toResponse)
                .toList();
    }

    private void checkAccess(Encounter encounter) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Allow IPS_ADMIN or assigned physician
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));

        UUID currentUserId = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication()).getId();
        boolean isPhysician = encounter.getPhysicianId().equals(currentUserId);

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this encounter");
        }
    }
}

