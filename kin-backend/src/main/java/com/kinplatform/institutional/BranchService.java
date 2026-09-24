package com.kinplatform.institutional;

import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository repository;

    public List<Branch> findAll() {
        return repository.findByOrganizationIdOrderByNameAsc(TenantContext.get());
    }

    public Branch get(UUID id) {
        return repository.findByIdAndOrganizationId(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Sede no encontrada: " + id));
    }

    @Transactional
    public Branch create(BranchRequest request) {
        UUID organizationId = TenantContext.get();
        if (repository.existsByOrganizationIdAndName(organizationId, request.name())) {
            throw new IllegalArgumentException("Ya existe una sede con el nombre: " + request.name());
        }
        Branch branch = Branch.builder()
                .organizationId(organizationId)
                .name(request.name())
                .address(request.address())
                .phone(request.phone())
                .city(request.city())
                .servicesEnabled(request.servicesEnabled())
                .build();
        return repository.save(branch);
    }

    @Transactional
    public Branch update(UUID id, BranchRequest request) {
        Branch branch = get(id);
        if (request.name() != null && !request.name().equals(branch.getName())) {
            if (repository.existsByOrganizationIdAndName(branch.getOrganizationId(), request.name())) {
                throw new IllegalArgumentException("Ya existe una sede con el nombre: " + request.name());
            }
            branch.setName(request.name());
        }
        if (request.address() != null) branch.setAddress(request.address());
        if (request.phone() != null) branch.setPhone(request.phone());
        if (request.city() != null) branch.setCity(request.city());
        if (request.servicesEnabled() != null) branch.setServicesEnabled(request.servicesEnabled());
        return repository.save(branch);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(get(id));
    }

    public record BranchRequest(
            String name,
            String address,
            String phone,
            String city,
            String servicesEnabled
    ) {
    }
}
