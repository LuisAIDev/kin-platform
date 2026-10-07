package com.kinplatform.kin.medical.billing.rips.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface RipsRecordRepository extends JpaRepository<RipsRecord, UUID> {

    List<RipsRecord> findByBatchIdOrderBySequenceNumber(UUID batchId);

    @Query("""
        SELECT r FROM RipsRecord r
        WHERE r.batchId IN (SELECT b.id FROM RipsBatch b WHERE b.organizationId = :orgId)
        AND r.sourceEntityType = :type
        AND r.sourceEntityId = :entityId
        """)
    List<RipsRecord> findBySourceEntity(@Param("orgId") UUID organizationId, @Param("type") String type, @Param("entityId") UUID entityId);
}
