package com.kinplatform.kin.medical.institutional;

import com.kinplatform.common.security.TenantContext;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

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
    private final String email = "medico@clinica.com";

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
        return User.builder().id(userId).email(email).fullName("Medico").build();
    }

    @Test
    void invite_existingUser_setsUserId() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user()));
        when(repository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());
        when(repository.existsByOrganizationIdAndInvitedEmail(orgId, email)).thenReturn(false);
        when(repository.save(any(OrganizationMember.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMember member = service.invite(new OrganizationMemberRequest(email, "IPS_MEDICO", null));

        assertEquals(userId, member.getUserId());
        assertEquals(email, member.getInvitedEmail());
        assertEquals(UserRole.IPS_MEDICO, member.getRole());
        assertEquals(OrganizationMember.MemberStatus.INVITED, member.getStatus());
    }

    @Test
    void invite_emailWithoutAccount_setsNullUserIdAndInvitedEmail() {
        when(userRepository.findByEmail("nuevo@x.com")).thenReturn(Optional.empty());
        when(repository.existsByOrganizationIdAndInvitedEmail(orgId, "nuevo@x.com")).thenReturn(false);
        when(repository.save(any(OrganizationMember.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMember member = service.invite(new OrganizationMemberRequest("Nuevo@X.com", "IPS_MEDICO", null));

        assertNull(member.getUserId());
        assertEquals("nuevo@x.com", member.getInvitedEmail());
        assertEquals(OrganizationMember.MemberStatus.INVITED, member.getStatus());
    }

    @Test
    void invite_duplicateByUserId_throws() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user()));
        when(repository.findByOrganizationIdAndUserId(orgId, userId))
                .thenReturn(Optional.of(OrganizationMember.builder().build()));

        assertThrows(IllegalArgumentException.class,
                () -> service.invite(new OrganizationMemberRequest(email, "IPS_MEDICO", null)));
    }

    @Test
    void invite_duplicateByEmail_throws() {
        when(userRepository.findByEmail("nuevo@x.com")).thenReturn(Optional.empty());
        when(repository.existsByOrganizationIdAndInvitedEmail(orgId, "nuevo@x.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.invite(new OrganizationMemberRequest("nuevo@x.com", "IPS_MEDICO", null)));
    }

    @Test
    void invite_invalidRole_throws() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user()));
        when(repository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty());
        when(repository.existsByOrganizationIdAndInvitedEmail(orgId, email)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.invite(new OrganizationMemberRequest(email, "NO_EXISTE", null)));
    }

    @Test
    void accept_invitedByEmail_linksUserId() {
        UUID id = UUID.randomUUID();
        OrganizationMember member = OrganizationMember.builder()
                .id(id).organizationId(orgId).userId(null).invitedEmail(email).build();
        when(repository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(member));
        when(repository.save(any(OrganizationMember.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMember accepted = service.accept(id, user());

        assertEquals(userId, accepted.getUserId());
        assertEquals(OrganizationMember.MemberStatus.ACTIVE, accepted.getStatus());
        assertNotNull(accepted.getJoinedAt());
    }

    @Test
    void accept_asOtherUser_throwsAccessDenied() {
        UUID id = UUID.randomUUID();
        OrganizationMember member = OrganizationMember.builder()
                .id(id).organizationId(orgId).userId(null).invitedEmail(email).build();
        when(repository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(member));

        User other = User.builder().id(UUID.randomUUID()).email("otro@x.com").build();

        assertThrows(AccessDeniedException.class, () -> service.accept(id, other));
    }
}


