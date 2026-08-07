package com.kinplatform.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class PricingEntitiesTest {

    @Test
    void userSubscription_onCreate_deberiaInicializarFechas() throws Exception {
        var subscription = new UserSubscription();
        assertNull(subscription.getCreatedAt());

        invokeProtected(subscription, "onCreate");

        assertNotNull(subscription.getCreatedAt());
        assertNotNull(subscription.getUpdatedAt());
        assertNotNull(subscription.getLastResetDate());
    }

    @Test
    void userSubscription_onUpdate_deberiaActualizarUpdatedAt() throws Exception {
        var subscription = new UserSubscription();
        invokeProtected(subscription, "onCreate");
        var previous = subscription.getUpdatedAt();

        invokeProtected(subscription, "onUpdate");

        assertNotNull(subscription.getUpdatedAt());
    }

    @Test
    void userSubscription_conLastResetDate_noDebeSobrescribirlo() throws Exception {
        var subscription = new UserSubscription();
        var reset = OffsetDateTime.now().minusDays(5);
        subscription.setLastResetDate(reset);

        invokeProtected(subscription, "onCreate");

        assertSame(reset, subscription.getLastResetDate());
    }

    @Test
    void pricingPlan_onUpdate_deberiaActualizarUpdatedAt() throws Exception {
        var plan = new PricingPlan();
        invokeProtected(plan, "onCreate");
        assertNotNull(plan.getCreatedAt());

        invokeProtected(plan, "onUpdate");

        assertNotNull(plan.getUpdatedAt());
    }

    @Test
    void pricingPlan_builderDefaults_deberianAplicarse() {
        var plan = PricingPlan.builder().build();

        assertEquals(SupportLevel.BASIC, plan.getSupportLevel());
        assertEquals(ViabilityScoringDetail.BASIC, plan.getViabilityScoringDetail());
        assertEquals(false, plan.getAdvancedAI());
        assertEquals(false, plan.getPdfExport());
        assertEquals(true, plan.getIsActive());
    }

    @Test
    void userSubscription_builderDefaults_deberianAplicarse() {
        var sub = UserSubscription.builder().build();

        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertEquals(0, sub.getMessagesUsed());
    }

    @Test
    void planNotFound_sinCausa() {
        var ex = new PlanNotFoundException("not found");

        assertEquals("not found", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void planNotFound_conCausa() {
        var cause = new RuntimeException("boom");
        var ex = new PlanNotFoundException("not found", cause);

        assertEquals("not found", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    private static void invokeProtected(Object target, String name) throws Exception {
        var method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }
}
