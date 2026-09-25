package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, UUID> {

    Optional<TreatmentPlan> findByEncounterId(UUID encounterId);

    List<TreatmentPlan> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<TreatmentPlan> findByPhysicianIdOrderByCreatedAtDesc(UUID physicianId);

    List<TreatmentPlan> findByConduct(Conduct conduct);

    @Query("SELECT t FROM TreatmentPlan t WHERE t.patientId = :patientId AND t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    List<TreatmentPlan> findByPatientIdAndCreatedAtBetween(
            @Param("patientId") UUID patientId,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end);
}