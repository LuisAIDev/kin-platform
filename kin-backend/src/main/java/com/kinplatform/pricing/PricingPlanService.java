package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.CreatePricingPlanRequest;
import com.kinplatform.pricing.dto.PricingPlanResponse;
import com.kinplatform.pricing.dto.UpdatePricingPlanRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PricingPlanService {

    List<PricingPlanResponse> getAllActive();

    List<PricingPlan> getActivePlans();

    PricingPlanResponse getById(UUID id);

    Optional<PricingPlan> getPlanByName(String name);

    PricingPlanResponse create(CreatePricingPlanRequest request);

    PricingPlanResponse update(UUID id, UpdatePricingPlanRequest request);

    void deactivate(UUID id);
}
