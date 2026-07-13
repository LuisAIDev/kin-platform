package com.kinplatform.ai;

import com.kinplatform.chat.ChatMessage;
import com.kinplatform.chat.MessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiEngineServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    private AiEngineService aiEngineService;

    private static final String MODEL_NAME = "llama3.2";
    private static final String USER_MSG = "¿Qué opinas de mi proyecto?";
    private static final String TITLE = "Mi App";
    private static final String DESC = "App para gestionar tareas";
    private static final String CAT = "EMPRENDIMIENTO";
    private static final String OLLAMA_RESP = "¡Excelente idea! Recomiendo enfocarte en la validación temprana con clientes potenciales.";

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.messages(any(Message[].class))).thenReturn(requestSpec);
        when(requestSpec.user(USER_MSG)).thenReturn(requestSpec);
        aiEngineService = new AiEngineService(chatClientBuilder, MODEL_NAME);
    }

    @Test
    void generateAiResponse_deberiaRetornarRespuestaDeOllama_cuandoOllamaResponde() {
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(OLLAMA_RESP);

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertEquals(OLLAMA_RESP, result);
        verify(requestSpec).call();
        verify(callResponseSpec).content();
    }

    @Test
    void generateAiResponse_deberiaRetornarMock_cuandoOllamaFalla() {
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenThrow(new RuntimeException("Connection refused"));

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertNotNull(result);
        assertFalse(result.isBlank());
        verify(requestSpec).call();
        verify(callResponseSpec).content();
    }

    @Test
    void generateAiResponse_deberiaRetornarMock_cuandoOllamaTimeout() {
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(null);

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertTrue(result.contains(TITLE));
        verify(requestSpec).call();
        verify(callResponseSpec).content();
    }

    @Test
    void generateAiResponseStream_deberiaEmitirTokensDeOllama_cuandoOllamaResponde() {
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("Token 1", "Token 2"));

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        Flux<String> result = aiEngineService.generateAiResponseStream(
                history, USER_MSG, TITLE, DESC, CAT);

        StepVerifier.create(result)
                .expectNext("Token 1")
                .expectNext("Token 2")
                .verifyComplete();

        verify(requestSpec).stream();
        verify(streamResponseSpec).content();
    }

    @Test
    void generateAiResponseStream_deberiaEmitirMock_cuandoOllamaFalla() {
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.error(new RuntimeException("Stream error")));

        var history = List.<ChatMessage>of();

        Flux<String> result = aiEngineService.generateAiResponseStream(
                history, USER_MSG, TITLE, DESC, CAT);

        StepVerifier.create(result)
                .expectNextMatches(s -> s.contains(TITLE))
                .verifyComplete();

        verify(requestSpec).stream();
        verify(streamResponseSpec).content();
    }
}
