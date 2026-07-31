package com.kinplatform.kin.engine;

/**
 * Contrato común de los resultados producidos por los motores de dominio.
 *
 * <p>Todos los resultados comparten trazabilidad (quién lo generó, con qué
 * versión), confianza y explicación. Cada motor conserva además su tipo
 * concreto (tipado fuerte) con sus campos específicos.</p>
 */
public interface EngineResult {

    double confidence();

    String explanation();

    String generatedBy();

    String engineVersion();

    boolean isEmpty();
}
