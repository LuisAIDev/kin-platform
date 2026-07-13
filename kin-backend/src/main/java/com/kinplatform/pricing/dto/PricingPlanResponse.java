package com.kinplatform.pricing.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.pricing.PricingPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingPlanResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private String currency;
    private String billingPeriod;
    private List<String> features;
    private Boolean isPopular;
    private Integer displayOrder;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static PricingPlanResponse fromEntity(PricingPlan plan) {
        List<String> featureList;
        try {
            featureList = MAPPER.readValue(plan.getFeatures(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            featureList = Collections.emptyList();
        }

        return PricingPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .billingPeriod(plan.getBillingPeriod())
                .features(featureList)
                .isPopular(plan.getIsPopular())
                .displayOrder(plan.getDisplayOrder())
                .build();
    }
}
