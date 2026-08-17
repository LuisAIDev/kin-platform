package com.kinplatform.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.context.Message;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Verifica el streaming de {@link DeepSeekProvider}: entrega de chunks,
 * completado y, en particular, que un stream SILENCIOSO (sin chunks) falla de
 * forma controlada con {@link TimeoutException} en lugar de colgar (el fix del
 * timeout de inactividad SSE que evitaba el esperar 180 s).
 */
@ExtendWith(MockitoExtension.class)
class DeepSeekProviderStreamTimeoutTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec spec;

    @Mock
    private ChatClient.StreamResponseSpec streamSpec;

    private final List<Message> history = List.of(Message.user("hola"));

    @BeforeEach
    void setUp() {
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.messages(any(org.springframework.ai.chat.messages.Message[].class)))
                .thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
    }

    private static ChatResponse chunk(String text) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(text))))
                .build();
    }

    @Test
    void generateStream_conChunks_entregaYCompleta() {
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.just(chunk("a"), chunk("b"), chunk("c")));
        DeepSeekProvider provider = new DeepSeekProvider(chatClient, "deepseek-v4-flash");

        StepVerifier.create(provider.generateStream(history, "hi", "sys"))
                .expectNext("a", "b", "c")
                .verifyComplete();
    }

    @Test
    void generateStream_conStreamSilencioso_timeoutControlado() {
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.never());
        // Timeout de inactividad muy corto para el test (el default es 90 s).
        DeepSeekProvider provider = new DeepSeekProvider(chatClient, "deepseek-v4-flash", 1, Duration.ofMillis(10), 1);

        StepVerifier.create(provider.generateStream(history, "hi", "sys"))
                .expectError(TimeoutException.class)
                .verify(Duration.ofSeconds(2));
    }

    @Test
    void generateStream_conError_propagaError() {
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.error(new RuntimeException("llm down")));
        DeepSeekProvider provider = new DeepSeekProvider(chatClient, "deepseek-v4-flash", 0, Duration.ofMillis(10), 1);

        StepVerifier.create(provider.generateStream(history, "hi", "sys")).verifyError(RuntimeException.class);
    }

    @Test
    void generateStream_conStreamVacio_completa() {
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.empty());
        DeepSeekProvider provider = new DeepSeekProvider(chatClient, "deepseek-v4-flash", 1, Duration.ofMillis(10), 1);

        StepVerifier.create(provider.generateStream(history, "hi", "sys")).verifyComplete();
    }

    @Test
    void generateStream_entregaTokensProgresivos() {
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.just(chunk("un "), chunk("mensaje "), chunk("largo")));
        DeepSeekProvider provider = new DeepSeekProvider(chatClient, "deepseek-v4-flash", 1, Duration.ofMillis(10), 1);

        StringBuilder sb = new StringBuilder();
        StepVerifier.create(provider.generateStream(history, "hi", "sys"))
                .consumeNextWith(sb::append)
                .consumeNextWith(sb::append)
                .consumeNextWith(sb::append)
                .verifyComplete();
        assertEquals("un mensaje largo", sb.toString());
    }
}
