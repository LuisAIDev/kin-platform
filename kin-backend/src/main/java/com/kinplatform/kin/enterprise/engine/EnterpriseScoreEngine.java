package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.enterprise.engine.input.EnterpriseScoreInput;
import com.kinplatform.kin.enterprise.engine.result.EnterpriseScoreResult;

/**
 * Contrato del motor de puntuación empresarial (Fase 10).
 *
 * <p>Calculará el Enterprise Score multidimensional (mercado, innovación,
 * viabilidad, finanzas, riesgo, escalabilidad, equipo y sostenibilidad) como
 * value object {@code EnterpriseScore} (vía {@link EnterpriseScoreResult}) a
 * partir de la entrada tipada {@link EnterpriseScoreInput}, de forma totalmente
 * determinista (Java decide; el LLM solo comunica). Es un {@link DomainEngine}
 * determinista.</p>
 *
 * <p>El Milestone 2A especializa el contrato (entrada y resultado concretos,
 * sin genéricos ambiguos); la implementación se realizará en el Milestone 2.
 * Véase la decisión de aislamiento de {@code EngineRegistry} en
 * {@code package-info}.</p>
 */
public interface EnterpriseScoreEngine
        extends DomainEngine<EnterpriseScoreInput, EnterpriseScoreResult> {
}
