package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.AnamnesisResponse;
import com.kinplatform.kin.health.hce.dto.CreateAnamnesisRequest;
import com.kinplatform.kin.health.hce.dto.UpdateAnamnesisRequest;
import com.kinplatform.kin.health.hce.entity.Anamnesis;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.AnamnesisRepository;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnamnesisService {

    private final AnamnesisRepository anamnesisRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;

    @Transactional
    public AnamnesisResponse createAnamnesis(CreateAnamnesisRequest request) {
        Encounter encounter = encounterRepository.findById(request.getEncounterId())
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        if (request.getSeveritySelfReported() != null) {
            if (request.getSeveritySelfReported() < 1 || request.getSeveritySelfReported() > 10) {
                throw new IllegalArgumentException("Severity self-reported must be between 1 and 10");
            }
        }

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        Anamnesis anamnesis = Anamnesis.builder()
                .encounterId(encounter.getId())
                .patientId(encounter.getPatientId())
                .physicianId(currentUser.getId())
                .onsetDatetime(request.getOnsetDatetime())
                .evolutionDescription(request.getEvolutionDescription())
                .aggravatingFactors(request.getAggravatingFactors())
                .alleviatingFactors(request.getAlleviatingFactors())
                .associatedSymptoms(request.getAssociatedSymptoms())
                .severitySelfReported(request.getSeveritySelfReported())
                .systemsReview(request.getSystemsReview() != null ? request.getSystemsReview() : "{}")
                .previousEpisodes(request.getPreviousEpisodes() != null ? request.getPreviousEpisodes() : 0)
                .previousTreatments(request.getPreviousTreatments())
                .functionalImpact(request.getFunctionalImpact())
                .build();

        Anamnesis saved = anamnesisRepository.saveAndFlush(anamnesis);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AnamnesisResponse getByEncounterId(UUID encounterId) {
        Anamnesis anamnesis = anamnesisRepository.findByEncounterId(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Anamnesis not found for encounter"));
        checkAccessByAnamnesis(anamnesis);
        return toResponse(anamnesis);
    }

    @Transactional
    public AnamnesisResponse updateAnamnesis(UUID id, UpdateAnamnesisRequest request) {
        Anamnesis anamnesis = anamnesisRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Anamnesis not found"));

        checkAccessByAnamnesis(anamnesis);

        if (request.getSeveritySelfReported() != null) {
            if (request.getSeveritySelfReported() < 1 || request.getSeveritySelfReported() > 10) {
                throw new IllegalArgumentException("Severity self-reported must be between 1 and 10");
            }
            anamnesis.setSeveritySelfReported(request.getSeveritySelfReported());
        }

        if (request.getOnsetDatetime() != null) {
            anamnesis.setOnsetDatetime(request.getOnsetDatetime());
        }
        if (request.getEvolutionDescription() != null) {
            anamnesis.setEvolutionDescription(request.getEvolutionDescription());
        }
        if (request.getAggravatingFactors() != null) {
            anamnesis.setAggravatingFactors(request.getAggravatingFactors());
        }
        if (request.getAlleviatingFactors() != null) {
            anamnesis.setAlleviatingFactors(request.getAlleviatingFactors());
        }
        if (request.getAssociatedSymptoms() != null) {
            anamnesis.setAssociatedSymptoms(request.getAssociatedSymptoms());
        }
        if (request.getSystemsReview() != null) {
            anamnesis.setSystemsReview(request.getSystemsReview());
        }
        if (request.getPreviousEpisodes() != null) {
            anamnesis.setPreviousEpisodes(request.getPreviousEpisodes());
        }
        if (request.getPreviousTreatments() != null) {
            anamnesis.setPreviousTreatments(request.getPreviousTreatments());
        }
        if (request.getFunctionalImpact() != null) {
            anamnesis.setFunctionalImpact(request.getFunctionalImpact());
        }

        Anamnesis updated = anamnesisRepository.saveAndFlush(anamnesis);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<AnamnesisResponse> findByPatientId(UUID patientId) {
        return anamnesisRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
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

    private void checkAccessByAnamnesis(Anamnesis anamnesis) {
        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_IPS_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isPhysician = anamnesis.getPhysicianId().equals(currentUser.getId());

        if (!isAdmin && !isPhysician) {
            throw new AccessDeniedException("Only the assigned physician or an admin can access this anamnesis");
        }
    }

    private AnamnesisResponse toResponse(Anamnesis a) {
        return AnamnesisResponse.builder()
                .id(a.getId())
                .encounterId(a.getEncounterId())
                .patientId(a.getPatientId())
                .physicianId(a.getPhysicianId())
                .onsetDatetime(a.getOnsetDatetime())
                .evolutionDescription(a.getEvolutionDescription())
                .aggravatingFactors(a.getAggravatingFactors())
                .alleviatingFactors(a.getAlleviatingFactors())
                .associatedSymptoms(a.getAssociatedSymptoms())
                .severitySelfReported(a.getSeveritySelfReported())
                .systemsReview(a.getSystemsReview())
                .previousEpisodes(a.getPreviousEpisodes())
                .previousTreatments(a.getPreviousTreatments())
                .functionalImpact(a.getFunctionalImpact())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
