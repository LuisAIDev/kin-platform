package com.kinplatform.ai.usage;

import com.kinplatform.platform.usage.AiReservation;
import org.springframework.stereotype.Component;

/**
 * Contexto de la reserva de presupuesto de IA vigente en el hilo de la
 * solicitud. Se establece en el orquestador de chat antes de ejecutar el
 * pipeline y lo lee el proveedor (DeepSeekProvider) para atribuir el uso real
 * de tokens a la reserva correcta.
 */
@Component
public class ReservationContext {

    private static final ThreadLocal<AiReservation> CURRENT = new ThreadLocal<>();

    public AiReservation current() {
        return CURRENT.get();
    }

    public void set(AiReservation reservation) {
        CURRENT.set(reservation);
    }

    public void clear() {
        CURRENT.remove();
    }
}

