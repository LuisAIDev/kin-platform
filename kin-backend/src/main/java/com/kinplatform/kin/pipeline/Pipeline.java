package com.kinplatform.kin.pipeline;

import com.kinplatform.kin.pipeline.resilience.PipelineErrorHandler;
import com.kinplatform.kin.pipeline.resilience.PipelineExecutionException;
import com.kinplatform.kin.pipeline.resilience.PipelineMetrics;
import com.kinplatform.kin.pipeline.resilience.StageExecutionStats;
import com.kinplatform.kin.pipeline.resilience.StagePolicy;
import com.kinplatform.kin.pipeline.resilience.StageRetryPolicy;
import com.kinplatform.kin.pipeline.resilience.StageTimeoutConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta la ejecución secuencial de stages del pipeline (ADR-017, Etapa E3).
 *
 * <p>La ejecución incorpora resiliencia por stage de forma <strong>aditiva</strong>:
 * manejo de errores (fail-fast/skip/retry), timeout por stage y métricas
 * ({@link PipelineMetrics}), manteniendo intacta la firma pública
 * {@code execute(PipelineContext)} y la API de {@link PipelineStage}
 * ({@code name()}/{@code supports()}/{@code execute()}).</p>
 *
 * <p>El constructor histórico {@code Pipeline(List)} conserva el comportamiento
 * previo (políticas fail-fast por defecto, sin reintentos); el constructor con
 * políticas habilita la resiliencia configurada. Decisión 100 % Java: retry
 * únicamente donde la política lo permite (fail-fast por defecto, timeout
 * medido sin infraestructura externa, métricas internas sin persistir ni
 * exponer).</p>
 */
public class Pipeline {

    private static final Logger log = LoggerFactory.getLogger(Pipeline.class);

    private final List<PipelineStage> stages;
    private final Map<String, StagePolicy> policies;
    private final StageRetryPolicy retryPolicy;
    private final StageTimeoutConfig timeoutConfig;

    private PipelineMetrics lastMetrics;

    /**
     * Constructor de compatibilidad: resiliencia por defecto (fail-fast por
     * stage, sin reintentos, timeout por defecto).
     */
    public Pipeline(List<PipelineStage> stages) {
        this(stages, defaultPolicies(stages), StageRetryPolicy.none(),
            StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS));
    }

    /**
     * Constructor con resiliencia explícita (ADR-017, Etapa E3).
     *
     * @param stages         stages del pipeline
     * @param stagePolicies  políticas por stage (vacíos → fail-fast por defecto)
     * @param retryPolicy    estrategia global de reintento
     * @param timeoutConfig  configuración de timeout por stage
     */
    public Pipeline(List<PipelineStage> stages, List<StagePolicy> stagePolicies,
                    StageRetryPolicy retryPolicy, StageTimeoutConfig timeoutConfig) {
        if (stages == null) {
            throw new IllegalArgumentException("stages no puede ser null");
        }
        this.stages = new ArrayList<>(stages);
        this.policies = indexPolicies(stagePolicies);
        this.retryPolicy = retryPolicy == null
            ? StageRetryPolicy.none()
            : retryPolicy;
        this.timeoutConfig = timeoutConfig == null
            ? StageTimeoutConfig.defaultTimeout(StagePolicy.DEFAULT_TIMEOUT_MILLIS)
            : timeoutConfig;
    }

    public PipelineContext execute(PipelineContext context) {
        log.info("Pipeline execution started with {} stages", stages.size());
        List<StageExecutionStats> stats = new ArrayList<>();
        long runStart = System.nanoTime();
        PipelineContext current = context;
        for (var stage : stages) {
            if (!stage.supports(current)) {
                log.info("Stage '{}' skipped (not supported)", stage.name());
                continue;
            }
            current.currentStage(stage.name());
            StageRun run = runStage(stage, current, policyFor(stage.name()));
            stats.add(run.stats());
            current = run.context();
            log.info("Pipeline stage: '{}' -> success={}, duration={}ms, attempts={}, timedOut={}",
                stage.name(), run.stats().success(), run.stats().durationMillis(),
                run.stats().attempts(), run.stats().timedOut());
            if (run.failure() != null) {
                this.lastMetrics = new PipelineMetrics(stats, elapsedMillis(runStart));
                throw run.failure();
            }
            if (current.completed()) {
                log.info("Pipeline marked as completed after stage '{}'", stage.name());
                break;
            }
        }
        current.markCompleted();
        this.lastMetrics = new PipelineMetrics(stats, elapsedMillis(runStart));
        log.info("Pipeline execution finished with {} stage(s)", stats.size());
        return current;
    }

    /**
     * Métricas de la última ejecución (o vacías si no hubo ejecución).
     * Aditivo: no altera el contrato de {@code execute}.
     */
    public PipelineMetrics metrics() {
        return lastMetrics == null ? PipelineMetrics.empty() : lastMetrics;
    }

    private StageRun runStage(PipelineStage stage, PipelineContext context, StagePolicy policy) {
        int attempts = 0;
        while (true) {
            attempts++;
            long start = System.nanoTime();
            PipelineContext updated = context;
            Throwable failure = null;
            try {
                updated = stage.execute(context);
            } catch (RuntimeException e) {
                failure = e;
            }
            long elapsed = elapsedMillis(start);
            boolean timedOut = (failure != null && PipelineErrorHandler.isTimeout(failure))
                || elapsed > timeoutConfig.timeoutMillisFor(stage.name());

            if (failure == null && !timedOut) {
                return new StageRun(StageExecutionStats.success(stage.name(), elapsed, attempts), updated, null);
            }

            boolean retryRequested = timedOut
                ? timeoutConfig.onTimeout() == StageTimeoutConfig.TimeoutAction.RETRY
                : policy.onFailure() == StagePolicy.FailureAction.RETRY;
            if (retryRequested && canRetry(stage.name(), policy, attempts)) {
                log.info("Stage '{}' failed (attempt {}) — retrying", stage.name(), attempts);
                sleepBackoff(stage.name(), attempts);
                continue;
            }

            if (timedOut) {
                if (timeoutConfig.onTimeout() == StageTimeoutConfig.TimeoutAction.SKIP) {
                    return new StageRun(StageExecutionStats.timedOut(stage.name(), elapsed, attempts), context, null);
                }
                return new StageRun(StageExecutionStats.timedOut(stage.name(), elapsed, attempts), context,
                    PipelineErrorHandler.classify(stage.name(), failure, true, attempts));
            }

            if (policy.onFailure() == StagePolicy.FailureAction.SKIP) {
                return new StageRun(StageExecutionStats.failure(stage.name(), elapsed, attempts, message(failure)),
                    context, null);
            }

            PipelineExecutionException exception = attempts > 1
                ? new PipelineExecutionException(stage.name(), PipelineExecutionException.FailureKind.RETRY_EXHAUSTED, failure)
                : PipelineErrorHandler.classify(stage.name(), failure, false, attempts);
            return new StageRun(StageExecutionStats.failure(stage.name(), elapsed, attempts, message(failure)),
                context, exception);
        }
    }

    private boolean canRetry(String stageName, StagePolicy policy, int attempts) {
        return !policy.retriesExhausted(attempts)
            && retryPolicy.maxRetries() > 0
            && retryPolicy.isEligible(stageName);
    }

    private StagePolicy policyFor(String stageName) {
        return policies.getOrDefault(stageName, StagePolicy.failFast(stageName));
    }

    private void sleepBackoff(String stageName, int attempts) {
        long delay = retryPolicy.delayForAttempt(attempts);
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PipelineExecutionException(stageName, PipelineExecutionException.FailureKind.UNEXPECTED, e);
        }
    }

    private static List<StagePolicy> defaultPolicies(List<PipelineStage> stages) {
        return stages == null ? List.of()
            : stages.stream().map(s -> StagePolicy.failFast(s.name())).toList();
    }

    private static Map<String, StagePolicy> indexPolicies(List<StagePolicy> stagePolicies) {
        Map<String, StagePolicy> indexed = new LinkedHashMap<>();
        if (stagePolicies != null) {
            for (StagePolicy policy : stagePolicies) {
                if (policy != null) {
                    indexed.put(policy.stageName(), policy);
                }
            }
        }
        return Map.copyOf(indexed);
    }

    private static String message(Throwable failure) {
        if (failure == null) {
            return "fallo";
        }
        return failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName();
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private record StageRun(StageExecutionStats stats, PipelineContext context,
                            PipelineExecutionException failure) {
    }
}
