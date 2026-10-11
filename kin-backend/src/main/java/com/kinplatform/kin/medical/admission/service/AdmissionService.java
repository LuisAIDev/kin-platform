package com.kinplatform.kin.medical.admission.service;

import com.kinplatform.common.audit.api.AuditService;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.kin.medical.admission.api.PatientDTO;
import com.kinplatform.kin.medical.admission.api.PatientResponse;
import com.kinplatform.kin.medical.admission.domain.Patient;
import com.kinplatform.kin.medical.admission.repository.PatientRepository;
import com.kinplatform.common.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionService {

    private static final Logger log = LoggerFactory.getLogger(AdmissionService.class);

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdmissionService(PatientRepository patientRepository,
                            UserRepository userRepository,
                            AuditService auditService) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PatientResponse register(PatientDTO dto, UUID createdBy) {
        if (patientRepository.existsByDocumentTypeAndDocumentNumber(dto.getDocumentType(), dto.getDocumentNumber())) {
            throw new PatientAlreadyExistsException(
                    "Ya existe un paciente con documento " + dto.getDocumentType() + " " + dto.getDocumentNumber());
        }

        Patient patient = Patient.builder()
                .documentType(dto.getDocumentType())
                .documentNumber(dto.getDocumentNumber())
                .firstName(dto.getFirstName())
                .middleName(dto.getMiddleName())
                .lastName(dto.getLastName())
                .secondLastName(dto.getSecondLastName())
                .birthDate(dto.getBirthDate())
                .gender(dto.getGender())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .city(dto.getCity())
                .department(dto.getDepartment())
                .eps(dto.getEps())
                .regime(dto.getRegime())
                .rhFactor(dto.getRhFactor())
                .emergencyContactName(dto.getEmergencyContactName())
                .emergencyContactPhone(dto.getEmergencyContactPhone())
                .emergencyContactRelationship(dto.getEmergencyContactRelationship())
                .createdBy(createdBy)
                .build();

        Patient saved = patientRepository.save(patient);

        auditService.logAccess(createdBy, AuditAction.CREATE, AuditResourceType.PACIENTE,
                saved.getId(), saved.getId(), java.util.Map.of(
                        "documentType", saved.getDocumentType().name(),
                        "documentNumber", saved.getDocumentNumber()
                ));

        log.info("AdmissionService: paciente registrado id={}, documento={} {}",
                saved.getId(), saved.getDocumentType(), saved.getDocumentNumber());

        return PatientResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Optional<PatientResponse> findById(UUID id) {
        return patientRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .map(PatientResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<PatientResponse> findByDocument(Patient.DocumentType documentType, String documentNumber) {
        return patientRepository.findActiveByDocument(documentType, documentNumber)
                .map(PatientResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> search(String query, Pageable pageable) {
        return patientRepository.searchByNameOrDocument(query, pageable)
                .map(PatientResponse::from);
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findByBirthDateBetween(java.time.LocalDate start, java.time.LocalDate end) {
        return patientRepository.findByBirthDateBetween(start, end).stream()
                .map(PatientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findByEps(String eps) {
        return patientRepository.findByEps(eps).stream()
                .map(PatientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findByRegime(Patient.Regime regime) {
        return patientRepository.findByRegime(regime).stream()
                .map(PatientResponse::from)
                .toList();
    }

    @Transactional
    public PatientResponse update(UUID id, PatientDTO dto, UUID updatedBy) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Paciente no encontrado: " + id));

        if (patient.isDeleted()) {
            throw new PatientNotFoundException("Paciente no encontrado: " + id);
        }

        if (!patient.getDocumentType().equals(dto.getDocumentType())
                || !patient.getDocumentNumber().equals(dto.getDocumentNumber())) {
            if (patientRepository.existsByDocumentTypeAndDocumentNumber(dto.getDocumentType(), dto.getDocumentNumber())) {
                throw new PatientAlreadyExistsException(
                        "Ya existe un paciente con documento " + dto.getDocumentType() + " " + dto.getDocumentNumber());
            }
        }

        patient.setDocumentType(dto.getDocumentType());
        patient.setDocumentNumber(dto.getDocumentNumber());
        patient.setFirstName(dto.getFirstName());
        patient.setMiddleName(dto.getMiddleName());
        patient.setLastName(dto.getLastName());
        patient.setSecondLastName(dto.getSecondLastName());
        patient.setBirthDate(dto.getBirthDate());
        patient.setGender(dto.getGender());
        patient.setPhone(dto.getPhone());
        patient.setEmail(dto.getEmail());
        patient.setAddress(dto.getAddress());
        patient.setCity(dto.getCity());
        patient.setDepartment(dto.getDepartment());
        patient.setEps(dto.getEps());
        patient.setRegime(dto.getRegime());
        patient.setRhFactor(dto.getRhFactor());
        patient.setEmergencyContactName(dto.getEmergencyContactName());
        patient.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        patient.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());

        Patient saved = patientRepository.save(patient);

        auditService.logAccess(updatedBy, AuditAction.UPDATE, AuditResourceType.PACIENTE,
                saved.getId(), saved.getId(), java.util.Map.of(
                        "documentType", saved.getDocumentType().name(),
                        "documentNumber", saved.getDocumentNumber()
                ));

        log.info("AdmissionService: paciente actualizado id={}", saved.getId());

        return PatientResponse.from(saved);
    }

    @Transactional
    public void softDelete(UUID id, UUID deletedBy, String reason) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Paciente no encontrado: " + id));

        if (patient.isDeleted()) {
            throw new PatientNotFoundException("Paciente no encontrado: " + id);
        }

        patient.setDeletedAt(Instant.now());
        patient.setDeletionReason(reason != null ? reason : "Eliminado por usuario");
        patientRepository.save(patient);

        auditService.logAccess(deletedBy, AuditAction.DELETE, AuditResourceType.PACIENTE,
                id, id, java.util.Map.of("reason", reason != null ? reason : ""));

        log.info("AdmissionService: paciente eliminado (soft delete) id={}, motivo={}", id, reason);
    }
}