package com.kinplatform.institutional;

import com.kinplatform.common.security.TenantContext;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizationMemberServiceTest {

    private OrganizationMemberRepository repository;
    private BranchRepository branchRepository;
    private UserRepository userRepository;
    private OrganizationMemberService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(OrganizationMemberRepository.class);
        branchRepository = mock(BranchRepository.class);
        userRepository = mock(UserRepository.class);
        service = new OrganizationMemberService(repository, branchRepository, userRepository);
        TenantContext.set(orgId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private User user() {
        return User.builder().id(userId).email("medico@clinica.com").fullName("Medico").build();
    }

    @Test
    void invite_byEmail_createsInvitedMember() {
        when(userRepository.findByEmail("medico@clinica.com")).thenReturn(Optional.of(user()));
        when(repository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());
        when(repository.save(any(OrganizationMember.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMember member = service.invite(new OrganizationMemberRequest(
                "Medico@Clinica.com", "IPS_MEDICO", null));

        assertEquals(orgId, member.getOrganizationId());
        assertEquals(userId, member.getUserId());
        assertEquals(UserRole.IPS_MEDICO, member.getRole());
        assertEquals(OrganizationMember.MemberStatus.INVITED, member.getStatus());
        assertNotNull(member.getInvitedAt());
    }

    @Test
    void invite_userNotFound_throws() {
        when(userRepository.findByEmail("nadie@x.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.invite(
                new OrganizationMemberRequest("nadie@x.com", "IPS_MEDICO", null)));
    }

    @Test
    void invite_duplicateUser_throws() {
        when(userRepository.findByEmail("medico@clinica.com")).thenReturn(Optional.of(user()));
        when(repository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(OrganizationMember.builder().build()));

        assertThrows(IllegalArgumentException.class, () -> service.invite(
                new OrganizationMemberRequest("medico@clinica.com", "IPS_MEDICO", null)));
    }

    @Test
    void invite_invalidRole_throws() {
        when(userRepository.findByEmail("medico@clinica.com")).thenReturn(Optional.of(user()));
        when(repository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.invite(
                new OrganizationMemberRequest("medico@clinica.com", "NO_EXISTE", null)));
    }

    @Test
    void invite_invalidBranch_throws() {
        UUID branchId = UUID.randomUUID();
        when(userRepository.findByEmail("medico@clinica.com")).thenReturn(Optional.of(user()));
        when(repository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());
        when(branchRepository.findByIdAndOrganizationId(branchId, orgId)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.invite(
                new OrganizationMemberRequest("medico@clinica.com", "IPS_MEDICO", branchId)));
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

        assertEquals(branchId, service.assignBranch(id, branchId).getBranchId());
    }
}
