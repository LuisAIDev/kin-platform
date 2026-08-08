package com.kinplatform.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.context.Message;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DeepSeekProviderTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec spec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamSpec;

    private DeepSeekProvider provider;

    private final List<Message> history = List.of(Message.user("hola"), Message.assistant("ok"));

    @BeforeEach
    void setUp() {
        provider = new DeepSeekProvider(chatClient, "deepseek-v4-flash");
    }

    private DeepSeekProvider provider(int maxRetries, Duration baseBackoff) {
        return new DeepSeekProvider(chatClient, "deepseek-v4-flash", maxRetries, baseBackoff);
    }

    private void stubPrompt() {
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.messages(any(org.springframework.ai.chat.messages.Message[].class)))
                .thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
    }

    @Test
    void providerName_deberiaSerDeepSeek() {
        assertEquals("DeepSeek", provider.providerName());
    }

    @Test
    void generateBlocking_deberiaDevolverLaRespuesta() {
        stubPrompt();
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("respuesta");

        assertEquals("respuesta", provider.generateBlocking(history, "hi", "sys"));
    }

    @Test
    void generateBlocking_conRespuestaNula_deberiaDevolverNull() {
        stubPrompt();
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(null);

        assertNull(provider.generateBlocking(history, "hi", "sys"));
    }

    @Test
    void generateStream_deberiaEmitirLosTokens() {
        stubPrompt();
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("a", "b"));

        StepVerifier.create(provider.generateStream(history, "hi", "sys"))
                .expectNext("a", "b")
                .verifyComplete();
    }

    @Test
    void generateBlocking_conErrorNoTransitorio_deberiaDevolverNullSinReintentar() {
        stubPrompt();
        when(spec.call()).thenThrow(new RuntimeException("llm down"));

        assertNull(provider.generateBlocking(history, "hi", "sys"));
        verify(spec, times(1)).call();
    }

    @Test
    void generateBlocking_conError429_deberiaReintentarYResponder() {
        stubPrompt();
        when(spec.call())
                .thenThrow(new TransientAiException("429 - Too Many Requests"))
                .thenReturn(callSpec);
        when(callSpec.content()).thenReturn("respuesta");

        DeepSeekProvider shortProvider = provider(1, Duration.ofMillis(10));
        assertEquals("respuesta", shortProvider.generateBlocking(history, "hi", "sys"));
        verify(spec, times(2)).call();
    }

    @Test
    void generateBlocking_con429ComoNonTransient_deberiaReintentar() {
        stubPrompt();
        when(spec.call())
                .thenThrow(new NonTransientAiException("429 - Too Many Requests"))
                .thenReturn(callSpec);
        when(callSpec.content()).thenReturn("respuesta");

        DeepSeekProvider shortProvider = provider(1, Duration.ofMillis(10));
        assertEquals("respuesta", shortProvider.generateBlocking(history, "hi", "sys"));
        verify(spec, times(2)).call();
    }

    @Test
    void generateBlocking_con429Cliente_deberiaReintentar() {
        stubPrompt();
        when(spec.call())
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests",
                        new HttpHeaders(),
                        new byte[0],
                        StandardCharsets.UTF_8))
                .thenReturn(callSpec);
        when(callSpec.content()).thenReturn("respuesta");

        DeepSeekProvider shortProvider = provider(1, Duration.ofMillis(10));
        assertEquals("respuesta", shortProvider.generateBlocking(history, "hi", "sys"));
        verify(spec, times(2)).call();
    }

    @Test
    void generateBlocking_conErroresTransitoriosAgotados_deberiaDevolverNull() {
        stubPrompt();
        when(spec.call()).thenThrow(new TransientAiException("429 - Too Many Requests"));

        DeepSeekProvider shortProvider = provider(1, Duration.ofMillis(10));
        assertNull(shortProvider.generateBlocking(history, "hi", "sys"));
        verify(spec, times(2)).call();
    }

    @Test
    void generateStream_conErrorTransitorio_deberiaReintentar() {
        stubPrompt();
        when(spec.stream()).thenReturn(streamSpec);
        AtomicInteger contentCalls = new AtomicInteger();
        when(streamSpec.content()).thenAnswer(inv -> contentCalls.getAndIncrement() == 0
                ? Flux.error(new TransientAiException("503 - Service Unavailable"))
                : Flux.just("ok"));

        DeepSeekProvider shortProvider = provider(1, Duration.ofMillis(10));
        StepVerifier.create(shortProvider.generateStream(history, "hi", "sys"))
                .expectNext("ok")
                .verifyComplete();
        verify(spec, times(2)).stream();
    }

    @Test
    void generateStream_conRolesSystemYDesconocidos_deberiaMapear() {
        stubPrompt();
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.empty());

        var mixedHistory = List.of(Message.system("sys"), new Message("CUSTOM", "raro"));
        StepVerifier.create(provider.generateStream(mixedHistory, "hi", "sys")).verifyComplete();
    }
}
