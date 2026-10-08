package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.Diagnoses;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagnosesRepository extends JpaRepository<Diagnoses, UUID> {

    List<Diagnoses> findByEncounterIdOrderByCreatedAtDesc(UUID encounterId);

    List<Diagnoses> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    List<Diagnoses> findByEncounterIdAndDiagnosisType(
            UUID encounterId, com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType diagnosisType);

    Optional<Diagnoses> findByEncounterIdAndDiagnosisTypeAndStatus(
            UUID encounterId,
            com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType diagnosisType,
            com.kinplatform.kin.health.hce.entity.Diagnoses.Status status);

    List<Diagnoses> findByPatientIdAndStatus(
            UUID patientId, com.kinplatform.kin.health.hce.entity.Diagnoses.Status status);

    @Query("SELECT d FROM Diagnoses d WHERE d.patientId = :patientId AND d.cie10Code = :cie10Code")
    List<Diagnoses> findByPatientIdAndCie10Code(
            @Param("patientId") UUID patientId, @Param("cie10Code") String cie10Code);

    @Query(
            "SELECT d FROM Diagnoses d WHERE d.encounterId = :encounterId AND d.diagnosisType = :type AND d.status = :status")
    Optional<Diagnoses> findPrincipalByEncounterId(
            @Param("encounterId") UUID encounterId,
            @Param("type") com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType type,
            @Param("status") com.kinplatform.kin.health.hce.entity.Diagnoses.Status status);

    @Query("SELECT d FROM Diagnoses d WHERE d.cie10Code = :cie10Code AND d.status = :status ORDER BY d.createdAt DESC")
    Page<Diagnoses> findByCie10CodeAndStatus(
            @Param("cie10Code") String cie10Code,
            @Param("status") com.kinplatform.kin.health.hce.entity.Diagnoses.Status status,
            org.springframework.data.domain.Pageable pageable);

    long countByEncounterIdAndDiagnosisType(
            UUID encounterId, com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType diagnosisType);

    boolean existsByEncounterIdAndDiagnosisTypeAndStatus(
            UUID encounterId,
            com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType diagnosisType,
            com.kinplatform.kin.health.hce.entity.Diagnoses.Status status);
}
