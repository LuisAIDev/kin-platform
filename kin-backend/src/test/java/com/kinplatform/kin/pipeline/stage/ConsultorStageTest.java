package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.ai.PromptAssembler;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultorStageTest {

    @Mock
    private AIResponder aiResponder;

    private final PromptAssembler promptAssembler = new PromptAssembler();
    private ConsultorStage stage;

    @BeforeEach
    void setUp() {
        stage = new ConsultorStage(aiResponder, promptAssembler);
    }

    private PipelineContext context(boolean streaming) {
        var ctx = new PipelineContext(
            UUID.randomUUID(), UUID.randomUUID(), "hola", List.of(),
            "Mi App", "App de gestión", "Software");
        ctx.projectContext(ProjectContext.fromProject("Mi App", "App de gestión", "Software"));
        ctx.streaming(streaming);
        return ctx;
    }

    @Test
    void name_deberiaSerConsultor() {
        assertEquals("Consultor", stage.name());
    }

    @Test
    void supports_deberiaSerSiempreVerdadero() {
        assertTrue(stage.supports(context(false)));
    }

    @Test
    void execute_bloqueante_deberiaEscribirLaRespuestaYNoUsarStreaming() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("respuesta de KIN");

        var ctx = context(false);
        var result = stage.execute(ctx);

        assertSame(ctx, result);
        assertEquals("respuesta de KIN", ctx.aiResponse());
        assertNull(ctx.aiResponseFlux());
        verify(aiResponder, never()).respondStream(any());
    }

    @Test
    void execute_streaming_deberiaGuardarElFluxSinBloquear() {
        when(aiResponder.respondStream(any(AIRequest.class))).thenReturn(Flux.just("a", "b"));

        var ctx = context(true);
        var result = stage.execute(ctx);

        assertSame(ctx, result);
        assertNull(ctx.aiResponse());
        assertNotNull(ctx.aiResponseFlux());
        verify(aiResponder, never()).respond(any());
    }

    @Test
    void execute_deberiaEnviarElPromptEnsambladoAlResponder() {
        when(aiResponder.respond(any(AIRequest.class))).thenReturn("ok");

        var ctx = context(false);
        stage.execute(ctx);

        var captor = ArgumentCaptor.forClass(AIRequest.class);
        verify(aiResponder).respond(captor.capture());
        var request = captor.getValue();
        assertEquals("hola", request.userMessage());
        assertTrue(request.systemPrompt().contains("Mi App"));
        assertTrue(request.systemPrompt().contains("App de gestión"));
        assertTrue(request.history().isEmpty());
    }
}
