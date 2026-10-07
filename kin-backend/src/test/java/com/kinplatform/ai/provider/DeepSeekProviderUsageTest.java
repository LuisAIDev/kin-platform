package com.kinplatform.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.ai.usage.AiUsageRecorder;
import com.kinplatform.ai.usage.ReservationContext;
import com.kinplatform.platform.usage.AiReservation;
import com.kinplatform.platform.usage.UsagePeriod;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/** Verifica que DeepSeekProvider captura el usage real y lo reporta al recorder. */
@ExtendWith(MockitoExtension.class)
class DeepSeekProviderUsageTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec spec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamSpec;

    @Mock
    private AiUsageRecorder recorder;

    private ReservationContext reservationContext = new ReservationContext();
    private DeepSeekProvider provider;

    private final AiReservation reservation =
            new AiReservation(UUID.randomUUID(), UsagePeriod.current(), new BigDecimal("0.01"));

    @BeforeEach
    void setUp() {
        provider = new DeepSeekProvider(chatClient, "deepseek-v4-flash", recorder, reservationContext);
        reservationContext.set(reservation);
    }

    @AfterEach
    void tearDown() {
        reservationContext.clear();
    }

    private void stubPrompt() {
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.messages(any(org.springframework.ai.chat.messages.Message[].class)))
                .thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
    }

    private static ChatResponse chatResponseWithUsage(String text, int in, int out) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(text))))
                .metadata(ChatResponseMetadata.builder()
                        .usage(new DefaultUsage(in, out, in + out))
                        .build())
                .build();
    }

    private static ChatResponse chatResponseWithoutUsage(String text) {
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(text))))
                .metadata(ChatResponseMetadata.builder().build())
                .build();
    }

    @Test
    void generateBlocking_conUso_loReportaALaReserva() {
        stubPrompt();
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(chatResponseWithUsage("resp", 100, 50));

        assertEquals("resp", provider.generateBlocking(List.of(), "hi", "sys"));

        verify(recorder).recordActual(reservation, 100L, 50L);
    }

    @Test
    void generateBlocking_sinUso_liberaReserva() {
        stubPrompt();
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(chatResponseWithoutUsage("resp"));

        provider.generateBlocking(List.of(), "hi", "sys");

        verify(recorder).recordActual(reservation, 0L, 0L);
    }

    @Test
    void generateBlocking_error_liberaReserva() {
        stubPrompt();
        when(spec.call()).thenThrow(new RuntimeException("llm down"));

        provider.generateBlocking(List.of(), "hi", "sys");

        verify(recorder).recordActual(reservation, 0L, 0L);
    }

    @Test
    void generateBlocking_respuestaNula_liberaReserva() {
        stubPrompt();
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(null);

        provider.generateBlocking(List.of(), "hi", "sys");

        verify(recorder).recordActual(reservation, 0L, 0L);
    }

    @Test
    void generateStream_conUso_loReportaAlCompletar() {
        stubPrompt();
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.just(chatResponseWithUsage("a", 200, 30)));

        StepVerifier.create(provider.generateStream(List.of(), "hi", "sys"))
                .expectNext("a")
                .verifyComplete();

        verify(recorder).recordActual(reservation, 200L, 30L);
    }

    @Test
    void generateStream_chunkSoloUso_sinContenido_igualmenteReportaUso() {
        stubPrompt();
        when(spec.stream()).thenReturn(streamSpec);
        // Chunk final/metadata: trae usage pero sin resultado (getResult() null).
        when(streamSpec.chatResponse()).thenReturn(Flux.just(ChatResponse.builder()
                .generations(List.of())
                .metadata(ChatResponseMetadata.builder()
                        .usage(new DefaultUsage(50, 7, 57))
                        .build())
                .build()));

        StepVerifier.create(provider.generateStream(List.of(), "hi", "sys"))
                .verifyComplete();

        verify(recorder).recordActual(reservation, 50L, 7L);
    }

    @Test
    void generateStream_error_liberaReserva() {
        stubPrompt();
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.error(new RuntimeException("stream broken")));

        StepVerifier.create(provider.generateStream(List.of(), "hi", "sys")).verifyError(RuntimeException.class);

        verify(recorder).recordActual(reservation, 0L, 0L);
    }

    @Test
    void generateStream_sinUso_liberaReserva() {
        stubPrompt();
        when(spec.stream()).thenReturn(streamSpec);
        when(streamSpec.chatResponse()).thenReturn(Flux.just(chatResponseWithoutUsage("a")));

        StepVerifier.create(provider.generateStream(List.of(), "hi", "sys"))
                .expectNext("a")
                .verifyComplete();

        verify(recorder).recordActual(reservation, 0L, 0L);
    }

    @Test
    void sinReservaVigente_noReporta() {
        reservationContext.clear();
        stubPrompt();
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(chatResponseWithUsage("resp", 10, 5));

        provider.generateBlocking(List.of(), "hi", "sys");

        verify(recorder, never()).recordActual(any(), anyLong(), anyLong());
    }
}

