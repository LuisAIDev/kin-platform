package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.CreatePricingPlanRequest;
import com.kinplatform.pricing.dto.PricingPlanResponse;
import com.kinplatform.pricing.dto.UpdatePricingPlanRequest;

import java.util.List;
import java.util.UUID;

public interface PricingPlanService {

    List<PricingPlanResponse> getAllActive();

    PricingPlanResponse getById(UUID id);

    PricingPlanResponse create(CreatePricingPlanRequest request);

    PricingPlanResponse update(UUID id, UpdatePricingPlanRequest request);

    void deactivate(UUID id);
}
