package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PatientHistory;
import com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Status;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientHistoryRepository extends JpaRepository<PatientHistory, UUID> {

    List<PatientHistory> findByPatientIdOrderByRecordedAtDesc(UUID patientId);

    List<PatientHistory> findByPatientIdAndHistoryType(UUID patientId, HistoryType historyType);

    List<PatientHistory> findByPatientIdAndStatus(UUID patientId, Status status);

    @Query(
            "SELECT p FROM PatientHistory p WHERE p.patientId = :patientId AND p.historyType = :historyType AND p.status = :status")
    List<PatientHistory> findByPatientIdAndHistoryTypeAndStatus(
            @Param("patientId") UUID patientId,
            @Param("historyType") HistoryType historyType,
            @Param("status") Status status);

    @Query(
            "SELECT p FROM PatientHistory p WHERE p.patientId = :patientId AND p.recordedAt BETWEEN :start AND :end ORDER BY p.recordedAt DESC")
    List<PatientHistory> findByPatientIdAndRecordedAtBetween(
            @Param("patientId") UUID patientId,
            @Param("start") java.time.Instant start,
            @Param("end") java.time.Instant end);

    @Query(
            "SELECT p FROM PatientHistory p WHERE p.patientId = :patientId AND p.historyType = :historyType ORDER BY p.recordedAt DESC")
    List<PatientHistory> findByPatientIdAndHistoryTypeOrderByRecordedAtDesc(
            @Param("patientId") UUID patientId, @Param("historyType") HistoryType historyType);

    long countByPatientIdAndHistoryType(UUID patientId, HistoryType historyType);
}
