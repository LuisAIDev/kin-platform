package com.kinplatform.ai;

import com.kinplatform.ai.provider.ProviderRouter;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiEngineServiceTest {

    @Mock
    private ProviderRouter providerRouter;

    private AiEngineService aiEngineService;

    private static final String USER_MSG = "\u00BFQu\u00E9 opinas de mi proyecto?";
    private static final String TITLE = "Mi App";
    private static final String DESC = "App para gestionar tareas";
    private static final String CAT = "EMPRENDIMIENTO";
    private static final String AI_RESP = "\u00A1Excelente idea! Recomiendo enfocarte en la validaci\u00F3n temprana con clientes potenciales.";

    @BeforeEach
    void setUp() {
        aiEngineService = new AiEngineService(providerRouter);
    }

    @Test
    void generateAiResponse_deberiaRetornarRespuestaDelRouter_cuandoRouterResponde() {
        when(providerRouter.routeBlocking(anyList(), eq(USER_MSG), anyString())).thenReturn(AI_RESP);

        var history = List.of(
                new Message("USER", "Hola")
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertEquals(AI_RESP, result);
        verify(providerRouter).routeBlocking(anyList(), eq(USER_MSG), anyString());
    }

    @Test
    void generateAiResponse_deberiaRetornarUnavailable_cuandoRouterDevuelveNull() {
        when(providerRouter.routeBlocking(anyList(), eq(USER_MSG), anyString())).thenReturn(null);

        var history = List.of(
                new Message("USER", "Hola")
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertNotNull(result);
        assertTrue(result.contains("dificultades temporales"));
        verify(providerRouter).routeBlocking(anyList(), eq(USER_MSG), anyString());
    }

    @Test
    void generateAiResponse_deberiaRetornarUnavailable_cuandoRouterDevuelveVacio() {
        when(providerRouter.routeBlocking(anyList(), eq(USER_MSG), anyString())).thenReturn("");

        var history = List.of(
                new Message("USER", "Hola")
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertNotNull(result);
        assertTrue(result.contains("dificultades temporales"));
        verify(providerRouter).routeBlocking(anyList(), eq(USER_MSG), anyString());
    }

    @Test
    void generateAiResponseStream_deberiaEmitirTokensDelRouter_cuandoRouterResponde() {
        when(providerRouter.routeStream(anyList(), eq(USER_MSG), anyString()))
                .thenReturn(Flux.just("Token 1", "Token 2"));

        var history = List.of(
                new Message("USER", "Hola")
        );

        Flux<String> result = aiEngineService.generateAiResponseStream(
                history, USER_MSG, TITLE, DESC, CAT);

        StepVerifier.create(result)
                .expectNext("Token 1")
                .expectNext("Token 2")
                .verifyComplete();

        verify(providerRouter).routeStream(anyList(), eq(USER_MSG), anyString());
    }

    @Test
    void generateAiResponseStream_deberiaEmitirUnavailable_cuandoRouterDevuelveVacio() {
        when(providerRouter.routeStream(anyList(), eq(USER_MSG), anyString()))
                .thenReturn(Flux.empty());

        var history = List.<Message>of();

        Flux<String> result = aiEngineService.generateAiResponseStream(
                history, USER_MSG, TITLE, DESC, CAT);

        StepVerifier.create(result)
                .expectNextMatches(s -> s.contains("dificultades temporales"))
                .verifyComplete();

        verify(providerRouter).routeStream(anyList(), eq(USER_MSG), anyString());
    }
}
