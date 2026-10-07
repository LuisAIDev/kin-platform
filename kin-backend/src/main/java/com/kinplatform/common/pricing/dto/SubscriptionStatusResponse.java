package com.kinplatform.common.pricing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatusResponse {

    @JsonProperty("isActive")
    private boolean isActive;

    private String planName;
    private String planCode;
    private String planDescription;
    private int remainingMessages;
    private boolean canCreateProject;
    private String aiLevel;
    private Integer messagesPerMonth;
    private Integer maxProjects;
    private Boolean advancedAI;
    private Boolean pdfExport;
    private String supportLevel;

    // Fase 1 — cuota de proyectos completados por período
    private int completedProjects;
    private int completedProjectsLimit;
    private boolean canCompleteProject;

    // Fase 1 — presupuesto de IA del período vigente
    private boolean aiCostControlEnabled;
    private BigDecimal aiBudgetUsed;
    private BigDecimal aiBudgetReserved;
    private BigDecimal aiBudgetLimit;
    private BigDecimal aiBudgetRemaining;
    private OffsetDateTime aiUsagePeriodStart;
    private OffsetDateTime aiUsagePeriodEnd;
}

