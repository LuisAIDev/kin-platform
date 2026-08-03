package com.kinplatform.ai.enterprise.adapter;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.ports.EnterpriseProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseProjectRepositoryAdapterTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private EnterpriseProjectJpaRepository repository;

    private EnterpriseProjectRepositoryAdapter adapter() {
        return new EnterpriseProjectRepositoryAdapter(repository);
    }

    @Test
    void save_deberiaPersistirYDevolverElAggregate() {
        var project = EnterprisePersistenceTestFixtures.completed(PROJECT_ID, 1);
        when(repository.saveAndFlush(any(EnterpriseProjectEntity.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        var result = adapter().save(project);

        var captor = ArgumentCaptor.forClass(EnterpriseProjectEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        var entity = captor.getValue();
        assertEquals(PROJECT_ID, entity.getProjectId());
        assertEquals(1, entity.getVersion());
        assertEquals("COMPLETED", entity.getStatus());
        assertEquals(3, entity.getDocuments().size());
        assertEquals(project.projectId(), result.projectId());
        assertEquals(project.version(), result.version());
        assertEquals(3, result.documentCount());
    }

    @Test
    void save_conProyectoNulo_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class, () -> adapter().save(null));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void findLatestVersion_deberiaDevolverLaUltimaVersion() {
        var entity = new EnterpriseProjectEntity();
        entity.setProjectId(PROJECT_ID);
        entity.setVersion(2);
        entity.setStatus("COMPLETED");
        var now = java.time.OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCompletedAt(now);
        when(repository.findTopByProjectIdOrderByVersionDesc(PROJECT_ID)).thenReturn(Optional.of(entity));

        var result = adapter().findLatestVersion(PROJECT_ID);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().version());
        assertTrue(result.get().isCompleted());
    }

    @Test
    void findLatestVersion_sinProyecto_deberiaDevolverVacio() {
        when(repository.findTopByProjectIdOrderByVersionDesc(PROJECT_ID)).thenReturn(Optional.empty());

        assertTrue(adapter().findLatestVersion(PROJECT_ID).isEmpty());
    }

    @Test
    void findLatestVersion_conProjectIdNull_deberiaDevolverVacio() {
        assertTrue(adapter().findLatestVersion(null).isEmpty());
        verify(repository, never()).findTopByProjectIdOrderByVersionDesc(any());
    }

    @Test
    void findByVersion_deberiaDevolverLaVersionSolicitada() {
        var entity = new EnterpriseProjectEntity();
        entity.setProjectId(PROJECT_ID);
        entity.setVersion(3);
        entity.setStatus("FAILED");
        var now = java.time.OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setFailedReason("motivo");
        when(repository.findByProjectIdAndVersion(PROJECT_ID, 3)).thenReturn(Optional.of(entity));

        var result = adapter().findByVersion(PROJECT_ID, 3);

        assertTrue(result.isPresent());
        assertEquals(3, result.get().version());
        assertEquals("motivo", result.get().failedReason());
    }

    @Test
    void findByVersion_conProjectIdNull_deberiaDevolverVacio() {
        assertTrue(adapter().findByVersion(null, 1).isEmpty());
        verify(repository, never()).findByProjectIdAndVersion(any(), anyInt());
    }

    @Test
    void findAllVersions_deberiaDevolverLasVersionesOrdenadas() {
        var now = java.time.OffsetDateTime.now();
        var v1 = new EnterpriseProjectEntity();
        v1.setProjectId(PROJECT_ID);
        v1.setVersion(1);
        v1.setStatus("COMPLETED");
        v1.setCreatedAt(now);
        v1.setUpdatedAt(now);
        v1.setCompletedAt(now);
        var v2 = new EnterpriseProjectEntity();
        v2.setProjectId(PROJECT_ID);
        v2.setVersion(2);
        v2.setStatus("FAILED");
        v2.setCreatedAt(now);
        v2.setUpdatedAt(now);
        v2.setFailedReason("motivo");
        when(repository.findByProjectIdOrderByVersionAsc(PROJECT_ID)).thenReturn(List.of(v1, v2));

        var result = adapter().findAllVersions(PROJECT_ID);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).version());
        assertEquals(2, result.get(1).version());
    }

    @Test
    void findAllVersions_sinVersion_deberiaDevolverListaVacia() {
        when(repository.findByProjectIdOrderByVersionAsc(PROJECT_ID)).thenReturn(List.of());

        assertTrue(adapter().findAllVersions(PROJECT_ID).isEmpty());
    }

    @Test
    void findAllVersions_conProjectIdNull_deberiaDevolverListaVacia() {
        assertTrue(adapter().findAllVersions(null).isEmpty());
        verify(repository, never()).findByProjectIdOrderByVersionAsc(any());
    }

    @Test
    void constructor_conDependenciaNula_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProjectRepositoryAdapter(null));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProjectRepositoryAdapter(repository, null));
    }

    @Test
    void contrato_deberiaImplementarElPuerto() {
        assertTrue(EnterpriseProjectRepository.class.isAssignableFrom(
            EnterpriseProjectRepositoryAdapter.class));
        assertFalse(EnterpriseProjectRepositoryAdapter.class.isInterface());
    }
}
