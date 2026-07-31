package com.kinplatform.kin.engine;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

/**
 * Ejecutor de motores de dominio. Encapsula el modelo de ejecución y prepara
 * la plataforma para los modos: secuencial, paralelo, condicional, opcional y
 * por prioridades — sin que los motores dependan de este ejecutor.
 *
 * <p>Actualmente se implementa la ejecución <b>secuencial por prioridad</b>.
 * La ejecución paralela está diseñada pero NO implementada (no se introduce
 * concurrencia todavía): basta sustituir el cuerpo de {@link #executeAll} por
 * un flujo con {@code parallelStream()} o un {@code ExecutorService} sin
 * cambiar la firma, porque los motores son stateless y sus entradas/resultados
 * inmutables (seguro para concurrencia por diseño).</p>
 *
 * <p>Servicio de dominio puro: stateless, sin Spring, determinista en el
 * resultado (el runtime solo es métrica de observabilidad).</p>
 */
public class EngineExecutor {

    public static final long UNKNOWN_RUNTIME = -1L;

    /**
     * Ejecuta un motor y captura el resultado y el tiempo de ejecución.
     */
    public <E extends EngineInput, R extends EngineResult> EngineExecution<R> execute(
            DomainEngine<E, R> engine, E input) {
        long start = System.nanoTime();
        R result = engine.evaluate(input);
        long runtimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        return new EngineExecution<>(result, runtimeMs, engine.metadata());
    }

    /**
     * Ejecuta una lista de motores en orden de prioridad (menor primero),
     * construyendo la entrada de cada uno con la fábrica provista.
     */
    public <E extends EngineInput, R extends EngineResult> List<EngineExecution<R>> executeAll(
            List<? extends DomainEngine<E, R>> engines, Function<DomainEngine<E, R>, E> inputFactory) {
        return engines.stream()
            .sorted(Comparator.comparingInt((DomainEngine<E, R> e) -> e.metadata().priority()))
            .map(e -> execute(e, inputFactory.apply(e)))
            .toList();
    }

    /**
     * Ejecución condicional: solo si el predicado sobre los metadatos del motor
     * se cumple. Devuelve vacío cuando la condición no aplica.
     */
    public <E extends EngineInput, R extends EngineResult> Optional<EngineExecution<R>> executeIf(
            DomainEngine<E, R> engine, E input, Predicate<EngineMetadata> condition) {
        if (!condition.test(engine.metadata())) {
            return Optional.empty();
        }
        return Optional.of(execute(engine, input));
    }

    /**
     * Ejecución opcional: si la fábrica produce {@code null}, el motor se omite
     * (devuelve vacío). Permite motores que dependen de datos que pueden no
     * existir, sin lanzar excepciones.
     */
    public <E extends EngineInput, R extends EngineResult> Optional<EngineExecution<R>> executeOptional(
            DomainEngine<E, R> engine, Supplier<E> inputFactory) {
        E input = inputFactory.get();
        if (input == null) {
            return Optional.empty();
        }
        return Optional.of(execute(engine, input));
    }

    /**
     * Ejecución paralela (diseñada, NO implementada). Documenta el contrato que
     * permitirá ejecutar motores independientes en paralelo cuando se active.
     * Los motores son stateless y sus VOs inmutables, por lo que es seguro.
     */
    public <E extends EngineInput, R extends EngineResult> List<EngineExecution<R>> executeAllParallel(
            List<? extends DomainEngine<E, R>> engines, Function<DomainEngine<E, R>, E> inputFactory) {
        return executeAll(engines, inputFactory);
    }
}
