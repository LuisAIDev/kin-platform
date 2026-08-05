package com.kinplatform.kin.knowledge.planner;

/**
 * Contrato de una regla de intención (especificación Fase 3, Strategy Pattern).
 * Reglas declarativas registrables: el {@code IntentAnalyzer} las recorre en
 * orden de registro y agrega la faceta de cada regla que coincide.
 *
 * <p>OCP: nuevas reglas de intención se registran sin modificar el analizador.
 * Una regla {@code stable} marca conocimiento estable (p. ej. "scrum") y no
 * contribuye facetas.</p>
 */
public interface IntentRule {

    String name();

    IntentFacet facet();

    boolean stable();

    boolean matches(String normalizedText);
}
