package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.ClinicalAttachment;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AbnormalFlag;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClinicalAttachmentRepository extends JpaRepository<ClinicalAttachment, UUID> {

    List<ClinicalAttachment> findByPatientIdOrderByPerformedAtDesc(UUID patientId);

    List<ClinicalAttachment> findByEncounterIdOrderByPerformedAtDesc(UUID encounterId);

    List<ClinicalAttachment> findByEvolutionIdOrderByPerformedAtDesc(UUID evolutionId);

    List<ClinicalAttachment> findByOrderId(UUID orderId);

    List<ClinicalAttachment> findByAttachmentType(AttachmentType attachmentType);

    List<ClinicalAttachment> findByLoincCode(String loincCode);

    List<ClinicalAttachment> findByDicomStudyUid(String dicomStudyUid);

    List<ClinicalAttachment> findByAbnormalFlag(AbnormalFlag abnormalFlag);

    @Query(
            "SELECT a FROM ClinicalAttachment a WHERE a.patientId = :patientId AND a.performedAt BETWEEN :start AND :end ORDER BY a.performedAt DESC")
    List<ClinicalAttachment> findByPatientIdAndPerformedAtBetween(
            @Param("patientId") UUID patientId,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end);

    @Query(
            "SELECT a FROM ClinicalAttachment a WHERE a.attachmentType = :type AND a.patientId = :patientId ORDER BY a.performedAt DESC")
    List<ClinicalAttachment> findByPatientIdAndTypeOrderByPerformedAtDesc(
            @Param("patientId") UUID patientId,
            @Param("type") com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType type);

    @Query(
            "SELECT a FROM ClinicalAttachment a WHERE a.loincCode = :loincCode AND a.patientId = :patientId ORDER BY a.performedAt DESC")
    List<ClinicalAttachment> findByPatientIdAndLoincCodeOrderByPerformedAtDesc(
            @Param("patientId") UUID patientId, @Param("loincCode") String loincCode);

    long countByPatientId(UUID patientId);

    long countByPatientIdAndAttachmentType(
            UUID patientId, com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType attachmentType);
}
