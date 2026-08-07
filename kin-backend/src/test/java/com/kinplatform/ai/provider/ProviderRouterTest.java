package com.kinplatform.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.context.Message;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProviderRouterTest {

    @Mock
    private AIProvider primary;

    @Mock
    private AIProvider fallback;

    private final List<Message> history = List.of(new Message("USER", "hola"));

    private ProviderRouter router(AIProvider... providers) {
        return new ProviderRouter(List.of(providers));
    }

    @Test
    void routeBlocking_primarioResponde_deberiaUsarlo() {
        when(primary.providerName()).thenReturn("Primary");
        when(primary.generateBlocking(history, "hi", "sys")).thenReturn("respuesta");
        ProviderRouter router = router(primary, fallback);

        assertEquals("respuesta", router.routeBlocking(history, "hi", "sys"));
        verify(primary).generateBlocking(history, "hi", "sys");
    }

    @Test
    void routeBlocking_primarioVacio_deberiaCaerAlFallback() {
        when(primary.providerName()).thenReturn("Primary");
        when(fallback.providerName()).thenReturn("Fallback");
        when(primary.generateBlocking(history, "hi", "sys")).thenReturn(null);
        when(fallback.generateBlocking(history, "hi", "sys")).thenReturn("fallback-ok");
        ProviderRouter router = router(primary, fallback);

        assertEquals("fallback-ok", router.routeBlocking(history, "hi", "sys"));
    }

    @Test
    void routeBlocking_todosFallan_deberiaDevolverNull() {
        when(primary.providerName()).thenReturn("Primary");
        when(fallback.providerName()).thenReturn("Fallback");
        when(primary.generateBlocking(history, "hi", "sys")).thenReturn("  ");
        when(fallback.generateBlocking(history, "hi", "sys")).thenReturn(null);
        ProviderRouter router = router(primary, fallback);

        assertNull(router.routeBlocking(history, "hi", "sys"));
    }

    @Test
    void routeBlocking_sinProviders_deberiaDevolverNull() {
        assertNull(router().routeBlocking(history, "hi", "sys"));
    }

    @Test
    void routeStream_primarioOk_deberiaEmitirSusTokens() {
        when(primary.generateStream(history, "hi", "sys")).thenReturn(Flux.just("a", "b"));
        ProviderRouter router = router(primary, fallback);

        StepVerifier.create(router.routeStream(history, "hi", "sys"))
                .expectNext("a", "b")
                .verifyComplete();
    }

    @Test
    void routeStream_primarioFalla_deberiaCaerAlFallback() {
        when(primary.providerName()).thenReturn("Primary");
        when(fallback.providerName()).thenReturn("Fallback");
        when(primary.generateStream(history, "hi", "sys")).thenReturn(Flux.error(new RuntimeException("boom")));
        when(fallback.generateStream(history, "hi", "sys")).thenReturn(Flux.just("c"));

        ProviderRouter router = router(primary, fallback);

        StepVerifier.create(router.routeStream(history, "hi", "sys"))
                .expectNext("c")
                .verifyComplete();
    }

    @Test
    void routeStream_sinProviders_deberiaEstarVacio() {
        StepVerifier.create(router().routeStream(history, "hi", "sys")).verifyComplete();
    }
}
