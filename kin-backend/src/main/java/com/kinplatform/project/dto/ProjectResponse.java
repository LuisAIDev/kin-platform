package com.kinplatform.project.dto;

import com.kinplatform.project.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class ProjectResponse {

    private UUID id;
    private UUID userId;
    private String title;
    private String description;
    private String category;
    private String categoryName;
    private String categoryColor;
    private ProjectStatus status;
    private BigDecimal viabilityScore;
    private String aiSummary;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer progressPercentage;
}
