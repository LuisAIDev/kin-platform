package com.kinplatform.kin.medical.institutional;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Branch> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    boolean existsByOrganizationIdAndName(UUID organizationId, String name);
}

