package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Diagnosis;
import com.kinplatform.kin.health.hce.entity.Diagnosis.DiagnosisType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    Optional<Diagnosis> findByEncounterIdAndDiagnosisType(UUID encounterId, DiagnosisType type);

    List<Diagnosis> findByEncounterId(UUID encounterId);

    List<Diagnosis> findByPatientId(UUID patientId);

    List<Diagnosis> findByEncounterIdAndStatus(UUID encounterId, Diagnosis.Status status);

    @Modifying
    @Transactional
    @Query("UPDATE Diagnosis d SET d.diagnosisType = :newType WHERE d.encounterId = :encounterId AND d.diagnosisType = :oldType")
    int updateDiagnosisTypeByEncounter(@Param("encounterId") UUID encounterId,
                                       @Param("oldType") DiagnosisType oldType,
                                       @Param("newType") DiagnosisType newType);

    @Modifying
    @Transactional
    @Query("UPDATE Diagnosis d SET d.diagnosisType = :type WHERE d.id = :id")
    int setDiagnosisType(@Param("id") UUID id, @Param("type") DiagnosisType type);

    @Query("SELECT COUNT(d) FROM Diagnosis d WHERE d.encounterId = :encounterId AND d.diagnosisType = :type")
    long countByEncounterIdAndType(@Param("encounterId") UUID encounterId, @Param("type") DiagnosisType type);
}