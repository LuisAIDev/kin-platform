package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PhysicalExam;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhysicalExamRepository extends JpaRepository<PhysicalExam, UUID> {

    Optional<PhysicalExam> findByEncounterId(UUID encounterId);

    List<PhysicalExam> findByEncounterIdOrderByRecordedAtDesc(UUID encounterId);

    List<PhysicalExam> findByPatientIdOrderByRecordedAtDesc(UUID patientId);

    List<PhysicalExam> findByEvolutionId(UUID evolutionId);

    @Query("SELECT p FROM PhysicalExam p WHERE p.patientId = :patientId AND p.recordedAt BETWEEN :start AND :end ORDER BY p.recordedAt DESC")
    List<PhysicalExam> findByPatientIdAndRecordedAtBetween(
            @Param("patientId") UUID patientId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT p FROM PhysicalExam p WHERE p.encounterId = :encounterId ORDER BY p.recordedAt DESC")
    Page<PhysicalExam> findByEncounterIdOrderByRecordedAtDescPage(
            @Param("encounterId") UUID encounterId,
            org.springframework.data.domain.Pageable pageable);
}