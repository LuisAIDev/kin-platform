package com.kinplatform.kin;

import reactor.core.publisher.Flux;

/**
 * Resultado del runtime en modo streaming (aditivo, ADR-013/017).
 *
 * <p>Entrega el {@link Flux} de tokens de la respuesta del LLM (con el
 * safety-net de respuesta segura ya aplicado por {@link KinMethod}) junto con
 * el {@link KinMethodResult} completo del turno, para que la capa de I/O
 * pueda consumir la decisión y el {@code ConsultingReport} sin re-ejecutar el
 * pipeline.</p>
 */
public record StreamingMethodOutcome(
        Flux<String> safeFlux,
        KinMethodResult result
) {

    public StreamingMethodOutcome {
        if (safeFlux == null) {
            throw new IllegalArgumentException("safeFlux no puede ser null");
        }
        if (result == null) {
            throw new IllegalArgumentException("result no puede ser null");
        }
    }
}
