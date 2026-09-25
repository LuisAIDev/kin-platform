package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.DischargeSummary;
import com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DischargeSummaryRepository extends JpaRepository<DischargeSummary, UUID> {

    Optional<DischargeSummary> findByAdmissionId(UUID admissionId);

    List<DischargeSummary> findByPatientIdOrderByDischargeDateDesc(UUID patientId);

    List<DischargeSummary> findByAttendingPhysicianIdOrderByDischargeDateDesc(UUID physicianId);

    List<DischargeSummary> findByDischargeCondition(DischargeCondition condition);

    @Query("SELECT d FROM DischargeSummary d WHERE d.patientId = :patientId AND d.dischargeDate BETWEEN :start AND :end ORDER BY d.dischargeDate DESC")
    List<DischargeSummary> findByPatientIdAndDischargeDateBetween(
            @Param("patientId") UUID patientId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT d FROM DischargeSummary d WHERE d.admissionDate BETWEEN :start AND :end ORDER BY d.admissionDate DESC")
    List<DischargeSummary> findByAdmissionDateBetween(
            @Param("start") Instant start,
            @Param("end") Instant end);

    long countByPatientId(UUID patientId);
}