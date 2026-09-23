package com.kinplatform.billing.rips.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RipsBatchRepository extends JpaRepository<RipsBatch, UUID> {

    Optional<RipsBatch> findByOrganizationIdAndContractIdAndPeriodStartAndPeriodEndAndRipsType(
            UUID organizationId, UUID contractId, LocalDate periodStart, LocalDate periodEnd, RipsBatch.RipsType ripsType);

    List<RipsBatch> findByOrganizationIdAndPeriodStartBetween(UUID organizationId, LocalDate start, LocalDate end);

    List<RipsBatch> findByContractIdAndStatus(UUID contractId, RipsBatch.BatchStatus status);

    @Query("""
        SELECT b FROM RipsBatch b
        WHERE b.organizationId = :orgId
        AND b.periodStart = :periodStart
        AND b.periodEnd = :periodEnd
        AND b.status IN ('VALID', 'SENT_TO_DIAN', 'ACCEPTED')
        """)
    List<RipsBatch> findValidBatchesForPeriod(@Param("orgId") UUID orgId, @Param("periodStart") LocalDate start, @Param("periodEnd") LocalDate end);
}