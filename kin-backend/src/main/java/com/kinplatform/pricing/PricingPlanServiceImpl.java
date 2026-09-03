package com.kinplatform.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.pricing.dto.CreatePricingPlanRequest;
import com.kinplatform.pricing.dto.PricingPlanResponse;
import com.kinplatform.pricing.dto.UpdatePricingPlanRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<PricingPlan> getActivePlans() {
        log.debug("Fetching all active pricing plan entities");
        return repository.findByIsActiveTrueOrderByPriceAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingPlan> getPlanByName(String name) {
        log.debug("Fetching pricing plan by name: {}", name);
        return repository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingPlan> getPlanByCode(String code) {
        log.debug("Fetching pricing plan by code: {}", code);
        return repository.findByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingPlan> getPlanByCodeAndVertical(String code, ProductVertical vertical) {
        log.debug("Fetching pricing plan by code: {} and vertical: {}", code, vertical);
        return repository.findByCodeAndVertical(code, vertical);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PricingPlan> getPlanByNameAndVertical(String name, ProductVertical vertical) {
        log.debug("Fetching pricing plan by name: {} and vertical: {}", name, vertical);
        return repository.findByNameAndVertical(name, vertical);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PricingPlanResponse> getByVertical(ProductVertical vertical) {
        log.debug("Fetching active pricing plans by vertical: {}", vertical);
        return repository.findByVerticalAndIsActiveTrueOrderByPriceAsc(vertical).stream()
                .map(PricingPlanResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPlanResponse getById(UUID id) {
        log.debug("Fetching pricing plan by id: {}", id);
        var plan = repository
                .findById(id)
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
                    .code(request.getCode())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .features(objectMapper.writeValueAsString(request.getFeatures()))
                    .maxProjects(request.getMaxProjects())
                    .messagesPerMonth(request.getMessagesPerMonth())
                    .aiBudgetUsd(request.getAiBudgetUsd())
                    .advancedAI(request.getAdvancedAI() != null ? request.getAdvancedAI() : false)
                    .pdfExport(request.getPdfExport() != null ? request.getPdfExport() : false)
                    .supportLevel(request.getSupportLevel() != null ? request.getSupportLevel() : SupportLevel.BASIC)
                    .viabilityScoringDetail(
                            request.getViabilityScoringDetail() != null
                                    ? request.getViabilityScoringDetail()
                                    : ViabilityScoringDetail.BASIC)
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

        var plan = repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + id));

        plan.setName(request.getName());
        plan.setCode(request.getCode());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setMaxProjects(request.getMaxProjects());
        plan.setMessagesPerMonth(request.getMessagesPerMonth());
        plan.setAiBudgetUsd(request.getAiBudgetUsd());
        plan.setAdvancedAI(request.getAdvancedAI() != null ? request.getAdvancedAI() : false);
        plan.setPdfExport(request.getPdfExport() != null ? request.getPdfExport() : false);
        plan.setSupportLevel(request.getSupportLevel() != null ? request.getSupportLevel() : SupportLevel.BASIC);
        plan.setViabilityScoringDetail(
                request.getViabilityScoringDetail() != null
                        ? request.getViabilityScoringDetail()
                        : ViabilityScoringDetail.BASIC);
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
        var plan = repository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing plan not found: " + id));
        plan.setIsActive(false);
        repository.save(plan);
        log.info("Pricing plan deactivated: {}", id);
    }
}
