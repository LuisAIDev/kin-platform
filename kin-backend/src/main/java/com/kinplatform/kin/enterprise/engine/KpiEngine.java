package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.enterprise.engine.input.KpiInput;
import com.kinplatform.kin.enterprise.engine.result.KpiResult;

/**
 * Contrato del motor de KPIs (Fase 10).
 *
 * <p>Definirá los KPIs por fase (adquisición, activación, retención, ingresos
 * y margen) como value object {@code KpiSet} (vía {@link KpiResult}) a partir
 * de la entrada tipada {@link KpiInput}, con objetivos derivados del plan de
 * mercado y del plan financiero. Es un {@link DomainEngine} determinista.</p>
 *
 * <p>El Milestone 2A especializa el contrato (entrada y resultado concretos,
 * sin genéricos ambiguos); la implementación se realizará en el Milestone 2.
 * Véase la decisión de aislamiento de {@code EngineRegistry} en
 * {@code package-info}.</p>
 */
public interface KpiEngine
        extends DomainEngine<KpiInput, KpiResult> {
}
