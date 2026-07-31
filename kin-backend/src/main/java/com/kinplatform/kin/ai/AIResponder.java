package com.kinplatform.kin.ai;

import reactor.core.publisher.Flux;

/**
 * Puerto del proveedor de IA.
 *
 * <p>Desacopla el pipeline del proveedor concreto: el dominio solo conoce este
 * contrato y el {@link PromptAssembler}; la implementación (actualmente
 * {@code AiEngineService} con router de proveedores) es un adaptador.</p>
 *
 * <p>El dominio acepta {@link Flux} en la variante streaming porque la pila de
 * IA es reactiva (Spring AI); es la excepción pragmática que mantiene la
 * semántica de backpressure del proveedor.</p>
 */
public interface AIResponder {

    String respond(AIRequest request);

    Flux<String> respondStream(AIRequest request);
}
