package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EncounterRepository extends JpaRepository<Encounter, UUID> {

    Optional<Encounter> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("SELECT e FROM Encounter e WHERE e.patientId = :patientId ORDER BY e.startedAt DESC")
    Page<Encounter> findByPatientIdOrderByStartedAtDesc(@Param("patientId") UUID patientId, Pageable pageable);

    @Query("SELECT e FROM Encounter e WHERE e.organizationId = :organizationId AND e.status = :status ORDER BY e.startedAt DESC")
    Page<Encounter> findByOrganizationIdAndStatusOrderByStartedAtDesc(
            @Param("organizationId") UUID organizationId,
            @Param("status") EncounterStatus status,
            Pageable pageable);

    @Query("SELECT e FROM Encounter e WHERE e.physicianId = :physicianId AND e.status = :status ORDER BY e.startedAt DESC")
    Page<Encounter> findByPhysicianIdAndStatusOrderByStartedAtDesc(
            @Param("physicianId") UUID physicianId,
            @Param("status") EncounterStatus status,
            Pageable pageable);

    @Query("SELECT e FROM Encounter e WHERE e.patientId = :patientId AND e.startedAt BETWEEN :start AND :end ORDER BY e.startedAt DESC")
    List<Encounter> findByPatientIdAndStartedAtBetween(
            @Param("patientId") UUID patientId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    long countByPatientIdAndStatus(UUID patientId, EncounterStatus status);

    boolean existsByAppointmentId(UUID appointmentId);
}