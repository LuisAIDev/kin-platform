package com.kinplatform.pricing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String planDescription;
    private int remainingMessages;
    private boolean canCreateProject;
    private String aiLevel;
    private Integer messagesPerMonth;
    private Integer maxProjects;
    private Boolean advancedAI;
    private Boolean pdfExport;
    private String supportLevel;
}
