package com.kinplatform.kin.enterprise.web;

import com.kinplatform.kin.enterprise.application.InMemoryEnterpriseProjectRepository;
import com.kinplatform.kin.enterprise.progress.EnterpriseProgressPublisher;
import com.kinplatform.kin.enterprise.valueobjects.DocumentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EnterpriseProgressControllerTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    private ScheduledExecutorService executor;
    private EnterpriseProgressService service;
    private EnterpriseProgressPublisher publisher;
    private InMemoryEnterpriseProjectRepository repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "test-progress");
            thread.setDaemon(true);
            return thread;
        });
        service = new EnterpriseProgressService(executor, 60_000L);
        publisher = new EnterpriseProgressPublisher(service);
        repository = new InMemoryEnterpriseProjectRepository();
        var controller = new EnterpriseProgressController(service, publisher, repository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new EnterpriseApiExceptionHandler())
            .build();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void stream_conVersionCompletada_deberiaEmitirElEstadoTerminal() throws Exception {
        repository.save(com.kinplatform.kin.enterprise.web.WebTestFixtures.completed(
            PROJECT_ID, 1, DocumentType.LEAN_CANVAS, DocumentType.KPI));

        MvcResult result = mockMvc.perform(get("/enterprise/{projectId}/1/stream", PROJECT_ID)
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andReturn();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .asyncDispatch(result))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("event:progress")))
            .andExpect(content().string(containsString("COMPLETED")))
            .andExpect(content().string(containsString("LEAN_CANVAS")));
        assertEquals(0, service.activeSubscriptionCount());
    }

    @Test
    void stream_deberiaEmitirEventosEnVivoDespuesDeSuscribirse() throws Exception {
        MvcResult result = mockMvc.perform(get("/enterprise/{projectId}/1/stream", PROJECT_ID)
                .accept(MediaType.TEXT_EVENT_STREAM))
            .andReturn();

        publisher.publishFor(com.kinplatform.kin.enterprise.web.WebTestFixtures.completed(
            PROJECT_ID, 1, DocumentType.KPI));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .asyncDispatch(result))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("KPI")))
            .andExpect(content().string(containsString("COMPLETED")));
    }

    @Test
    void stream_conVersionInvalida_deberiaLanzarErrorDeValidacion() {
        // El advice global mapea IllegalArgumentException a 400 en el contexto
        // Spring completo; en MockMvc standalone el emisor SSE no puede escribir
        // la respuesta de error y la excepción se propaga como ServletException.
        org.junit.jupiter.api.Assertions.assertThrows(jakarta.servlet.ServletException.class,
            () -> mockMvc.perform(get("/enterprise/{projectId}/0/stream", PROJECT_ID)
                .accept(MediaType.TEXT_EVENT_STREAM)));
    }

    @Test
    void constructor_conDependenciaNula_deberiaLanzar() {
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProgressController(null, publisher, repository));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProgressController(service, null, repository));
        assertThrows(IllegalArgumentException.class,
            () -> new EnterpriseProgressController(service, publisher, null));
    }
}
