package com.kinplatform.kin.knowledge.policy;

/**
 * Veredicto determinista de una política (especificación Fase 2): aceptar,
 * rechazar (con motivo) o degradar (con acción de mitigación).
 *
 * <p>El Knowledge Policy Engine nunca ejecuta: únicamente emite veredictos
 * puros que otros componentes del dominio consumen.</p>
 */
public enum PolicyVerdict {

    ALLOW("Permitir"),
    REJECT("Rechazar"),
    DEGRADE("Degradar");

    private final String displayName;

    PolicyVerdict(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
