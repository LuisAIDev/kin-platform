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
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KinMethodFallbackTest {

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

    private KinMethod kinMethod(ResponseFallback fallback) {
        return new KinMethod(pipeline, eventBus, contextRepository, fallback);
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
    void validacionRechazada_deberiaAnexarRespuestaSegura() {
        stubPipeline(Flux.just("token"), ResponseValidation.rejected(List.of("issue")));

        var flux = kinMethod(new ResponseFallback(List.of("respuesta segura"), 0)).executeStream(command());

        StepVerifier.create(flux)
            .expectNext("token", ResponseFallback.DEFAULT_CANNED_RESPONSE + " Motivo: issue.")
            .verifyComplete();
    }

    @Test
    void validacionAceptada_deberiaConservarLosTokens() {
        stubPipeline(Flux.just("a", "b"), ResponseValidation.ok());

        var flux = kinMethod(new ResponseFallback(List.of("respuesta segura"), 0)).executeStream(command());

        StepVerifier.create(flux).expectNext("a", "b").verifyComplete();
    }

    @Test
    void validacionNula_deberiaConservarLosTokens() {
        stubPipeline(Flux.just("a"), null);

        var flux = kinMethod(new ResponseFallback(List.of("respuesta segura"), 0)).executeStream(command());

        StepVerifier.create(flux).expectNext("a").verifyComplete();
    }

    @Test
    void fluxNulo_deberiaDevolverNull() {
        when(pipeline.execute(any(PipelineContext.class))).thenAnswer(inv -> inv.getArgument(0));

        assertNull(kinMethod(new ResponseFallback(List.of("respuesta segura"), 0)).executeStream(command()));
    }
}
