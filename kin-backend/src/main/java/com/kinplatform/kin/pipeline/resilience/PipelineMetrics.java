package com.kinplatform.kin.pipeline.resilience;

import com.kinplatform.kin.engine.EngineResult;

import java.util.List;
import java.util.Optional;

/**
 * Métricas inmutables de una ejecución del pipeline (ADR-017, Etapa E2).
 *
 * <p>Agrega las estadísticas individuales de cada stage (duración, estado,
 * reintentos, timeouts). Implementa {@link EngineResult} para compartir el
 * contrato común de trazabilidad y operar con la infraestructura de motores.
 * Se almacena en un registro separado del {@code PipelineContext} (mitigación
 * del riesgo R6 "God Class").</p>
 */
public record PipelineMetrics(
    List<StageExecutionStats> stageStats,
    long totalDurationMillis
) implements EngineResult {

    public static final String GENERATOR_NAME = "Pipeline";
    public static final String ENGINE_VERSION = "v1";

    public PipelineMetrics {
        stageStats = stageStats == null ? List.of() : List.copyOf(stageStats);
        totalDurationMillis = Math.max(0, totalDurationMillis);
    }

    public static PipelineMetrics empty() {
        return new PipelineMetrics(List.of(), 0L);
    }

    public int totalStages() {
        return stageStats.size();
    }

    public long successfulStages() {
        return stageStats.stream().filter(StageExecutionStats::success).count();
    }

    public long failedStages() {
        return stageStats.stream().filter(stats -> !stats.success()).count();
    }

    public long timedOutStages() {
        return stageStats.stream().filter(StageExecutionStats::timedOut).count();
    }

    public long totalRetries() {
        return stageStats.stream().mapToLong(StageExecutionStats::retries).sum();
    }

    public Optional<StageExecutionStats> statsFor(String stageName) {
        return stageStats.stream()
            .filter(stats -> stageName.equals(stats.stageName()))
            .findFirst();
    }

    /**
     * Tasa de éxito en [0,1]; 0 si no hay stages.
     */
    public double successRate() {
        if (stageStats.isEmpty()) {
            return 0.0;
        }
        return (double) successfulStages() / stageStats.size();
    }

    @Override
    public double confidence() {
        return successRate();
    }

    @Override
    public String explanation() {
        if (stageStats.isEmpty()) {
            return "Sin métricas de pipeline.";
        }
        return "Pipeline ejecutado con " + totalStages() + " stage(s): "
            + successfulStages() + " exitosos, " + failedStages() + " fallidos, "
            + totalRetries() + " reintentos.";
    }

    @Override
    public String generatedBy() {
        return GENERATOR_NAME;
    }

    @Override
    public String engineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public boolean isEmpty() {
        return stageStats.isEmpty();
    }
}
