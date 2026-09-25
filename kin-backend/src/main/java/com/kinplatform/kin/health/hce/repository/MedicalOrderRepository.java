package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.MedicalOrder;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.OrderType;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, UUID> {

    List<MedicalOrder> findByTreatmentPlanIdOrderByOrderedAtDesc(UUID treatmentPlanId);

    List<MedicalOrder> findByEncounterIdOrderByOrderedAtDesc(UUID encounterId);

    List<MedicalOrder> findByPatientIdOrderByOrderedAtDesc(UUID patientId);

    List<MedicalOrder> findByPhysicianIdOrderByOrderedAtDesc(UUID physicianId);

    List<MedicalOrder> findByOrderTypeAndStatus(OrderType orderType, Status status);

    List<MedicalOrder> findByStatus(Status status);

    @Query("SELECT m FROM MedicalOrder m WHERE m.patientId = :patientId AND m.orderedAt BETWEEN :start AND :end ORDER BY m.orderedAt DESC")
    List<MedicalOrder> findByPatientIdAndOrderedAtBetween(
            @Param("patientId") UUID patientId,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end);

    @Query("SELECT m FROM MedicalOrder m WHERE m.encounterId = :encounterId ORDER BY m.orderedAt DESC")
    List<MedicalOrder> findByEncounterIdOrderByOrderedAtDescList(@Param("encounterId") UUID encounterId);

    @Query("SELECT m FROM MedicalOrder m WHERE m.treatmentPlanId = :treatmentPlanId ORDER BY m.orderedAt DESC")
    List<MedicalOrder> findByTreatmentPlanIdOrderByOrderedAtDescList(@Param("treatmentPlanId") UUID treatmentPlanId);

    long countByEncounterIdAndStatus(UUID encounterId, com.kinplatform.kin.health.hce.entity.MedicalOrder.Status status);

    boolean existsByEncounterIdAndCupsCode(UUID encounterId, String cupsCode);
}