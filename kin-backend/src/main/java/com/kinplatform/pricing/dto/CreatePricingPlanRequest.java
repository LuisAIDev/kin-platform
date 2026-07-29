package com.kinplatform.pricing.dto;

import com.kinplatform.pricing.SupportLevel;
import com.kinplatform.pricing.ViabilityScoringDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePricingPlanRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    private BigDecimal price;

    @NotNull(message = "Features are required")
    private List<String> features;

    private Integer maxProjects;

    private Integer messagesPerMonth;

    private Boolean advancedAI;

    private Boolean pdfExport;

    private SupportLevel supportLevel;

    private ViabilityScoringDetail viabilityScoringDetail;

    private Boolean isActive;
}
