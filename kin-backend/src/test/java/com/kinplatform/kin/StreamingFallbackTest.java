package com.kinplatform.kin;

import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.conversation.ResponseFallback;
import com.kinplatform.kin.conversation.ResponseValidation;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.pipeline.Pipeline;
import com.kinplatform.kin.pipeline.PipelineContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Valida que el flujo streaming no rompe el contrato SSE (el flux completa sin
 * error) y que, ante una validación rechazada (reintentos agotados), la
 * respuesta segura del {@link ResponseFallback} llega como contenido final.
 */
@ExtendWith(MockitoExtension.class)
class StreamingFallbackTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private Pipeline pipeline;

    @Mock
    private DomainEventBus eventBus;

    @Mock
    private ContextRepository contextRepository;

    private KinMethodCommand command() {
        return new KinMethodCommand(PROJECT_ID, USER_ID, "m", List.of(), "t", "d", "c");
    }

    private KinMethod kinMethod() {
        return new KinMethod(pipeline, eventBus, contextRepository,
            new ResponseFallback(List.of("respuesta segura"), 0));
    }

    private void stubPipeline(Flux<String> flux, ResponseValidation validation) {
        when(pipeline.execute(any(PipelineContext.class))).thenAnswer(inv -> {
            PipelineContext ctx = inv.getArgument(0);
            ctx.streaming(true);
            ctx.aiResponseFlux(flux);
            ctx.responseValidation(validation);
            return ctx;
        });
    }

    @Test
    void streamConValidacionRechazada_porLongitud_deberiaConservarLosTokensEntregados() {
        stubPipeline(Flux.just("t1", "t2"), ResponseValidation.rejected(List.of("response.too_long")));

        String content = kinMethod().executeStream(command())
            .reduce("", (acc, next) -> acc + next)
            .block();

        assertEquals("t1t2", content);
        assertFalse(content.contains(ResponseFallback.DEFAULT_CANNED_RESPONSE));
        assertFalse(content.contains("response.too_long"));
    }

    @Test
    void streamConValidacionRechazada_dura_deberiaCompletarConRespuestaSegura() {
        stubPipeline(Flux.just("t1", "t2"), ResponseValidation.rejected(List.of("response.multiple_questions")));

        String expected = "t1t2" + "respuesta segura";
        String content = kinMethod().executeStream(command())
            .reduce("", (acc, next) -> acc + next)
            .block();

        assertEquals(expected, content);
    }

    @Test
    void streamConValidacionAceptada_deberiaCompletarConLosTokens() {
        stubPipeline(Flux.just("a", "b"), ResponseValidation.ok());

        String content = kinMethod().executeStream(command())
            .reduce("", (acc, next) -> acc + next)
            .block();

        assertEquals("ab", content);
    }

    @Test
    void streamSinValidacion_deberiaCompletarSinAlterarElContenido() {
        stubPipeline(Flux.just("x"), null);

        String content = kinMethod().executeStream(command())
            .reduce("", (acc, next) -> acc + next)
            .block();

        assertEquals("x", content);
    }
}
