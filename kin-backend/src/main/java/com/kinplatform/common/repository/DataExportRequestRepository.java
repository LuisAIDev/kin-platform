package com.kinplatform.common.repository;

import com.kinplatform.common.entity.DataExportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DataExportRequestRepository extends JpaRepository<DataExportRequest, UUID> {

    List<DataExportRequest> findByUserIdOrderByRequestedAtDesc(UUID userId);

    List<DataExportRequest> findByStatus(String status);

    @Query("SELECT d FROM DataExportRequest d WHERE d.expiresAt < :now AND d.status = 'COMPLETED'")
    List<DataExportRequest> findExpiredCompleted(@Param("now") Instant now);

    @Query("SELECT COUNT(d) FROM DataExportRequest d WHERE d.userId = :userId AND d.status IN ('PENDING', 'PROCESSING')")
    long countActiveByUser(@Param("userId") UUID userId);
}