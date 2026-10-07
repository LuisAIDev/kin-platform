package com.kinplatform.kin.medical.institutional;

import com.kinplatform.common.security.TenantContext;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationMemberService {

    private final OrganizationMemberRepository repository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    public List<OrganizationMember> findAll() {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(TenantContext.get());
    }

    public List<OrganizationMember> findByBranch(UUID branchId) {
        return repository.findByBranchIdAndOrganizationId(branchId, TenantContext.get());
    }

    @Transactional
    public OrganizationMember invite(OrganizationMemberRequest request) {
        UUID organizationId = TenantContext.get();
        String email = request.email().trim().toLowerCase();
        User invitee = userRepository.findByEmail(email).orElse(null);

        if (invitee != null
                && repository.findByOrganizationIdAndUserId(organizationId, invitee.getId()).isPresent()) {
            throw new IllegalArgumentException("El usuario ya pertenece a la organizacion");
        }
        if (repository.existsByOrganizationIdAndInvitedEmail(organizationId, email)) {
            throw new IllegalArgumentException("Ya existe una invitacion para ese email");
        }
        UUID branchId = validateBranch(request.branchId(), organizationId);
        OrganizationMember member = OrganizationMember.builder()
                .organizationId(organizationId)
                .userId(invitee != null ? invitee.getId() : null)
                .invitedEmail(email)
                .branchId(branchId)
                .role(parseRole(request.role()))
                .status(OrganizationMember.MemberStatus.INVITED)
                .invitedAt(OffsetDateTime.now())
                .build();
        return repository.save(member);
    }

    @Transactional
    public OrganizationMember accept(UUID id, User currentUser) {
        OrganizationMember member = get(id);
        boolean isOwner = (member.getUserId() != null && member.getUserId().equals(currentUser.getId()))
                || (member.getInvitedEmail() != null
                        && member.getInvitedEmail().equalsIgnoreCase(currentUser.getEmail()));
        if (!isOwner) {
            throw new AccessDeniedException("No puedes aceptar una invitacion ajena");
        }
        member.setUserId(currentUser.getId());
        member.setStatus(OrganizationMember.MemberStatus.ACTIVE);
        member.setJoinedAt(OffsetDateTime.now());
        return repository.save(member);
    }

    @Transactional
    public OrganizationMember assignBranch(UUID id, UUID branchId) {
        OrganizationMember member = get(id);
        member.setBranchId(validateBranch(branchId, member.getOrganizationId()));
        return repository.save(member);
    }

    @Transactional
    public OrganizationMember remove(UUID id) {
        OrganizationMember member = get(id);
        member.setStatus(OrganizationMember.MemberStatus.REMOVED);
        return repository.save(member);
    }

    public OrganizationMember get(UUID id) {
        return repository.findByIdAndOrganizationId(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Miembro no encontrado: " + id));
    }

    private UUID validateBranch(UUID branchId, UUID organizationId) {
        if (branchId == null) {
            return null;
        }
        branchRepository.findByIdAndOrganizationId(branchId, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada: " + branchId));
        return branchId;
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role);
        } catch (Exception e) {
            throw new IllegalArgumentException("Rol institucional invalido: " + role);
        }
    }
}


