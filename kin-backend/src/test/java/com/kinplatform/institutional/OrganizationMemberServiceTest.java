package com.kinplatform.institutional;

import com.kinplatform.common.security.TenantContext;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizationMemberServiceTest {

    private OrganizationMemberRepository repository;
    private BranchRepository branchRepository;
    private OrganizationMemberService service;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(OrganizationMemberRepository.class);
        branchRepository = mock(BranchRepository.class);
        service = new OrganizationMemberService(repository, branchRepository);
        TenantContext.set(orgId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void invite_createsInvitedMember() {
        UUID userId = UUID.randomUUID();
        when(repository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());
        when(repository.save(any(OrganizationMember.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMember member = service.invite(new OrganizationMemberService.InviteRequest(
                userId, "IPS_MEDICO", null));

        assertEquals(orgId, member.getOrganizationId());
        assertEquals(UserRole.IPS_MEDICO, member.getRole());
        assertEquals(OrganizationMember.MemberStatus.INVITED, member.getStatus());
        assertNotNull(member.getInvitedAt());
    }

    @Test
    void invite_duplicateUser_throws() {
        UUID userId = UUID.randomUUID();
        when(repository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(OrganizationMember.builder().build()));

        assertThrows(IllegalArgumentException.class, () -> service.invite(
                new OrganizationMemberService.InviteRequest(userId, "IPS_MEDICO", null)));
    }

    @Test
    void invite_invalidBranch_throws() {
        UUID userId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(repository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());
        when(branchRepository.findByIdAndOrganizationId(branchId, orgId)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.invite(
                new OrganizationMemberService.InviteRequest(userId, "IPS_MEDICO", branchId)));
    }

    @Test
    void invite_invalidRole_throws() {
        UUID userId = UUID.randomUUID();
        when(repository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.invite(
                new OrganizationMemberService.InviteRequest(userId, "NO_EXISTE", null)));
    }

    @Test
    void accept_setsActiveAndJoinedAt() {
        UUID id = UUID.randomUUID();
        OrganizationMember member = OrganizationMember.builder().id(id).organizationId(orgId).build();
        when(repository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(member));
        when(repository.save(any(OrganizationMember.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMember accepted = service.accept(id);

        assertEquals(OrganizationMember.MemberStatus.ACTIVE, accepted.getStatus());
        assertNotNull(accepted.getJoinedAt());
    }

    @Test
    void assignBranch_validatesBranchTenant() {
        UUID id = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        OrganizationMember member = OrganizationMember.builder().id(id).organizationId(orgId).build();
        when(repository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(member));
        when(branchRepository.findByIdAndOrganizationId(branchId, orgId))
                .thenReturn(Optional.of(Branch.builder().id(branchId).organizationId(orgId).build()));
        when(repository.save(any(OrganizationMember.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMember result = service.assignBranch(id, branchId);

        assertEquals(branchId, result.getBranchId());
    }
}
