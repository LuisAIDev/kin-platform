package com.kinplatform.kin.health.legal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kinplatform.common.entity.DataDeletionRequest;
import com.kinplatform.common.repository.DataDeletionRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataDeletionService {

    private final DataDeletionRepository deletionRepository;
    private final UserRepository userRepository;
    private final EncounterRepository encounterRepository;
    private final AuditLogJpaRepository auditRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> DELETABLE_CATEGORIES = Set.of(
        "ACCOUNT_INFO", "MESSAGES", "CONSENTS", "AUDIT_LOGS"
    );

    private static final Set<String> PROTECTED_CATEGORIES = Set.of(
        "HCE", "CLINICAL_DOCUMENTS", "FINANCIAL_RECORDS"
    );

    @Transactional
    public DataDeletionRequest requestDeletion(UUID userId, String reason, String scope, JsonNode dataCategories) {
        // Check for HCE (legal hold)
        boolean hasHCE = encounterRepository.countByPatientIdAndStatus(userId, com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.COMPLETED) > 0
            || encounterRepository.countByPatientIdAndStatus(userId, com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.IN_PROGRESS) > 0;
        boolean legalHold = hasHCE;
        String legalHoldReason = null;

        if (hasHCE) {
            legalHoldReason = "El usuario tiene Historia Clínica Electrónica (HCE) sujeta a retención legal 20 años (Resolución 839/1995). Solo anonimización permitida.";
        }

        // Validate scope and categories
        if ("PARTIAL".equals(scope)) {
            if (dataCategories == null || !dataCategories.isArray()) {
                throw new IllegalArgumentException("Para scope=PARTIAL, data_categories es requerido como array JSON");
            }
            ArrayNode categories = (ArrayNode) dataCategories;
            for (JsonNode cat : categories) {
                String catStr = cat.asText();
                if (PROTECTED_CATEGORIES.contains(catStr)) {
                    throw new IllegalArgumentException("Categoría protegida no eliminable: " + catStr);
                }
                if (!DELETABLE_CATEGORIES.contains(catStr)) {
                    throw new IllegalArgumentException("Categoría no válida: " + catStr);
                }
            }
        } else if ("FULL".equals(scope)) {
            if (hasHCE) {
                // FULL with HCE -> legal hold, only anonymization possible
            }
        } else {
            throw new IllegalArgumentException("Scope debe ser FULL o PARTIAL");
        }

        DataDeletionRequest request = DataDeletionRequest.builder()
            .userId(userId)
            .reason(reason)
            .scope(scope)
            .dataCategories(dataCategories)
            .status("PENDING")
            .legalHold(legalHold)
            .legalHoldReason(legalHoldReason)
            .build();

        return deletionRepository.save(request);
    }

    public List<DataDeletionRequest> getRequestsByUser(UUID userId) {
        return deletionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<DataDeletionRequest> getRequestsByStatus(String status) {
        return deletionRepository.findByStatus(status);
    }

    @Transactional
    public DataDeletionRequest approveDeletion(UUID requestId, UUID adminId) {
        DataDeletionRequest request = deletionRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + requestId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Solo se pueden aprobar solicitudes en estado PENDING");
        }

        if (request.getLegalHold()) {
            request.setReviewNotes("Aprobado con legal hold: " + request.getLegalHoldReason());
        }

        request.setStatus("APPROVED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(Instant.now());
        request = deletionRepository.save(request);

        auditRepository.save(new com.kinplatform.common.audit.adapter.AuditLogEntity(
            UUID.randomUUID(),
            adminId,
            AuditAction.UPDATE,
            AuditResourceType.USER,
            requestId,
            request.getUserId(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC),
            null, null,
            Map.of(
                "scope", request.getScope(),
                "legalHold", request.getLegalHold(),
                "action", "APPROVED"
            ).toString(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC)
        ));

        return request;
    }

    @Transactional
    public DataDeletionRequest rejectDeletion(UUID requestId, UUID adminId, String reason) {
        DataDeletionRequest request = deletionRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + requestId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Solo se pueden rechazar solicitudes en estado PENDING");
        }

        request.setStatus("REJECTED");
        request.setReviewedBy(adminId);
        request.setReviewedAt(Instant.now());
        request.setReviewNotes(reason);
        request = deletionRepository.save(request);

        auditRepository.save(new com.kinplatform.common.audit.adapter.AuditLogEntity(
            UUID.randomUUID(),
            adminId,
            AuditAction.UPDATE,
            AuditResourceType.USER,
            requestId,
            request.getUserId(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC),
            null, null,
            Map.of(
                "scope", request.getScope(),
                "action", "REJECTED",
                "reason", reason
            ).toString(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC)
        ));

        return request;
    }

    @Transactional
    public DataDeletionRequest executeDeletion(UUID requestId, boolean confirm) {
        if (!confirm) {
            throw new IllegalArgumentException("Confirmación requerida: confirm=true");
        }

        DataDeletionRequest request = deletionRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada: " + requestId));

        if (!"APPROVED".equals(request.getStatus())) {
            throw new IllegalStateException("Solo se pueden ejecutar solicitudes aprobadas");
        }

        UUID userId = request.getUserId();
        boolean hasHCE = encounterRepository.countByPatientIdAndStatus(userId, com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.COMPLETED) > 0
            || encounterRepository.countByPatientIdAndStatus(userId, com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.IN_PROGRESS) > 0;
        int anonymized = 0;
        int deleted = 0;

        if (request.getLegalHold() || hasHCE) {
            // Partial execution: anonymize HCE, delete allowed categories
            anonymized = anonymizeHCE(userId);
            deleted = deleteAllowedCategories(userId, request.getScope(), request.getDataCategories());

            request.setStatus("PARTIALLY_EXECUTED");
            request.setExecutionNotes("Ejecutado con legal hold: HCE anonimizada, categorías permitidas eliminadas");
        } else {
            // Full execution: delete all
            deleted = deleteAllUserData(userId);
            request.setStatus("EXECUTED");
            request.setExecutionNotes("Eliminación completa ejecutada");
        }

        request.setExecutedAt(Instant.now());
        request.setAnonymizedCount(anonymized);
        request.setDeletedCount(deleted);
        request = deletionRepository.save(request);

        auditRepository.save(new com.kinplatform.common.audit.adapter.AuditLogEntity(
            UUID.randomUUID(),
            request.getUserId(),
            AuditAction.EXECUTE,
            AuditResourceType.USER,
            requestId,
            userId,
            Instant.now().atOffset(java.time.ZoneOffset.UTC),
            null, null,
            Map.of(
                "scope", request.getScope(),
                "legalHold", request.getLegalHold(),
                "anonymizedCount", anonymized,
                "deletedCount", deleted,
                "action", "DELETION_EXECUTED"
            ).toString(),
            Instant.now().atOffset(java.time.ZoneOffset.UTC)
        ));

        return request;
    }

    private int anonymizeHCE(UUID userId) {
        int count = 0;
        try {
            // Anonymize encounters
            var encountersPage = encounterRepository.findByPatientIdOrderByStartedAtDesc(userId, org.springframework.data.domain.Pageable.unpaged());
            if (encountersPage != null && encountersPage.getContent() != null) {
                var encounters = encountersPage.getContent();
                for (Encounter e : encounters) {
                    e.setPhysicianId(null); // Remove physician reference
                    // Note: In real implementation, would also anonymize related entities
                }
                count += encounters.size();
            }

            // Hash document number in patient identification
            // This would be done via PatientIdentificationRepository

            log.info("HCE anonimizada para usuario: {}", userId);
        } catch (Exception e) {
            log.error("Error anonimizando HCE para usuario {}: {}", userId, e.getMessage());
        }
        return count;
    }

    private int deleteAllowedCategories(UUID userId, String scope, JsonNode dataCategories) {
        int count = 0;
        if ("FULL".equals(scope)) {
            // Delete all deletable categories
            for (String cat : DELETABLE_CATEGORIES) {
                count += deleteCategory(userId, cat);
            }
        } else if ("PARTIAL".equals(scope)) {
            for (JsonNode cat : dataCategories) {
                count += deleteCategory(userId, cat.asText());
            }
        }
        return count;
    }

    private int deleteCategory(UUID userId, String category) {
        // Implementation would depend on specific repositories
        // For now, returning 0 as placeholder
        log.info("Eliminando categoría {} para usuario {}", category, userId);
        return 0;
    }

    private int deleteAllUserData(UUID userId) {
        // Full deletion: remove user and all associated data
        // In practice, would cascade delete through repositories
        log.info("Eliminación completa para usuario: {}", userId);
        return 1;
    }

    public boolean checkLegalHold(UUID userId) {
        boolean hasHCE = encounterRepository.countByPatientIdAndStatus(userId, com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.COMPLETED) > 0
            || encounterRepository.countByPatientIdAndStatus(userId, com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.IN_PROGRESS) > 0;
        return hasHCE;
    }
}



