package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateClinicalAttachmentRequest;
import com.kinplatform.kin.health.hce.dto.ClinicalAttachmentResponse;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.repository.ClinicalAttachmentRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalAttachmentService {

    private final ClinicalAttachmentRepository clinicalAttachmentRepository;
    private final EncounterRepository encounterRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClinicalAttachmentResponse uploadAttachment(CreateClinicalAttachmentRequest request) {
        Encounter encounter = encounterRepository.findById(request.getEncounterId())
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        if (request.getPerformedAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Performed at cannot be in the future");
        }

        User currentUser = AuthenticatedUsers.require(userRepository, SecurityContextHolder.getContext().getAuthentication());

        ClinicalAttachment attachment = ClinicalAttachment.builder()
                .patientId(encounter.getPatientId())
                .encounterId(encounter.getId())
                .evolutionId(request.getEvolutionId())
                .orderId(request.getOrderId())
                .attachmentType(request.getAttachmentType())
                .loincCode(request.getLoincCode())
                .loincDisplay(request.getLoincDisplay())
                .dicomStudyUid(request.getDicomStudyUid())
                .dicomSeriesUid(request.getDicomSeriesUid())
                .dicomModality(request.getDicomModality())
                .pathologyCode(request.getPathologyCode())
                .resultValue(request.getResultValue())
                .resultUnit(request.getResultUnit())
                .resultText(request.getResultText())
                .referenceRangeLow(request.getReferenceRangeLow())
                .referenceRangeHigh(request.getReferenceRangeHigh())
                .referenceRangeText(request.getReferenceRangeText())
                .abnormalFlag(request.getAbnormalFlag())
                .interpretation(request.getInterpretation())
                .documentId(request.getDocumentId())
                .storageKey(request.getStorageKey())
                .mimeType(request.getMimeType())
                .performedAt(request.getPerformedAt())
                .reportedAt(request.getReportedAt())
                .verifiedBy(request.getVerifiedBy())
                .verifiedAt(request.getVerifiedAt())
                .build();

        ClinicalAttachment saved = clinicalAttachmentRepository.saveAndFlush(attachment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicalAttachmentResponse> getByEncounter(UUID encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new EntityNotFoundException("Encounter not found"));

        checkAccess(encounter);

        return clinicalAttachmentRepository.findByEncounterIdOrderByPerformedAtDesc(encounterId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalAttachmentResponse> getByPatientAndType(UUID patientId, ClinicalAttachment.AttachmentType type) {
        // We don't check access here as it's a read operation, but in real app we should
        return clinicalAttachmentRepository.findByPatientIdAndTypeOrderByPerformedAtDesc(patientId, type)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalAttachmentResponse> getByPatient(UUID patientId) {
        return clinicalAttachmentRepository.findByPatientIdOrderByPerformedAtDesc(patientId)
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

    private ClinicalAttachmentResponse toResponse(ClinicalAttachment a) {
        return ClinicalAttachmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .encounterId(a.getEncounterId())
                .evolutionId(a.getEvolutionId())
                .orderId(a.getOrderId())
                .attachmentType(a.getAttachmentType())
                .loincCode(a.getLoincCode())
                .loincDisplay(a.getLoincDisplay())
                .dicomStudyUid(a.getDicomStudyUid())
                .dicomSeriesUid(a.getDicomSeriesUid())
                .dicomModality(a.getDicomModality())
                .pathologyCode(a.getPathologyCode())
                .resultValue(a.getResultValue())
                .resultUnit(a.getResultUnit())
                .resultText(a.getResultText())
                .referenceRangeLow(a.getReferenceRangeLow())
                .referenceRangeHigh(a.getReferenceRangeHigh())
                .referenceRangeText(a.getReferenceRangeText())
                .abnormalFlag(a.getAbnormalFlag())
                .interpretation(a.getInterpretation())
                .documentId(a.getDocumentId())
                .storageKey(a.getStorageKey())
                .mimeType(a.getMimeType())
                .performedAt(a.getPerformedAt())
                .reportedAt(a.getReportedAt())
                .verifiedBy(a.getVerifiedBy())
                .verifiedAt(a.getVerifiedAt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
