package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.InformedConsent;
import com.kinplatform.kin.health.hce.entity.InformedConsent.ConsentType;
import com.kinplatform.kin.health.hce.entity.InformedConsent.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InformedConsentRepository extends JpaRepository<InformedConsent, UUID> {

    Optional<InformedConsent> findByPatientIdAndProcedureNameAndStatus(
            UUID patientId, String procedureName, Status status);

    List<InformedConsent> findByPatientIdOrderBySignedAtDesc(UUID patientId);

    List<InformedConsent> findByConsentType(ConsentType consentType);

    List<InformedConsent> findByStatus(Status status);

    @Query(
            "SELECT c FROM InformedConsent c WHERE c.patientId = :patientId AND c.status = :status ORDER BY c.signedAt DESC")
    List<InformedConsent> findByPatientIdAndStatusOrderBySignedAtDesc(
            @Param("patientId") UUID patientId, @Param("status") Status status);

    @Query(
            "SELECT c FROM InformedConsent c WHERE c.physicianId = :physicianId AND c.signedAt BETWEEN :start AND :end ORDER BY c.signedAt DESC")
    List<InformedConsent> findByPhysicianIdAndSignedAtBetween(
            @Param("physicianId") UUID physicianId,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end);

    long countByPatientIdAndStatus(UUID patientId, Status status);

    boolean existsByPatientIdAndProcedureNameAndStatus(UUID patientId, String procedureName, Status status);
}
