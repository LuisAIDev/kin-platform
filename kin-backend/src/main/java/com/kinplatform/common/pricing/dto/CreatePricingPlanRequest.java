package com.kinplatform.common.pricing.dto;

import com.kinplatform.common.pricing.ProductVertical;
import com.kinplatform.common.pricing.SupportLevel;
import com.kinplatform.common.pricing.ViabilityScoringDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CreatePricingPlanRequest {

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

    private Boolean isActive;

    private ProductVertical vertical;

    private Integer maxTriagesPerMonth;

    private Integer trialDays;

    private Integer maxPatients;

    private Boolean triageSharing;
}

