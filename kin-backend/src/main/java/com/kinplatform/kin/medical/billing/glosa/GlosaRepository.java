package com.kinplatform.kin.medical.billing.glosa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GlosaRepository extends JpaRepository<Glosa, UUID> {

    Optional<Glosa> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Glosa> findByOrganizationId(UUID organizationId);

    List<Glosa> findByOrganizationIdAndStatus(UUID organizationId, Glosa.GlosaStatus status);

    List<Glosa> findByOrganizationIdAndContractId(UUID organizationId, UUID contractId);

    long countByOrganizationIdAndStatus(UUID organizationId, Glosa.GlosaStatus status);

    List<Glosa> findByStatusAndAppealDeadlineBefore(Glosa.GlosaStatus status, OffsetDateTime deadline);
}

