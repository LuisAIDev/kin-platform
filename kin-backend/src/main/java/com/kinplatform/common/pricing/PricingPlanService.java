package com.kinplatform.common.pricing;

import com.kinplatform.common.pricing.dto.CreatePricingPlanRequest;
import com.kinplatform.common.pricing.dto.PricingPlanResponse;
import com.kinplatform.common.pricing.dto.UpdatePricingPlanRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricingPlanService {

    List<PricingPlanResponse> getAllActive();

    List<PricingPlan> getActivePlans();

    Optional<PricingPlan> getPlanByName(String name);

    Optional<PricingPlan> getPlanByCode(String code);

    /**
     * Busca un plan por {@code code} dentro de una vertical concreta. Desde V37
     * el {@code code} es único por vertical (no global), por lo que las
     * búsquedas de planes de una vertical específica deben filtrar por vertical
     * para evitar {@code NonUniqueResultException}.
     */
    Optional<PricingPlan> getPlanByCodeAndVertical(String code, ProductVertical vertical);

    /**
     * Busca un plan por {@code name} dentro de una vertical concreta. Mismo
     * criterio que {@link #getPlanByCodeAndVertical}: {@code name} no es único
     * global entre verticales.
     */
    Optional<PricingPlan> getPlanByNameAndVertical(String name, ProductVertical vertical);

    PricingPlanResponse create(CreatePricingPlanRequest request);

    PricingPlanResponse update(UUID id, UpdatePricingPlanRequest request);

    void deactivate(UUID id);

    PricingPlanResponse getById(UUID id);

    List<PricingPlanResponse> getByVertical(ProductVertical vertical);
}

