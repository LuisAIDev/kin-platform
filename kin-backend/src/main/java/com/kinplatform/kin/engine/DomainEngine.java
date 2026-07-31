package com.kinplatform.kin.engine;

/**
 * Contrato único de los motores de dominio de KIN.
 *
 * <p>Permite que cualquier motor futuro (Opportunity, Knowledge, Innovation,
 * Financial, Competition, Market, Report, etc.) se integre sin modificar el
 * núcleo: basta implementar este contrato y el {@link EngineRegistry} lo
 * descubre automáticamente mediante inyección de {@code List<DomainEngine>}.</p>
 *
 * <p>Es un contrato por composición (interfaz genérica), no una clase base:
 * los motores conservan su propia forma y solo declaran sus metadatos y su
 * evaluación. Si la entrada no es procesable, cada motor devuelve un resultado
 * vacío de seguridad (patrón {@code empty()}) sin lanzar excepciones.</p>
 *
 * @param <E> tipo de entrada del motor (inmutable, tipado)
 * @param <R> tipo de resultado del motor (debe implementar {@link EngineResult})
 */
public interface DomainEngine<E extends EngineInput, R extends EngineResult> {

    EngineMetadata metadata();

    R evaluate(E input);
}
