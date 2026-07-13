package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.PricingPlanResponse;
import com.kinplatform.pricing.dto.UpdatePricingPlanRequest;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PricingPlanController {

    private final PricingPlanRepository pricingPlanRepository;
    private final UserRepository userRepository;

    @GetMapping("/pricing-plans")
    public ResponseEntity<List<PricingPlanResponse>> getAll() {
        var plans = pricingPlanRepository.findAllByOrderByDisplayOrderAsc();
        var response = plans.stream()
                .map(PricingPlanResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/admin/pricing-plans/{id}")
    public ResponseEntity<PricingPlanResponse> update(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePricingPlanRequest request
    ) {
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only administrators can update pricing plans");
        }

        var plan = pricingPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + id));

        plan.setName(request.getName());
        plan.setPrice(request.getPrice());
        plan.setCurrency(request.getCurrency());
        plan.setBillingPeriod(request.getBillingPeriod());
        plan.setIsPopular(request.getIsPopular());
        plan.setDisplayOrder(request.getDisplayOrder());

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            plan.setFeatures(mapper.writeValueAsString(request.getFeatures()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize features", e);
        }

        pricingPlanRepository.save(plan);
        return ResponseEntity.ok(PricingPlanResponse.fromEntity(plan));
    }
}
