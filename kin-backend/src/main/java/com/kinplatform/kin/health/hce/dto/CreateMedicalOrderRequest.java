package com.kinplatform.kin.health.hce.dto;

import com.kinplatform.kin.health.hce.entity.MedicalOrder.OrderType;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Priority;
import com.kinplatform.kin.health.hce.entity.MedicalOrder.Route;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMedicalOrderRequest {

    @NotNull(message = "Treatment plan ID is required")
    private UUID treatmentPlanId;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @Size(max = 200, message = "Drug name cannot exceed 200 characters")
    private String drugName;

    @Size(max = 100, message = "Dose cannot exceed 100 characters")
    private String dose;

    @Size(max = 50, message = "Dose unit cannot exceed 50 characters")
    private String doseUnit;

    private Route route;

    @Size(max = 100, message = "Frequency cannot exceed 100 characters")
    private String frequency;

    private Integer durationDays;

    @Size(max = 20, message = "CUPS code cannot exceed 20 characters")
    private String cupsCode;

    @Size(max = 500, message = "CUPS description cannot exceed 500 characters")
    private String cupsDescription;

    @Size(max = 100, message = "Body site cannot exceed 100 characters")
    private String bodySite;

    @Size(max = 2000, message = "Instructions cannot exceed 2000 characters")
    private String instructions;
}