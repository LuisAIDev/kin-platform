package com.kinplatform.kin.enterprise.engine;

import com.kinplatform.kin.engine.DomainEngine;
import com.kinplatform.kin.enterprise.engine.input.BusinessModelInput;
import com.kinplatform.kin.enterprise.engine.result.BusinessModelResult;

/**
 * Contrato del motor de modelo de negocio (Fase 10).
 *
 * <p>Propondrá el modelo de negocio y la propuesta de valor produciendo el
 * value object {@code LeanCanvas} (vía {@link BusinessModelResult}) a partir de
 * la entrada tipada {@link BusinessModelInput}. Es un {@link DomainEngine}
 * determinista: Java decide, DeepSeek únicamente comunica.</p>
 *
 * <p>El Milestone 2A especializa el contrato (entrada y resultado concretos,
 * sin genéricos ambiguos); la implementación se realizará en el Milestone 2.
 * Véase la decisión de aislamiento de {@code EngineRegistry} en
 * {@code package-info}.</p>
 */
public interface BusinessModelEngine
        extends DomainEngine<BusinessModelInput, BusinessModelResult> {
}
