package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Referral;
import com.kinplatform.kin.health.hce.entity.Referral.ReferralType;
import com.kinplatform.kin.health.hce.entity.Referral.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    Optional<Referral> findByPatientIdAndStatusOrderByCreatedAtDesc(UUID patientId, Status status);

    List<Referral> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<Referral> findByReferringPhysicianIdOrderByCreatedAtDesc(UUID physicianId);

    List<Referral> findByReferredToService(String service);

    List<Referral> findByReferredToInstitution(String institution);

    List<Referral> findByReferralType(ReferralType referralType);

    List<Referral> findByStatus(Status status);

    @Query(
            "SELECT r FROM Referral r WHERE r.referringPhysicianId = :physicianId AND r.status = :status ORDER BY r.createdAt DESC")
    List<Referral> findByReferringPhysicianIdAndStatus(
            @Param("physicianId") UUID physicianId, @Param("status") Status status);

    @Query(
            "SELECT r FROM Referral r WHERE r.patientId = :patientId AND r.createdAt BETWEEN :start AND :end ORDER BY r.createdAt DESC")
    List<Referral> findByPatientIdAndCreatedAtBetween(
            @Param("patientId") UUID patientId,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end);

    long countByPatientIdAndStatus(UUID patientId, Status status);
}
