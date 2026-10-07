package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreatePatientIdentificationRequest;
import com.kinplatform.kin.health.hce.dto.PatientIdentificationResponse;
import com.kinplatform.kin.health.hce.entity.PatientIdentification;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.Regimen;
import com.kinplatform.kin.health.hce.mapper.PatientIdentificationMapper;
import com.kinplatform.kin.health.hce.repository.PatientIdentificationRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientIdentificationService {

    private final PatientIdentificationRepository repository;
    private final UserRepository userRepository;
    private final PatientIdentificationMapper mapper;

    @Transactional
    public PatientIdentificationResponse upsertIdentification(UUID userId, CreatePatientIdentificationRequest request) {
        // Verificar que el usuario existe
        if (!userRepository.existsById(request.getUserId())) {
            throw new EntityNotFoundException("User not found");
        }

        // Validar que el documento no exista para OTRO usuario (único por document_type + document_number global)
        if (repository.existsByDocumentTypeAndDocumentNumber(
                DocumentType.valueOf(request.getDocumentType()), request.getDocumentNumber())) {
            // Verificar si ya existe para este usuario
            Optional<com.kinplatform.kin.health.hce.entity.PatientIdentification> existing = 
                repository.findByUserId(request.getUserId());
            if (existing.isPresent()) {
                // Si ya existe para este usuario, permitimos actualizar (incluso cambiar el número de documento)
                // No lanzamos excepción, permitimos la actualización
            } else {
                // El documento existe para OTRO usuario
                throw new IllegalStateException("Document number already registered for another user");
            }
        }

        com.kinplatform.kin.health.hce.entity.PatientIdentification entity = com.kinplatform.kin.health.hce.entity.PatientIdentification.builder()
                .userId(request.getUserId())
                .documentType(request.getDocumentType() != null ? DocumentType.valueOf(request.getDocumentType()) : null)
                .documentNumber(request.getDocumentNumber())
                .documentExpeditionDate(request.getDocumentExpeditionDate())
                .documentExpeditionPlace(request.getDocumentExpeditionPlace())
                .rhFactor(request.getRhFactor() != null ? com.kinplatform.kin.health.hce.entity.PatientIdentification.RhFactor.valueOf(request.getRhFactor()) : null)
                .epsCode(request.getEpsCode())
                .epsName(request.getEpsName())
                .regimen(request.getRegimen() != null ? Regimen.valueOf(request.getRegimen()) : null)
                .guardianName(request.getGuardianName())
                .guardianDocumentType(request.getGuardianDocumentType() != null ? 
                    com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType.valueOf(request.getGuardianDocumentType()) : null)
                .guardianDocumentNumber(request.getGuardianDocumentNumber())
                .guardianPhone(request.getGuardianPhone())
                .guardianRelationship(request.getGuardianRelationship())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .emergencyContactRelationship(request.getEmergencyContactRelationship())
                .address(request.getAddress())
                .cityCode(request.getCityCode())
                .departmentCode(request.getDepartmentCode())
                .zone(request.getZone() != null ? com.kinplatform.kin.health.hce.entity.PatientIdentification.Zone.valueOf(request.getZone()) : null)
                .stratum(request.getStratum())
                .emailInstitutional(request.getEmailInstitutional())
                .phoneSecondary(request.getPhoneSecondary())
                .ethnicity(request.getEthnicity())
                .displacementVictim(request.getDisplacementVictim())
                .disabilityCertificate(request.getDisabilityCertificate())
                .build();

        com.kinplatform.kin.health.hce.entity.PatientIdentification saved = repository.saveAndFlush(entity);
        return mapper.toResponse(saved);
    }

    /**
     * @deprecated Usar {@link #getByUserIdOptional(UUID)}; el endpoint devuelve
     * 204 (No Content) cuando no existe. Se conserva por compatibilidad.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public PatientIdentificationResponse getByUserId(UUID userId) {
        return getByUserIdOptional(userId)
                .orElseThrow(() -> new EntityNotFoundException("Patient identification not found for user: " + userId));
    }

    @Transactional(readOnly = true)
    public Optional<PatientIdentificationResponse> getByUserIdOptional(UUID userId) {
        return repository.findByUserId(userId).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<PatientIdentificationResponse> findByDocumentNumber(String documentType, String documentNumber) {
        return repository.findByDocumentTypeAndDocumentNumber(
                        com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType.valueOf(documentType),
                        documentNumber)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PatientIdentificationResponse> findByEpsCode(String epsCode) {
        return repository.findByEpsCode(epsCode)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientIdentificationResponse> findByRegimen(String regimen) {
        return repository.findByRegimen(regimen)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}

