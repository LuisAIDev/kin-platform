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
    private ChatClient deepseekChatClient;

    @Mock
    private ChatClient.Builder openaiBuilder;

    @Mock
    private ChatClient openaiChatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec deepseekRequestSpec;

    @Mock
    private ChatClient.ChatClientRequestSpec openaiRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec deepseekCallResponseSpec;

    @Mock
    private ChatClient.CallResponseSpec openaiCallResponseSpec;

    @Mock
    private ChatClient.StreamResponseSpec deepseekStreamResponseSpec;

    @Mock
    private ChatClient.StreamResponseSpec openaiStreamResponseSpec;

    private AiEngineService aiEngineService;

    private static final String DEEPSEEK_MODEL = "deepseek-chat";
    private static final String OPENAI_MODEL = "gpt-4o-mini";
    private static final String USER_MSG = "\u00BFQu\u00E9 opinas de mi proyecto?";
    private static final String TITLE = "Mi App";
    private static final String DESC = "App para gestionar tareas";
    private static final String CAT = "EMPRENDIMIENTO";
    private static final String AI_RESP = "\u00A1Excelente idea! Recomiendo enfocarte en la validaci\u00F3n temprana con clientes potenciales.";

    @BeforeEach
    void setUp() {
        when(openaiBuilder.build()).thenReturn(openaiChatClient);
        when(deepseekChatClient.prompt()).thenReturn(deepseekRequestSpec);
        lenient().when(openaiChatClient.prompt()).thenReturn(openaiRequestSpec);
        when(deepseekRequestSpec.messages(any(Message[].class))).thenReturn(deepseekRequestSpec);
        when(deepseekRequestSpec.user(USER_MSG)).thenReturn(deepseekRequestSpec);
        lenient().when(openaiRequestSpec.messages(any(Message[].class))).thenReturn(openaiRequestSpec);
        lenient().when(openaiRequestSpec.user(USER_MSG)).thenReturn(openaiRequestSpec);
        aiEngineService = new AiEngineService(deepseekChatClient, openaiBuilder, DEEPSEEK_MODEL, OPENAI_MODEL, true);
    }

    @Test
    void generateAiResponse_deberiaRetornarRespuestaDeDeepSeek_cuandoDeepSeekResponde() {
        when(deepseekRequestSpec.call()).thenReturn(deepseekCallResponseSpec);
        when(deepseekCallResponseSpec.content()).thenReturn(AI_RESP);

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertEquals(AI_RESP, result);
        verify(deepseekRequestSpec).call();
        verify(deepseekCallResponseSpec).content();
        verifyNoInteractions(openaiChatClient);
    }

    @Test
    void generateAiResponse_deberiaCaerAOpenAI_cuandoDeepSeekFallaConError() {
        when(deepseekRequestSpec.call()).thenReturn(deepseekCallResponseSpec);
        when(deepseekCallResponseSpec.content()).thenThrow(new RuntimeException("DeepSeek connection refused"));
        when(openaiRequestSpec.call()).thenReturn(openaiCallResponseSpec);
        when(openaiCallResponseSpec.content()).thenReturn(AI_RESP);

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertEquals(AI_RESP, result);
        verify(deepseekRequestSpec).call();
        verify(openaiRequestSpec).call();
        verify(openaiCallResponseSpec).content();
    }

    @Test
    void generateAiResponse_deberiaRetornarMock_cuandoAmbosFallan() {
        when(deepseekRequestSpec.call()).thenReturn(deepseekCallResponseSpec);
        when(deepseekCallResponseSpec.content()).thenThrow(new RuntimeException("DeepSeek error"));
        when(openaiRequestSpec.call()).thenReturn(openaiCallResponseSpec);
        when(openaiCallResponseSpec.content()).thenThrow(new RuntimeException("OpenAI error"));

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertNotNull(result);
        assertFalse(result.isBlank());
        verify(deepseekRequestSpec).call();
        verify(openaiRequestSpec).call();
    }

    @Test
    void generateAiResponse_deberiaRetornarMock_cuandoDeepSeekTimeoutYOpeNaiTambien() {
        when(deepseekRequestSpec.call()).thenReturn(deepseekCallResponseSpec);
        when(deepseekCallResponseSpec.content()).thenReturn(null);
        when(openaiRequestSpec.call()).thenReturn(openaiCallResponseSpec);
        when(openaiCallResponseSpec.content()).thenReturn(null);

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        String result = aiEngineService.generateAiResponse(history, USER_MSG, TITLE, DESC, CAT);

        assertNotNull(result);
        verify(deepseekRequestSpec).call();
        verify(openaiRequestSpec).call();
    }

    @Test
    void generateAiResponseStream_deberiaEmitirTokensDeDeepSeek_cuandoDeepSeekResponde() {
        when(deepseekRequestSpec.stream()).thenReturn(deepseekStreamResponseSpec);
        when(deepseekStreamResponseSpec.content()).thenReturn(Flux.just("Token 1", "Token 2"));

        var history = List.of(
                ChatMessage.builder().role(MessageRole.USER).content("Hola").build()
        );

        Flux<String> result = aiEngineService.generateAiResponseStream(
                history, USER_MSG, TITLE, DESC, CAT);

        StepVerifier.create(result)
                .expectNext("Token 1")
                .expectNext("Token 2")
                .verifyComplete();

        verify(deepseekRequestSpec).stream();
        verify(deepseekStreamResponseSpec).content();
        verifyNoInteractions(openaiChatClient);
    }

    @Test
    void generateAiResponseStream_deberiaEmitirErrorTecnico_cuandoAmbosFallen() {
        when(deepseekRequestSpec.stream()).thenReturn(deepseekStreamResponseSpec);
        when(deepseekStreamResponseSpec.content()).thenReturn(Flux.error(new RuntimeException("Stream error")));
        when(openaiRequestSpec.stream()).thenReturn(openaiStreamResponseSpec);
        when(openaiStreamResponseSpec.content()).thenReturn(Flux.error(new RuntimeException("OpenAI stream error")));

        var history = List.<ChatMessage>of();

        Flux<String> result = aiEngineService.generateAiResponseStream(
                history, USER_MSG, TITLE, DESC, CAT);

        StepVerifier.create(result)
                .expectNextMatches(s -> s.contains("dificultades temporales"))
                .verifyComplete();

        verify(deepseekRequestSpec).stream();
        verify(openaiRequestSpec).stream();
    }
}
