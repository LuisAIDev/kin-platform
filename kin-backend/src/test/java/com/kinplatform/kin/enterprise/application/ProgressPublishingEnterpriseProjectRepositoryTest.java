package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.progress.EnterpriseProgressPublisher;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgressPublishingEnterpriseProjectRepositoryTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private final InMemoryEnterpriseProjectRepository delegate = new InMemoryEnterpriseProjectRepository();
    private final EnterpriseProgressPublisher publisher = mock(EnterpriseProgressPublisher.class);

    private ProgressPublishingEnterpriseProjectRepository repository() {
        return new ProgressPublishingEnterpriseProjectRepository(delegate, publisher);
    }

    @Test
    void save_deberiaDelegarYPublicarProgreso() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.LEAN_CANVAS);
        var repo = repository();

        var result = repo.save(project);

        assertSame(project, result);
        verify(publisher).publishFor(project);
    }

    @Test
    void save_deberiaDelegarAlRepositorioReal() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.KPI);

        repository().save(project);

        assertTrue(delegate.findLatestVersion(PROJECT_ID).isPresent());
    }

    @Test
    void lecturas_deberianDelegarSinPublicar() {
        var project = ExportTestFixtures.project(PROJECT_ID, 1, DocumentType.KPI);
        var repo = repository();
        repo.save(project);
        org.mockito.Mockito.clearInvocations(publisher);

        assertEquals(1, repo.findLatestVersion(PROJECT_ID).orElseThrow().version());
        assertEquals(1, repo.findByVersion(PROJECT_ID, 1).orElseThrow().version());
        assertEquals(1, repo.findAllVersions(PROJECT_ID).size());
        verify(publisher, never()).publishFor(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findLatestVersion_sinVersion_deberiaDevolverVacio() {
        assertTrue(repository().findLatestVersion(PROJECT_ID).isEmpty());
    }

    @Test
    void constructor_conDependenciaNula_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new ProgressPublishingEnterpriseProjectRepository(null, publisher));
        assertThrows(IllegalArgumentException.class,
            () -> new ProgressPublishingEnterpriseProjectRepository(delegate, null));
    }
}
