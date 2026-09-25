package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.ObstetricHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObstetricHistoryRepository extends JpaRepository<ObstetricHistory, UUID> {

    Optional<ObstetricHistory> findByPatientId(UUID patientId);

    @Query("SELECT o FROM ObstetricHistory o WHERE o.patientId = :patientId AND o.currentPregnancy = true")
    Optional<ObstetricHistory> findCurrentPregnancyByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT o FROM ObstetricHistory o WHERE o.patientId = :patientId ORDER BY o.recordedAt DESC")
    List<ObstetricHistory> findByPatientIdOrderByRecordedAtDesc(@Param("patientId") UUID patientId);
}