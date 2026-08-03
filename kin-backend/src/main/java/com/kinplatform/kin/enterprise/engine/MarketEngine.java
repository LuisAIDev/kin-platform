package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.enterprise.engine.input.MarketInput;
import com.kinplatform.kin.enterprise.engine.result.MarketResult;

/**
 * Contrato del motor de mercado (Fase 10).
 *
 * <p>Producirá el plan de mercado (mercado direccionable proxy, segmentos,
 * canales, competidores verificados y barreras de entrada) como value object
 * {@code MarketPlan} (vía {@link MarketResult}) a partir de la entrada tipada
 * {@link MarketInput}, y poblará la {@code MarketSection} del informe de
 * consultoría, priorizando los hechos verificados del motor de conocimiento.
 * Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 2A especializa el contrato (entrada y resultado concretos,
 * sin genéricos ambiguos); la implementación se realizará en el Milestone 2.
 * Véase la decisión de aislamiento de {@code EngineRegistry} en
 * {@code package-info}.</p>
 */
public interface MarketEngine
        extends DomainEngine<MarketInput, MarketResult> {
}
