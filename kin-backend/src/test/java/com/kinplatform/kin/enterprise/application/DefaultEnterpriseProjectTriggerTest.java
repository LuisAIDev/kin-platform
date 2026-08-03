package com.kinplatform.kin.enterprise.application;

import com.kinplatform.kin.enterprise.aggregate.EnterpriseProject;
import com.kinplatform.kin.enterprise.events.EnterpriseProjectRequested;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultEnterpriseProjectTriggerTest {

    private final InMemoryEnterpriseProjectRepository repository = new InMemoryEnterpriseProjectRepository();
    private final InMemoryDomainEventBus eventBus = new InMemoryDomainEventBus();

    private DefaultEnterpriseProjectTrigger trigger() {
        return new DefaultEnterpriseProjectTrigger(repository, eventBus);
    }

    @Test
    void requestSinVersiones_publicaSolicitudVersion1() {
        var projectId = UUID.randomUUID();

        trigger().request(projectId);

        var events = eventBus.publishedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof EnterpriseProjectRequested requested
            && requested.projectId().equals(projectId) && requested.version() == 1);
    }

    @Test
    void requestDesdeCompletado_publicaSolicitudVersionSiguiente() {
        var projectId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        repository.save(EnterpriseProject.complete(projectId, 1, now, now, now, List.of()));

        trigger().request(projectId);

        var events = eventBus.publishedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof EnterpriseProjectRequested requested
            && requested.projectId().equals(projectId) && requested.version() == 2);
    }

    @Test
    void requestDesdeFallido_publicaSolicitudVersionSiguiente() {
        var projectId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        repository.save(EnterpriseProject.fail(projectId, 1, now, now, "motivo", List.of()));

        trigger().request(projectId);

        var events = eventBus.publishedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof EnterpriseProjectRequested requested
            && requested.version() == 2);
    }

    @Test
    void requestConRequestedEnVuelo_noPublicaNada() {
        var projectId = UUID.randomUUID();
        repository.save(EnterpriseProject.request(projectId, 1));

        trigger().request(projectId);

        assertTrue(eventBus.publishedEvents().isEmpty());
    }

    @Test
    void requestConRunningEnVuelo_noPublicaNada() {
        var projectId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        repository.save(EnterpriseProject.start(projectId, 1, now, now, List.of()));

        trigger().request(projectId);

        assertTrue(eventBus.publishedEvents().isEmpty());
    }

    @Test
    void requestConProjectIdNulo_lanza() {
        assertThrows(IllegalArgumentException.class, () -> trigger().request(null));
    }

    @Test
    void constructorConDependenciaNula_lanza() {
        assertThrows(IllegalArgumentException.class,
            () -> new DefaultEnterpriseProjectTrigger(null, eventBus));
        assertThrows(IllegalArgumentException.class,
            () -> new DefaultEnterpriseProjectTrigger(repository, null));
    }

    @Test
    void requestConVersionesMultiples_solicitaSobreLaUltima() {
        var projectId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        repository.save(EnterpriseProject.complete(projectId, 1, now, now, now, List.of()));
        repository.save(EnterpriseProject.complete(projectId, 2, now, now, now, List.of()));

        trigger().request(projectId);

        var events = eventBus.publishedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof EnterpriseProjectRequested requested
            && requested.version() == 3);
    }
}
