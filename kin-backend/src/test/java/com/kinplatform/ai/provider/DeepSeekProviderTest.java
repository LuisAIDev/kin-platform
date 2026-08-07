package com.kinplatform.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.context.Message;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
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
    void generateBlocking_conErrorEnLaLlamada_deberiaDevolverNull() {
        stubPrompt();
        when(spec.call()).thenThrow(new RuntimeException("llm down"));

        assertNull(provider.generateBlocking(history, "hi", "sys"));
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
