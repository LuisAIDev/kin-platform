package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.SurgicalHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SurgicalHistoryRepository extends JpaRepository<SurgicalHistory, UUID> {

    List<SurgicalHistory> findByPatientIdOrderBySurgeryDateDesc(UUID patientId);

    List<SurgicalHistory> findByProcedureCupsCode(String cupsCode);

    List<SurgicalHistory> findBySurgeryType(
            com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType surgeryType);

    @Query(
            "SELECT s FROM SurgicalHistory s WHERE s.patientId = :patientId AND s.surgeryDate BETWEEN :start AND :end ORDER BY s.surgeryDate DESC")
    List<SurgicalHistory> findByPatientIdAndSurgeryDateBetween(
            @Param("patientId") UUID patientId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT s FROM SurgicalHistory s WHERE s.procedureCupsCode = :cupsCode ORDER BY s.surgeryDate DESC")
    List<SurgicalHistory> findByProcedureCupsCodeOrderBySurgeryDateDesc(@Param("cupsCode") String cupsCode);

    long countByPatientId(UUID patientId);
}
