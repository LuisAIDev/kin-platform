package com.kinplatform.kin.health.triage.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request del endpoint de triaje (ADR-028).
 *
 * @param symptoms lista de síntomas reportados (nombres del catálogo)
 */
public record TriageRequest(
        @NotEmpty(message = "Se requiere al menos un síntoma")
                @Size(max = 30, message = "Máximo 30 síntomas por consulta")
                List<@Size(max = 120) String> symptoms) {}
