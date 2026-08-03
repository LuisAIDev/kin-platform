package com.kinplatform.kin.enterprise.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Sección del Enterprise Score del dashboard Enterprise (Fase 10, Milestone 2J).
 *
 * <p>Puntuación multidimensional del proyecto empresarial. El aggregate
 * {@code EnterpriseProject} (contrato congelado) no porta el score, por lo que
 * en el flujo actual este DTO es {@code null} en el dashboard; la estructura
 * queda preparada para cuando la puntuación se persista.</p>
 *
 * @param overall       puntuación global (0-100)
 * @param grade         grado derivado
 * @param confidence    confianza (0-1)
 * @param market        dimensión de mercado
 * @param innovation    dimensión de innovación
 * @param viability     dimensión de viabilidad
 * @param financial     dimensión financiera
 * @param risk          dimensión de riesgo
 * @param scalability   dimensión de escalabilidad
 * @param team          dimensión de equipo
 * @param sustainability dimensión de sostenibilidad
 */
@Schema(description = "Enterprise Score del proyecto empresarial (no disponible en el flujo actual)")
public record EnterpriseScoreSection(
    @PositiveOrZero @Schema(description = "Puntuación global (0-100)", example = "72")
    Integer overall,
    @Schema(description = "Grado derivado", example = "FAIR")
    String grade,
    @Schema(description = "Confianza (0-1)", example = "0.82")
    Double confidence,
    @Schema(description = "Dimensión de mercado", example = "70")
    Double market,
    @Schema(description = "Dimensión de innovación", example = "65")
    Double innovation,
    @Schema(description = "Dimensión de viabilidad", example = "80")
    Double viability,
    @Schema(description = "Dimensión financiera", example = "60")
    Double financial,
    @Schema(description = "Dimensión de riesgo", example = "50")
    Double risk,
    @Schema(description = "Dimensión de escalabilidad", example = "75")
    Double scalability,
    @Schema(description = "Dimensión de equipo", example = "68")
    Double team,
    @Schema(description = "Dimensión de sostenibilidad", example = "55")
    Double sustainability
) {
}
