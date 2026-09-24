package com.kinplatform.institutional;

import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BranchServiceTest {

    private BranchRepository repository;
    private BranchService service;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(BranchRepository.class);
        service = new BranchService(repository);
        TenantContext.set(orgId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_setsTenantAndSaves() {
        when(repository.existsByOrganizationIdAndName(orgId, "Sede Norte")).thenReturn(false);
        when(repository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

        Branch branch = service.create(new BranchService.BranchRequest(
                "Sede Norte", "Calle 1", "300", "Bogota", "[\"consulta\"]"));

        assertEquals(orgId, branch.getOrganizationId());
        assertEquals("Sede Norte", branch.getName());
        verify(repository).save(any(Branch.class));
    }

    @Test
    void create_duplicateName_throws() {
        when(repository.existsByOrganizationIdAndName(orgId, "Sede Norte")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(
                new BranchService.BranchRequest("Sede Norte", null, null, null, null)));
    }

    @Test
    void findAll_delegatesToTenantRepository() {
        when(repository.findByOrganizationIdOrderByNameAsc(orgId)).thenReturn(List.of());
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void get_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.get(id));
    }

    @Test
    void update_changesProvidedFields() {
        UUID id = UUID.randomUUID();
        Branch branch = Branch.builder().id(id).organizationId(orgId).name("Sede Norte").build();
        when(repository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(branch));
        when(repository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

        Branch updated = service.update(id, new BranchService.BranchRequest(null, "Calle 2", null, "Medellin", null));

        assertEquals("Calle 2", updated.getAddress());
        assertEquals("Medellin", updated.getCity());
        assertEquals("Sede Norte", updated.getName());
    }
}
