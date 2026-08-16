package com.kinplatform.pricing.dto;

import com.kinplatform.pricing.SupportLevel;
import com.kinplatform.pricing.ViabilityScoringDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class UpdatePricingPlanRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String code;

    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    private BigDecimal price;

    @NotNull(message = "Features are required")
    private List<String> features;

    private Integer maxProjects;

    private Integer messagesPerMonth;

    private BigDecimal aiBudgetUsd;

    private Boolean advancedAI;

    private Boolean pdfExport;

    private SupportLevel supportLevel;

    private ViabilityScoringDetail viabilityScoringDetail;

    @NotNull(message = "Active status is required")
    private Boolean isActive;
}
