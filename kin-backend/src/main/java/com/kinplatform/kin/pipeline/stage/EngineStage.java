package com.kinplatform.kin.pipeline.stage;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EngineExecutor;
import com.kinplatform.kin.engine.EngineInput;
import com.kinplatform.kin.engine.EngineResult;
import com.kinplatform.kin.pipeline.PipelineContext;
import com.kinplatform.kin.pipeline.PipelineStage;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Etapa genérica del pipeline que ejecuta un {@link DomainEngine} mediante
 * composición: recibe el motor, el predicado de soporte, la fábrica de entrada
 * y el escritor de resultado. Elimina la duplicación entre etapas de motores
 * (antes: {@code RecommendationStage} y {@code RiskStage} eran casi idénticas).
 *
 * <p>Cualquier motor futuro se integra configurando una instancia de esta
 * etapa (o la etapa concreta que la componga), sin modificar el pipeline ni la
 * infraestructura de motores.</p>
 */
public class EngineStage<E extends EngineInput, R extends EngineResult> implements PipelineStage {

    private final String stageName;
    private final DomainEngine<E, R> engine;
    private final Predicate<PipelineContext> supportsPredicate;
    private final Function<PipelineContext, E> inputFactory;
    private final BiConsumer<PipelineContext, R> resultWriter;
    private final EngineExecutor executor;

    public EngineStage(String stageName,
                       DomainEngine<E, R> engine,
                       Predicate<PipelineContext> supportsPredicate,
                       Function<PipelineContext, E> inputFactory,
                       BiConsumer<PipelineContext, R> resultWriter) {
        this(stageName, engine, supportsPredicate, inputFactory, resultWriter, new EngineExecutor());
    }

    public EngineStage(String stageName,
                       DomainEngine<E, R> engine,
                       Predicate<PipelineContext> supportsPredicate,
                       Function<PipelineContext, E> inputFactory,
                       BiConsumer<PipelineContext, R> resultWriter,
                       EngineExecutor executor) {
        this.stageName = stageName;
        this.engine = engine;
        this.supportsPredicate = supportsPredicate;
        this.inputFactory = inputFactory;
        this.resultWriter = resultWriter;
        this.executor = executor;
    }

    @Override
    public String name() {
        return stageName;
    }

    @Override
    public boolean supports(PipelineContext context) {
        return supportsPredicate.test(context);
    }

    @Override
    public PipelineContext execute(PipelineContext context) {
        E input = inputFactory.apply(context);
        R result = executor.execute(engine, input).result();
        resultWriter.accept(context, result);
        context.setEngineResult(engine.metadata().name(), result);
        return context;
    }
}
