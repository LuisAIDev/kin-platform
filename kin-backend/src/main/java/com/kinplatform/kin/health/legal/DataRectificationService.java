package com.kinplatform.kin.health.legal;

import com.kinplatform.common.entity.DataRectificationRequest;
import com.kinplatform.common.repository.DataRectificationRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.hce.entity.PatientIdentification;
import com.kinplatform.kin.health.hce.repository.PatientIdentificationRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataRectificationService {

    private final DataRectificationRepository rectificationRepository;
    private final UserRepository userRepository;
    private final PatientIdentificationRepository patientIdentificationRepository;
    private final AuditLogJpaRepository auditRepository;

    // Supported field paths that can be rectified
    private static final java.util.Set<String> SUPPORTED_FIELDS = java.util.Set.of(
        "users.email",
        "users.phone",
        "users.fullName",
        "patient_identification.document_number",
        "patient_identification.document_type"
    );

    // Fields that are NOT rectifiable (HCE protected by Res 839/1995)
    private static final java.util.Set<String> PROTECTED_FIELDS = java.util.Set.of(
        "encounters",
        "diagnoses",
        "treatment_plans",
        "medical_orders",
        "physical_exams",
        "clinical_documents"
    );

    @Transactional
    public DataRectificationRequest requestRectification(UUID userId, String fieldPath, String oldValue, String newValue, String reason) {
        // Validate field path
        if (!SUPPORTED_FIELDS.contains(fieldPath)) {
            if (PROTECTED_FIELDS.contains(fieldPath)) {
                throw new IllegalArgumentException("El campo '" + fieldPath + "' no puede ser rectificado (HCE protegida por Resolución 839/1995)");
            }
            throw new IllegalArgumentException("Campo no soportado para rectificación: " + fieldPath);
        }

        // Check active request limit (max 5 pending per user)
        long activeCount = rectificationRepository.countByUserIdAndStatusIn(userId, List.of("PENDING", "APPROVED"));
        if (activeCount >= 5) {
            throw new IllegalStateException("Máximo 5 solicitudes activas permitidas por usuario");
        }

        // Check for duplicate pending request for same field
        boolean duplicateExists = rectificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .anyMatch(r -> fieldPath.equals(r.getFieldPath()) && "PENDING".equals(r.getStatus()));
        if (duplicateExists) {
            throw new IllegalStateException("Ya existe una solicitud pendiente para el campo: " + fieldPath);
        }

        DataRectificationRequest request = DataRectificationRequest.builder()
            .userId(userId)
            .fieldPath(fieldPath)
            .oldValue(oldValue)
            .newValue(newValue)
            .reason(reason)
            .status("PENDING")
            .build();

        return rectificationRepository.save(request);
    }

    public List<DataRectificationRequest> getRequestsByUser(UUID userId) {
        return rectificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<DataRectificationRequest> getRequestsByStatus(String status) {
        return rectificationRepository.findByStatus(status);
    }

    @Transactional
    public DataRectificationRequest approveRectification(UUID requestId, UUID adminId, String notes) {
        DataRectificationRequest request = rectificationRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + requestId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Solo se pueden aprobar solicitudes en estado PENDING");
        }

        // Apply the change
        executeChange(request.getUserId(), request.getFieldPath(), request.getNewValue());

        request.setStatus("APPROVED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(Instant.now());
        request.setReviewNotes(notes);
        request = rectificationRepository.save(request);

        // Audit
        auditRepository.save(new com.kinplatform.common.audit.adapter.AuditLogEntity(
            UUID.randomUUID(),
            adminId,
            AuditAction.UPDATE,
            AuditResourceType.USER,
            requestId,
            request.getUserId(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC),
            null, null,
            java.util.Map.of(
                "fieldPath", request.getFieldPath(),
                "oldValue", request.getOldValue(),
                "newValue", request.getNewValue()
            ).toString(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC)
        ));

        return request;
    }

    @Transactional
    public DataRectificationRequest rejectRectification(UUID requestId, UUID adminId, String reason) {
        DataRectificationRequest request = rectificationRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + requestId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Solo se pueden rechazar solicitudes en estado PENDING");
        }

        request.setStatus("REJECTED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(Instant.now());
        request.setReviewNotes(reason);
        request = rectificationRepository.save(request);

        // Audit
        auditRepository.save(new com.kinplatform.common.audit.adapter.AuditLogEntity(
            UUID.randomUUID(),
            adminId,
            AuditAction.UPDATE,
            AuditResourceType.USER,
            requestId,
            request.getUserId(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC),
            null, null,
            java.util.Map.of(
                "fieldPath", request.getFieldPath(),
                "reason", reason
            ).toString(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC)
        ));

        return request;
    }

    @Transactional
    public DataRectificationRequest executeRectification(UUID requestId) {
        DataRectificationRequest request = rectificationRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + requestId));

        if ("EXECUTED".equals(request.getStatus())) {
            throw new IllegalStateException("La rectificación ya fue ejecutada");
        }

        if (!"APPROVED".equals(request.getStatus())) {
            throw new IllegalStateException("Solo se pueden ejecutar solicitudes aprobadas");
        }

        // Apply the change
        executeChange(request.getUserId(), request.getFieldPath(), request.getNewValue());

        request.setStatus("EXECUTED");
        request.setExecutedAt(Instant.now());
        request = rectificationRepository.save(request);

        // Audit
        auditRepository.save(new com.kinplatform.common.audit.adapter.AuditLogEntity(
            UUID.randomUUID(),
            request.getUserId(), // executed by system/user
            AuditAction.EXECUTE,
            AuditResourceType.USER,
            requestId,
            request.getUserId(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC),
            null, null,
            java.util.Map.of(
                "fieldPath", request.getFieldPath(),
                "oldValue", request.getOldValue(),
                "newValue", request.getNewValue()
            ).toString(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC)
        ));

        return request;
    }

    private void executeChange(UUID userId, String fieldPath, String newValue) {
        switch (fieldPath) {
            case "users.email" -> {
                User user = userRepository.findById(userId).orElseThrow();
                user.setEmail(newValue);
                userRepository.save(user);
            }
            case "users.phone" -> {
                User user = userRepository.findById(userId).orElseThrow();
                user.setPhone(newValue);
                userRepository.save(user);
            }
            case "users.fullName" -> {
                User user = userRepository.findById(userId).orElseThrow();
                user.setFullName(newValue);
                userRepository.save(user);
            }
            case "patient_identification.document_number" -> {
                PatientIdentification pi = patientIdentificationRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Identificación del paciente no encontrada"));
                pi.setDocumentNumber(newValue);
                patientIdentificationRepository.save(pi);
            }
            case "patient_identification.document_type" -> {
                PatientIdentification pi = patientIdentificationRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Identificación del paciente no encontrada"));
                pi.setDocumentType(PatientIdentification.DocumentType.valueOf(newValue));
                patientIdentificationRepository.save(pi);
            }
            default -> throw new IllegalArgumentException("Campo no soportado: " + fieldPath);
        }
    }
}


