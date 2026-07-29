package com.kinplatform.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.pricing.dto.CreatePricingPlanRequest;
import com.kinplatform.pricing.dto.PricingPlanResponse;
import com.kinplatform.pricing.dto.UpdatePricingPlanRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingPlanServiceImpl implements PricingPlanService {

    private final PricingPlanRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PricingPlanResponse> getAllActive() {
        log.debug("Fetching all active pricing plans");
        return repository.findByIsActiveTrueOrderByPriceAsc().stream()
                .map(PricingPlanResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPlanResponse getById(UUID id) {
        log.debug("Fetching pricing plan by id: {}", id);
        var plan = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + id));
        return PricingPlanResponse.fromEntity(plan);
    }

    @Override
    @Transactional
    public PricingPlanResponse create(CreatePricingPlanRequest request) {
        log.info("Creating new pricing plan: {}", request.getName());

        try {
            var plan = PricingPlan.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .features(objectMapper.writeValueAsString(request.getFeatures()))
                    .maxProjects(request.getMaxProjects())
                    .messagesPerMonth(request.getMessagesPerMonth())
                    .advancedAI(request.getAdvancedAI() != null ? request.getAdvancedAI() : false)
                    .pdfExport(request.getPdfExport() != null ? request.getPdfExport() : false)
                    .supportLevel(request.getSupportLevel() != null ? request.getSupportLevel() : SupportLevel.BASIC)
                    .viabilityScoringDetail(request.getViabilityScoringDetail() != null ? request.getViabilityScoringDetail() : ViabilityScoringDetail.BASIC)
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();

            var saved = repository.save(plan);
            log.info("Pricing plan created successfully: {} ({})", saved.getName(), saved.getId());
            return PricingPlanResponse.fromEntity(saved);
        } catch (Exception e) {
            log.error("Failed to create pricing plan", e);
            throw new RuntimeException("Failed to serialize features", e);
        }
    }

    @Override
    @Transactional
    public PricingPlanResponse update(UUID id, UpdatePricingPlanRequest request) {
        log.info("Updating pricing plan: {}", id);

        var plan = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + id));

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setMaxProjects(request.getMaxProjects());
        plan.setMessagesPerMonth(request.getMessagesPerMonth());
        plan.setAdvancedAI(request.getAdvancedAI() != null ? request.getAdvancedAI() : false);
        plan.setPdfExport(request.getPdfExport() != null ? request.getPdfExport() : false);
        plan.setSupportLevel(request.getSupportLevel() != null ? request.getSupportLevel() : SupportLevel.BASIC);
        plan.setViabilityScoringDetail(request.getViabilityScoringDetail() != null ? request.getViabilityScoringDetail() : ViabilityScoringDetail.BASIC);
        plan.setIsActive(request.getIsActive());

        try {
            plan.setFeatures(objectMapper.writeValueAsString(request.getFeatures()));
        } catch (Exception e) {
            log.error("Failed to serialize features for plan {}", id, e);
            throw new RuntimeException("Failed to serialize features", e);
        }

        var saved = repository.save(plan);
        log.info("Pricing plan updated successfully: {}", saved.getId());
        return PricingPlanResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        log.info("Deactivating pricing plan: {}", id);
        var plan = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + id));
        plan.setIsActive(false);
        repository.save(plan);
        log.info("Pricing plan deactivated: {}", id);
    }
}
