package com.kinplatform.billing.authorization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationRepository extends JpaRepository<Authorization, UUID> {

    Optional<Authorization> findByContractIdAndAuthorizationNumber(UUID contractId, String authorizationNumber);

    List<Authorization> findByContractIdAndStatus(UUID contractId, Authorization.AuthorizationStatus status);

    List<Authorization> findByPatientIdAndStatus(UUID patientId, Authorization.AuthorizationStatus status);

    Page<Authorization> findByOrganizationId(UUID organizationId, Pageable pageable);

    Optional<Authorization> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByContractIdAndAuthorizationNumber(UUID contractId, String authorizationNumber);

    @Query("""
        SELECT a FROM Authorization a 
        WHERE a.organizationId = :orgId 
        AND a.status IN ('APPROVED','PARTIAL') 
        AND a.expiryDate IS NOT NULL 
        AND a.expiryDate <= :threshold
        """)
    List<Authorization> findExpiringSoon(@Param("orgId") UUID organizationId, @Param("threshold") OffsetDateTime threshold);

    @Query("""
        SELECT a FROM Authorization a 
        WHERE a.contractId = :contractId 
        AND a.patientId = :patientId 
        AND a.status IN ('APPROVED','PARTIAL')
        AND (a.expiryDate IS NULL OR a.expiryDate > CURRENT_TIMESTAMP)
        ORDER BY a.requestedDate DESC
        """)
    List<Authorization> findValidForPatient(@Param("contractId") UUID contractId, @Param("patientId") UUID patientId);
}