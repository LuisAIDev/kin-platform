package com.kinplatform.pricing.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.SupportLevel;
import com.kinplatform.pricing.ViabilityScoringDetail;
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
public class PricingPlanDTO {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private List<String> features;
    private Integer maxProjects;
    private Integer messagesPerMonth;
    private Boolean advancedAI;
    private Boolean pdfExport;
    private SupportLevel supportLevel;
    private ViabilityScoringDetail viabilityScoringDetail;
    private Boolean isActive;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static PricingPlanDTO fromEntity(PricingPlan plan) {
        List<String> featureList;
        try {
            featureList = MAPPER.readValue(plan.getFeatures(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            featureList = Collections.emptyList();
        }

        return PricingPlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .features(featureList)
                .maxProjects(plan.getMaxProjects())
                .messagesPerMonth(plan.getMessagesPerMonth())
                .advancedAI(plan.getAdvancedAI())
                .pdfExport(plan.getPdfExport())
                .supportLevel(plan.getSupportLevel())
                .viabilityScoringDetail(plan.getViabilityScoringDetail())
                .isActive(plan.getIsActive())
                .build();
    }
}
