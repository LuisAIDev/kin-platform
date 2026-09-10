package com.kinplatform.kin.health.subscription.port;

import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.pricing.SubscriptionStatus;
import java.util.UUID;

/**
 * Puerto de cuotas de salud (KIN Salud).
 *
 * <p>Consulta los límites de la suscripción activa del usuario
 * (maxTriagesPerMonth, maxPatients, trialDays) resolviendo el
 * plan {@link com.kinplatform.pricing.PricingPlan} asociado.</p>
 */
public interface HealthQuotaPort {

    /** Máximo de triajes por mes para el usuario. {@code null} = ilimitado. */
    Integer getMaxTriagesPerMonth(UUID userId);

    /** Triajes utilizados por el usuario en el período actual. */
    Integer getTriagesUsed(UUID userId);

    /** Máximo de pacientes propios del médico. {@code null} = ilimitado. */
    Integer getMaxPatients(UUID physicianId);

    /** Días de prueba gratuita del usuario. {@code null} = sin trial. */
    Integer getTrialDays(UUID userId);

    /**
     * Indica si el usuario tiene una suscripción activa o en trial (vigente)
     * cuyo plan pertenece a la vertical indicada.
     *
     * <p>Método default ({@code false}) para no romper implementaciones y
     * tests anónimos existentes que no dependen de esta consulta.</p>
     */
    default boolean hasEligibleSubscription(
            UUID userId, ProductVertical vertical, SubscriptionStatus... statuses) {
        return false;
    }
}