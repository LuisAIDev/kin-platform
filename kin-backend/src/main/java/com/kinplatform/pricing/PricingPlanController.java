package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.CreatePricingPlanRequest;
import com.kinplatform.pricing.dto.PricingPlanResponse;
import com.kinplatform.pricing.dto.UpdatePricingPlanRequest;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PricingPlanController {

    private final PricingPlanService pricingPlanService;
    private final UserRepository userRepository;

    @GetMapping("/pricing-plans")
    public ResponseEntity<List<PricingPlanResponse>> getAll() {
        return ResponseEntity.ok(pricingPlanService.getAllActive());
    }

    @GetMapping("/pricing-plans/{id}")
    public ResponseEntity<PricingPlanResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(pricingPlanService.getById(id));
    }

    @PostMapping("/admin/pricing-plans")
    public ResponseEntity<PricingPlanResponse> create(
            Authentication auth,
            @Valid @RequestBody CreatePricingPlanRequest request
    ) {
        requireAdmin(auth);
        var response = pricingPlanService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/admin/pricing-plans/{id}")
    public ResponseEntity<PricingPlanResponse> update(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePricingPlanRequest request
    ) {
        requireAdmin(auth);
        return ResponseEntity.ok(pricingPlanService.update(id, request));
    }

    @DeleteMapping("/admin/pricing-plans/{id}")
    public ResponseEntity<Void> deactivate(
            Authentication auth,
            @PathVariable UUID id
    ) {
        requireAdmin(auth);
        pricingPlanService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Authentication auth) {
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only administrators can manage pricing plans");
        }
    }
}
