package com.kinplatform.ai;

import com.kinplatform.ai.provider.ProviderRouter;
import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.context.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiEngineServiceTest {

    @Mock
    private ProviderRouter providerRouter;

    private AiEngineService aiEngineService;

    private static final String USER_MSG = "¿Qué opinas de mi proyecto?";
    private static final String SYSTEM_PROMPT = "system prompt resuelto";
    private static final String AI_RESP = "¡Excelente idea! Recomiendo enfocarte en la validación temprana con clientes potenciales.";

    @BeforeEach
    void setUp() {
        aiEngineService = new AiEngineService(providerRouter);
    }

    private AIRequest request(List<Message> history) {
        return new AIRequest(history, USER_MSG, SYSTEM_PROMPT);
    }

    @Test
    void respond_deberiaRetornarRespuestaDelRouter_cuandoRouterResponde() {
        var history = List.of(new Message("USER", "Hola"));
        when(providerRouter.routeBlocking(anyList(), eq(USER_MSG), eq(SYSTEM_PROMPT))).thenReturn(AI_RESP);

        String result = aiEngineService.respond(request(history));

        assertEquals(AI_RESP, result);
        verify(providerRouter).routeBlocking(history, USER_MSG, SYSTEM_PROMPT);
    }

    @Test
    void respond_deberiaRetornarUnavailable_cuandoRouterDevuelveNull() {
        var history = List.of(new Message("USER", "Hola"));
        when(providerRouter.routeBlocking(anyList(), eq(USER_MSG), eq(SYSTEM_PROMPT))).thenReturn(null);

        String result = aiEngineService.respond(request(history));

        assertNotNull(result);
        assertTrue(result.contains("dificultades temporales"));
        verify(providerRouter).routeBlocking(history, USER_MSG, SYSTEM_PROMPT);
    }

    @Test
    void respond_deberiaRetornarUnavailable_cuandoRouterDevuelveVacio() {
        var history = List.of(new Message("USER", "Hola"));
        when(providerRouter.routeBlocking(anyList(), eq(USER_MSG), eq(SYSTEM_PROMPT))).thenReturn("");

        String result = aiEngineService.respond(request(history));

        assertNotNull(result);
        assertTrue(result.contains("dificultades temporales"));
        verify(providerRouter).routeBlocking(history, USER_MSG, SYSTEM_PROMPT);
    }

    @Test
    void respondStream_deberiaEmitirTokensDelRouter_cuandoRouterResponde() {
        var history = List.of(new Message("USER", "Hola"));
        when(providerRouter.routeStream(anyList(), eq(USER_MSG), eq(SYSTEM_PROMPT)))
            .thenReturn(Flux.just("Token 1", "Token 2"));

        Flux<String> result = aiEngineService.respondStream(request(history));

        StepVerifier.create(result)
            .expectNext("Token 1")
            .expectNext("Token 2")
            .verifyComplete();
        verify(providerRouter).routeStream(history, USER_MSG, SYSTEM_PROMPT);
    }

    @Test
    void respondStream_deberiaEmitirUnavailable_cuandoRouterDevuelveVacio() {
        var history = List.<Message>of();
        when(providerRouter.routeStream(anyList(), eq(USER_MSG), eq(SYSTEM_PROMPT)))
            .thenReturn(Flux.empty());

        Flux<String> result = aiEngineService.respondStream(request(history));

        StepVerifier.create(result)
            .expectNextMatches(s -> s.contains("dificultades temporales"))
            .verifyComplete();
        verify(providerRouter).routeStream(history, USER_MSG, SYSTEM_PROMPT);
    }
}
