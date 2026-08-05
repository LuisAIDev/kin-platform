package com.kinplatform.kin.knowledge.policy;

/**
 * Contrato base de una regla de política (especificación Fase 2, Strategy
 * Pattern). Cada regla declara su categoría y su nombre; la evaluación
 * tipada vive en los subcontratos por categoría.
 *
 * <p>OCP: nuevas reglas son estrategias registrables en el Knowledge Policy
 * Engine; el motor y el dominio no se modifican.</p>
 */
public interface PolicyRule {

    PolicyCategory category();

    String name();
}
