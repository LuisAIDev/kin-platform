package com.kinplatform.kin.enterprise.integration;

/**
 * Estado de un dato en la capa de integración Enterprise.
 *
 * <p>Reemplaza la semántica ambigua de {@code 0}, "Por definir" y "Sin definir":
 * un dato ausente nunca se representa como {@code 0}; se marca explícitamente
 * como {@code PENDING} (todavía no proporcionado) o {@code NOT_AVAILABLE} (no
 * disponible o no calculable con los datos actuales).</p>
 */
public enum DataState {

    /** Dato real confirmado (proporcionado por el usuario, o cero real). */
    CONFIRMED,

    /** Dato importado de un documento. */
    IMPORTED,

    /** Resultado obtenido matemáticamente a partir de otros datos. */
    CALCULATED,

    /** Valor supuesto para una simulación (no confirmado). */
    ESTIMATED,

    /** Propuesta generada por IA, todavía no confirmada por el usuario. */
    AI_SUGGESTED,

    /** El dato todavía no ha sido proporcionado. */
    PENDING,

    /** El dato no está disponible o no es calculable con los datos actuales. */
    NOT_AVAILABLE
}
