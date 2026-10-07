package com.kinplatform.platform.usage;

import java.util.UUID;

/**
 * Puerto de la cuota persistente de proyectos COMPLETADOS por período.
 *
 * <p>El contador está asociado al usuario (no al número de registros
 * existentes): eliminar un proyecto NO devuelve cupo, evitando el abuso
 * {@code crear → completar → eliminar → crear}. El rollover mensual lo
 * gestiona el backend.</p>
 */
public interface ProjectQuotaPort {

    /** Proyectos completados por el usuario en el período vigente. */
    int completedProjects(UUID userId);

    /**
     * ¿Puede el usuario completar otro proyecto? {@code limit} {@code null} =
     * ilimitado (PREMIUM).
     */
    boolean canComplete(UUID userId, Integer limit);

    /**
     * Incrementa el contador persistente en una unidad (transición a
     * COMPLETED) de forma atómica y condicional: solo si el usuario no supera
     * el límite. No decrementa nunca al eliminar proyectos.
     *
     * @return {@code true} si se aplicó el incremento (límite respetado).
     */
    boolean tryIncrementCompleted(UUID userId, Integer limit);

    /** Resetea el contador si el período cambió (rollover mensual). */
    void rolloverIfNeeded(UUID userId);
}

