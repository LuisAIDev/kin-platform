package com.kinplatform.pricing.stripe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.SupportLevel;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.pricing.ViabilityScoringDetail;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private StripeWebhookEventRepository webhookEventRepository;

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService =
                new StripeService(planRepository, userRepository, subscriptionRepository, webhookEventRepository);
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "whsec_test");
    }

    @AfterEach
    void tearDown() {}

    private User user() {
        return User.builder()
                .id(USER_ID)
                .email("u@kin.com")
                .fullName("U")
                .role(UserRole.FREE)
                .build();
    }

    private PricingPlan plan(boolean active) {
        return PricingPlan.builder()
                .id(PLAN_ID)
                .name("Premium Pro")
                .description("Desc")
                .price(new BigDecimal("29.00"))
                .features("[]")
                .isActive(active)
                .supportLevel(SupportLevel.PREMIUM)
                .viabilityScoringDetail(ViabilityScoringDetail.DETAILED)
                .build();
    }

    @Test
    void createCheckoutSession_conUrls_deberiaCrearSesion() throws StripeException {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(true)));
        var session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getUrl()).thenReturn("https://checkout.stripe.com/c/pay/cs_test_123");

        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

            var response =
                    stripeService.createCheckoutSession(USER_ID, PLAN_ID, "https://app/success", "https://app/cancel");

            assertEquals("cs_test_123", response.getSessionId());
            assertEquals("https://checkout.stripe.com/c/pay/cs_test_123", response.getUrl());
        }
    }

    @Test
    void createCheckoutSession_sinUrls_deberiaUsarDefaults() throws StripeException {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(true)));
        var session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_456");
        when(session.getUrl()).thenReturn("https://checkout.stripe.com/c/pay/cs_test_456");

        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

            var response = stripeService.createCheckoutSession(USER_ID, PLAN_ID, null, null);

            assertEquals("cs_test_456", response.getSessionId());
        }
    }

    @Test
    void createCheckoutSession_usuarioNoEncontrado_deberiaFallar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> stripeService.createCheckoutSession(USER_ID, PLAN_ID, null, null));
    }

    @Test
    void createCheckoutSession_planNoEncontrado_deberiaFallar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> stripeService.createCheckoutSession(USER_ID, PLAN_ID, null, null));
    }

    @Test
    void createCheckoutSession_planInactivo_deberiaFallar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(false)));

        assertThrows(
                IllegalArgumentException.class,
                () -> stripeService.createCheckoutSession(USER_ID, PLAN_ID, null, null));
    }

    @Test
    void createCheckoutSession_planGratis_deberiaFallar() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        var freePlan = plan(true);
        freePlan.setPrice(BigDecimal.ZERO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(freePlan));

        assertThrows(
                IllegalArgumentException.class,
                () -> stripeService.createCheckoutSession(USER_ID, PLAN_ID, null, null));
    }

    @Test
    void createCheckoutSession_errorStripe_deberiaEnvolver() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(true)));

        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenThrow(mock(StripeException.class));

            var ex = assertThrows(
                    RuntimeException.class, () -> stripeService.createCheckoutSession(USER_ID, PLAN_ID, null, null));

            assertTrue(ex.getMessage().contains("Payment processing error"));
        }
    }

    @Test
    void handleCheckoutCompleted_deberiaActivarSuscripcion() throws StripeException {
        var session = mock(Session.class);
        when(session.getClientReferenceId()).thenReturn(USER_ID.toString());
        when(session.getMetadata()).thenReturn(Map.of("plan_id", PLAN_ID.toString()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(true)));
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.retrieve("cs_completed")).thenReturn(session);

            stripeService.handleCheckoutCompleted("cs_completed");

            verify(subscriptionRepository).save(any(UserSubscription.class));
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void handleCheckoutCompleted_conSuscripcionActiva_deberiaFallar() throws StripeException {
        var session = mock(Session.class);
        when(session.getClientReferenceId()).thenReturn(USER_ID.toString());
        when(session.getMetadata()).thenReturn(Map.of("plan_id", PLAN_ID.toString()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(true)));
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(mock(UserSubscription.class)));

        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.retrieve("cs_dup")).thenReturn(session);

            assertThrows(RuntimeException.class, () -> stripeService.handleCheckoutCompleted("cs_dup"));
        }
    }

    @Test
    void processWebhookEvent_checkoutCompleted_deberiaProcesar() throws StripeException {
        var session = mock(Session.class);
        when(session.getId()).thenReturn("cs_wh");
        when(session.getClientReferenceId()).thenReturn(USER_ID.toString());
        when(session.getMetadata()).thenReturn(Map.of("plan_id", PLAN_ID.toString()));
        var deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        var event = mock(Event.class);
        when(event.getId()).thenReturn("evt_wh_1");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(webhookEventRepository.saveAndFlush(any(StripeWebhookEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(true)));
        when(subscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(UserSubscription.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
            mocked.when(() -> Session.retrieve("cs_wh")).thenReturn(session);

            boolean processed = stripeService.processWebhookEvent(event);

            assertTrue(processed);
        }
    }

    @Test
    void processWebhookEvent_checkoutExpired_deberiaSoloLoguear() {
        var event = mock(Event.class);
        when(event.getId()).thenReturn("evt_exp");
        when(event.getType()).thenReturn("checkout.session.expired");
        when(webhookEventRepository.saveAndFlush(any(StripeWebhookEvent.class))).thenAnswer(i -> i.getArgument(0));

        assertTrue(stripeService.processWebhookEvent(event));
        verify(event, never()).getDataObjectDeserializer();
    }

    @Test
    void processWebhookEvent_tipoNoManejado_deberiaProcesar() {
        var event = mock(Event.class);
        when(event.getId()).thenReturn("evt_un");
        when(event.getType()).thenReturn("invoice.paid");
        when(webhookEventRepository.saveAndFlush(any(StripeWebhookEvent.class))).thenAnswer(i -> i.getArgument(0));

        assertTrue(stripeService.processWebhookEvent(event));
    }

    @Test
    void processWebhookEvent_deserializacionFallida_deberiaFallar() {
        var deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.empty());

        var event = mock(Event.class);
        when(event.getId()).thenReturn("evt_des");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(webhookEventRepository.saveAndFlush(any(StripeWebhookEvent.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(RuntimeException.class, () -> stripeService.processWebhookEvent(event));
    }

    @Test
    void processWebhookEvent_duplicado_deberiaOmitirse() {
        var event = mock(Event.class);
        when(event.getId()).thenReturn("evt_dup");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(webhookEventRepository.saveAndFlush(any(StripeWebhookEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertFalse(stripeService.processWebhookEvent(event));
    }

    @Test
    void constructWebhookEvent_deberiaConstruir() {
        var event = mock(Event.class);
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);

            var result = stripeService.constructWebhookEvent("{}", "sig");

            assertEquals(event, result);
        }
    }

    @Test
    void constructWebhookEvent_sinSecret_deberiaFallar() {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "  ");

        assertThrows(IllegalStateException.class, () -> stripeService.constructWebhookEvent("{}", "sig"));
    }

    @Test
    void constructWebhookEvent_firmaInvalida_deberiaFallar() {
        try (MockedStatic<Webhook> mocked = mockStatic(Webhook.class)) {
            mocked.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("bad signature"));

            assertThrows(RuntimeException.class, () -> stripeService.constructWebhookEvent("{}", "sig"));
        }
    }
}
