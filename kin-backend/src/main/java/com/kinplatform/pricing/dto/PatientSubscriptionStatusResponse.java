package com.kinplatform.pricing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de estado de suscripción para pacientes (vertical SALUD_PERSONAL).
 * Incluye límites de triaje, almacenamiento y presupuesto de IA.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientSubscriptionStatusResponse {

    private boolean isActive;
    private String planName;
    private String planCode;
    private String planDescription;

    // Triaje
    private Integer maxTriagesPerMonth;
    private Integer triagesUsed;
    private Integer triagesRemaining;

    // Almacenamiento
    private Integer maxStorageMb;
    private Integer storageUsedMb;
    private Integer storageRemainingMb;

    // IA
    private BigDecimal aiBudgetUsd;
    private BigDecimal aiBudgetUsed;
    private BigDecimal aiBudgetRemaining;
    private String aiLevel;

    // Otros
    private Boolean pdfExport;
    private Boolean triageSharing;
    private Boolean advancedAI;
    private String supportLevel;

    // Fechas
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
    private OffsetDateTime subscriptionEndDate;
}