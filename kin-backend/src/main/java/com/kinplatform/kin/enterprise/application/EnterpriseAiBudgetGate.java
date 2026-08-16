package com.kinplatform.kin.enterprise.application;

import java.util.UUID;

/**
 * Gate de presupuesto de IA para la generación Enterprise (Fase 1).
 *
 * <p>La generación del proyecto empresarial ejecuta llamadas al LLM
 * (narrativa) fuera del chat, por lo que DEBE pasar por la misma autoridad de
 * presupuesto (reserva atómica + reconciliación con el uso real). El gate
 * resuelve el propietario del proyecto, estima el peor caso, reserva y deja la
 * reserva vigente en el hilo de generación. Si no hay presupuesto, devuelve
 * {@code false} y la generación NO debe invocar a DeepSeek.</p>
 *
 * <p>Es un puerto de dominio: la implementación vive en la capa de
 * infraestructura y reutiliza {@code AiBudgetControlService} /
 * {@code ReservationContext} (no crea un segundo sistema de cuotas).</p>
 */
public interface EnterpriseAiBudgetGate {

    /**
     * Reserva presupuesto para la generación del proyecto. Deja la reserva
     * vigente en el hilo (para atribuir el uso real). Devuelve {@code false}
     * si no hay presupuesto suficiente (la IA no debe ejecutarse).
     */
    boolean reserve(UUID projectId);

    /** Libera el contexto de reserva del hilo tras la generación. */
    void clear();
}
