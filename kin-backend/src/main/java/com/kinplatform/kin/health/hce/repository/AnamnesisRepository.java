package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Anamnesis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnamnesisRepository extends JpaRepository<Anamnesis, UUID> {

    Optional<Anamnesis> findByEncounterId(UUID encounterId);

    List<Anamnesis> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<Anamnesis> findByPhysicianIdOrderByCreatedAtDesc(UUID physicianId);

    @Query("SELECT a FROM Anamnesis a WHERE a.patientId = :patientId AND a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<Anamnesis> findByPatientIdAndCreatedAtBetween(
            @Param("patientId") UUID patientId,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end);
}