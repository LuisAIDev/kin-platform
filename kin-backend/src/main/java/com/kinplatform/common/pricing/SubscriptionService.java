package com.kinplatform.common.pricing;

import com.kinplatform.common.pricing.dto.PatientSubscriptionStatusResponse;
import com.kinplatform.common.pricing.dto.SubscriptionResponse;

import java.util.UUID;

public interface SubscriptionService {

    SubscriptionResponse subscribe(UUID userId, UUID planId);

    SubscriptionResponse startTrial(UUID userId, UUID planId);

    SubscriptionResponse cancelSubscription(UUID userId);

    SubscriptionResponse cancelPatientSubscription(UUID userId);

    SubscriptionResponse getCurrentSubscription(UUID userId);

    /**
     * Obtiene el estado de suscripción completo para un paciente (vertical SALUD_PERSONAL).
     * Incluye límites de triaje, almacenamiento, presupuesto de IA, etc.
     */
    PatientSubscriptionStatusResponse getPatientSubscriptionStatus(UUID userId);

    boolean hasAvailableMessages(UUID userId);

    void incrementMessagesUsed(UUID userId);

    void resetMonthlyUsage();
}


