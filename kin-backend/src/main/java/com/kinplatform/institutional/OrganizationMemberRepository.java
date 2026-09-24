package com.kinplatform.institutional;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    Optional<OrganizationMember> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    List<OrganizationMember> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<OrganizationMember> findByBranchIdAndOrganizationId(UUID branchId, UUID organizationId);

    long countByOrganizationIdAndRole(UUID organizationId, com.kinplatform.user.UserRole role);
}
