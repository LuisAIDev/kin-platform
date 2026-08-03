package com.kinplatform.kin.enterprise.valueobjects;

import java.util.List;

/**
 * Nivel de innovación de un plan de innovación (value object).
 *
 * <p>Clasifica el grado de novedad de la propuesta: incremental (mejoras
 * graduales), transformacional (cambios sustanciales) o disruptivo (nuevos
 * modelos de valor). Usado por {@link InnovationPlan}.</p>
 */
public enum InnovationLevel {
    INCREMENTAL,
    TRANSFORMATIONAL,
    DISRUPTIVE
}
