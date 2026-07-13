package com.kinplatform.pricing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePricingPlanRequest {

    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    @NotBlank
    private String currency;

    @NotBlank
    private String billingPeriod;

    @NotNull
    private List<String> features;

    @NotNull
    private Boolean isPopular;

    @NotNull
    private Integer displayOrder;
}
