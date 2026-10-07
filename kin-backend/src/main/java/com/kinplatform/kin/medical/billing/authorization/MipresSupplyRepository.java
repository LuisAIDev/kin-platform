package com.kinplatform.kin.medical.billing.authorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MipresSupplyRepository extends JpaRepository<MipresSupply, UUID> {

    Optional<MipresSupply> findBySupplyId(String supplyId);

    List<MipresSupply> findByPrescriptionId(UUID prescriptionId);

    List<MipresSupply> findByOrganizationId(UUID organizationId);

    List<MipresSupply> findByOrganizationIdAndStatus(UUID organizationId, MipresSupply.SupplyStatus status);

    @Query("""
        SELECT s FROM MipresSupply s
        WHERE s.organizationId = :orgId
        AND s.supplyDate BETWEEN :start AND :end
        ORDER BY s.supplyDate DESC
        """)
    List<MipresSupply> findByOrganizationIdAndDateRange(
            @Param("orgId") UUID organizationId,
            @Param("start") java.time.LocalDate start,
            @Param("end") java.time.LocalDate end);

    @Query("""
        SELECT s FROM MipresSupply s
        WHERE s.prescriptionNumber = :prescriptionNumber
        ORDER BY s.supplyDate DESC
        """)
    List<MipresSupply> findByPrescriptionNumber(@Param("prescriptionNumber") String prescriptionNumber);
}
