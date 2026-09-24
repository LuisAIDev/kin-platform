package com.kinplatform.institutional;

import com.kinplatform.common.security.TenantContext;
import com.kinplatform.user.UserRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

    public List<OrganizationMember> findAll() {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(TenantContext.get());
    }

    public List<OrganizationMember> findByBranch(UUID branchId) {
        return repository.findByBranchIdAndOrganizationId(branchId, TenantContext.get());
    }

    @Transactional
    public OrganizationMember invite(InviteRequest request) {
        UUID organizationId = TenantContext.get();
        if (repository.findByOrganizationIdAndUserId(organizationId, request.userId()).isPresent()) {
            throw new IllegalArgumentException("El usuario ya pertenece a la organizacion");
        }
        UUID branchId = validateBranch(request.branchId(), organizationId);
        OrganizationMember member = OrganizationMember.builder()
                .organizationId(organizationId)
                .userId(request.userId())
                .branchId(branchId)
                .role(parseRole(request.role()))
                .status(OrganizationMember.MemberStatus.INVITED)
                .invitedAt(OffsetDateTime.now())
                .build();
        return repository.save(member);
    }

    @Transactional
    public OrganizationMember accept(UUID id) {
        OrganizationMember member = get(id);
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

    public record InviteRequest(UUID userId, String role, UUID branchId) {
    }
}
