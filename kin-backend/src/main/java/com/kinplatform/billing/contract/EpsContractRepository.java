package com.kinplatform.billing.contract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EpsContractRepository extends JpaRepository<EpsContract, UUID> {

    Optional<EpsContract> findByOrganizationIdAndEpsNit(UUID organizationId, String epsNit);

    boolean existsByOrganizationIdAndEpsNit(UUID organizationId, String epsNit);

    Optional<EpsContract> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<EpsContract> findByOrganizationIdAndStatus(UUID organizationId, EpsContract.ContractStatus status);

    Page<EpsContract> findByOrganizationId(UUID organizationId, Pageable pageable);

    @Query("SELECT c FROM EpsContract c WHERE c.organizationId = :orgId AND c.status = 'ACTIVE' AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE)")
    List<EpsContract> findActiveContractsForBilling(@Param("orgId") UUID organizationId);
}