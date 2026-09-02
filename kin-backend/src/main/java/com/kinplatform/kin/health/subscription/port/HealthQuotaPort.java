package com.kinplatform.kin.health.subscription.port;

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

    /** Máximo de pacientes propios del médico. {@code null} = ilimitado. */
    Integer getMaxPatients(UUID physicianId);

    /** Días de prueba gratuita del usuario. {@code null} = sin trial. */
    Integer getTrialDays(UUID userId);
}