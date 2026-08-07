package com.kinplatform.pricing.stripe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.user.UserRepository;
import com.stripe.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class StripeServiceWebhookIdempotencyTest {

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private StripeWebhookEventRepository webhookEventRepository;

    @Mock
    private Event event;

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService =
                new StripeService(planRepository, userRepository, subscriptionRepository, webhookEventRepository);
    }

    @Test
    void eventoNuevo_deberiaProcesarse() {
        when(event.getId()).thenReturn("evt_new_1");
        when(event.getType()).thenReturn("unhandled.type");
        when(webhookEventRepository.saveAndFlush(any(StripeWebhookEvent.class))).thenAnswer(i -> i.getArgument(0));

        boolean processed = stripeService.processWebhookEvent(event);

        assertTrue(processed);
        verify(webhookEventRepository).saveAndFlush(any(StripeWebhookEvent.class));
    }

    @Test
    void eventoDuplicado_deberiaOmitirseSinEfectos() {
        when(event.getId()).thenReturn("evt_dup_1");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(webhookEventRepository.saveAndFlush(any(StripeWebhookEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        boolean processed = stripeService.processWebhookEvent(event);

        assertFalse(processed);
        verify(webhookEventRepository).saveAndFlush(any(StripeWebhookEvent.class));
        verify(event, never()).getDataObjectDeserializer();
    }

    @Test
    void eventoDuplicadoConEventoNoManejado_tambienSeOmitira() {
        when(event.getId()).thenReturn("evt_dup_2");
        when(event.getType()).thenReturn("checkout.session.expired");
        when(webhookEventRepository.saveAndFlush(any(StripeWebhookEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        boolean processed = stripeService.processWebhookEvent(event);

        assertFalse(processed);
    }
}
