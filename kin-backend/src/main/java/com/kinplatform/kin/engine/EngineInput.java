package com.kinplatform.kin.engine;

/**
 * Contrato común de entrada de los motores de dominio.
 *
 * <p>Interfaz marcadora: no impone métodos, porque cada motor consume un
 * subconjunto distinto de la información producida por Java en la
 * conversación. Los motores existentes (Recommendation, Risk, Scoring)
 * exponen sus campos tipados ({@code projectContext()}, {@code evaluation()},
 * {@code decision()}, {@code score()}) en sus propios records; los motores
 * futuros (Market, Competition, Innovation, Financial, etc.) declararán solo
 * lo que realmente necesitan.</p>
 *
 * <p>La marca {@code EngineInput} garantiza que el {@link EngineExecutor} y el
 * {@code EngineStage} puedan construir y consumir entradas con tipado fuerte
 * sin acoplarse a un motor específico.</p>
 */
public interface EngineInput {
}
