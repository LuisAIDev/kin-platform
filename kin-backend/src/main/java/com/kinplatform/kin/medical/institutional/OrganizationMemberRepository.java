package com.kinplatform.kin.medical.institutional;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    Optional<OrganizationMember> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndInvitedEmail(UUID organizationId, String invitedEmail);

    List<OrganizationMember> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<OrganizationMember> findByBranchIdAndOrganizationId(UUID branchId, UUID organizationId);

    long countByOrganizationIdAndRole(UUID organizationId, com.kinplatform.common.user.UserRole role);
}


