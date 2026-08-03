/**
 * Contratos de los motores deterministas del proyecto empresarial (Fase 10).
 *
 * <p>Este paquete define únicamente contratos: las interfaces de los motores
 * (especializadas en {@code DomainEngine}), sus entradas ({@code input/}) y sus
 * resultados ({@code result/}). No contiene lógica de negocio ni
 * implementaciones.</p>
 *
 * <h2>Decisión arquitectónica: aislamiento de {@code EngineRegistry}
 * (hallazgo R5 de la auditoría)</h2>
 *
 * <p>Los motores enterprise implementan el contrato común {@link DomainEngine}
 * del núcleo para reutilizar la infraestructura de motores existente
 * (metadatos, fases, prioridades) sin duplicarla. Sin embargo, a diferencia de
 * los motores del pipeline conversacional, <b>NUNCA deben registrarse como
 * beans de Spring de tipo {@code DomainEngine}</b>:</p>
 *
 * <ul>
 *   <li>{@code EngineRegistry} se alimenta mediante inyección de
 *       {@code List<DomainEngine>} ({@code KinConfig}): cualquier bean que
 *       implemente {@code DomainEngine} entra automáticamente en su índice.
 *       No se modifica {@code EngineRegistry} (contrato congelado).</li>
 *   <li>Los motores enterprise NO pertenecen al pipeline conversacional
 *       (Analizador → … → Eventos). Si entraran en el índice aparecerían en
 *       {@code allOrdered()}/{@code byPhase()}/{@code after()} y podrían ser
 *       ejecutados por cualquier flujo que recorra el registro de forma
 *       genérica, además de arriesgar colisiones de nombre (el índice es
 *       {@code last-wins}).</li>
 *   <li>Por ello, el ensamblado de los motores enterprise es responsabilidad
 *       EXCLUSIVA de la capa de aplicación de {@code kin.enterprise}
 *       (composition root del Milestone 2): se instancian y cablean fuera del
 *       contenedor Spring, garantizando que jamás entren en el índice del
 *       pipeline. La ejecución ocurre en el flujo de generación del proyecto
 *       empresarial, nunca mediante {@code EngineRegistry}.</li>
 * </ul>
 *
 * <p>Convenciones de metadatos para las implementaciones del Milestone 2:</p>
 * <ul>
 *   <li>Nombre único con prefijo reservado {@code kin.enterprise:} (p. ej.
 *       {@code kin.enterprise:BusinessModel}) para garantizar disyunción con
 *       los motores conversacionales.</li>
 *   <li>Fases reservadas de {@link EnginePhase}: {@code FINANCIAL},
 *       {@code MARKET}, {@code INNOVATION} y {@code EXPLANATION} (existentes y
 *       sin uso en el pipeline conversacional).</li>
 *   <li>Prioridades en la banda enterprise, disjunta de la conversacional
 *       (actualmente 30–70): <b>≥ 80</b>.</li>
 * </ul>
 */
package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.engine.EnginePhase;
