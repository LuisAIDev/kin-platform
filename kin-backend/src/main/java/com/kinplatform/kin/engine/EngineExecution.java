package com.kinplatform.kin.engine;

/**
 * Envoltorio inmutable de una ejecución de motor: el resultado producido,
 * el tiempo de ejecución y los metadatos del motor que lo ejecutó.
 *
 * <p>Proporciona trazabilidad por ejecución (cuánto tardó, con qué versión)
 * sin modificar el resultado tipado del motor.</p>
 *
 * @param <R> tipo de resultado del motor
 */
public record EngineExecution<R extends EngineResult>(
    R result,
    long runtimeMs,
    EngineMetadata metadata
) {
}
