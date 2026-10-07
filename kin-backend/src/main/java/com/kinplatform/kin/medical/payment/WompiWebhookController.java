package com.kinplatform.kin.medical.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recibe los eventos de Wompi. El checksum viaja en el header
 * {@code X-Event-Checksum} (o, alternativamente, en
 * {@code signature.checksum} del cuerpo). Devolver 200 confirma la recepción;
 * cualquier otro estado hace que Wompi reintente (hasta 3 veces en 24 h), por
 * lo que los errores de procesamiento devuelven 400 para forzar el reintento y
 * la idempotencia evita dobles activaciones.
 */
@Slf4j
@RestController
@RequestMapping("/wompi")
@RequiredArgsConstructor
public class WompiWebhookController {

    private final WompiService wompiService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {

        try {
            wompiService.handleWebhook(payload, checksum);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.warn("Checksum de webhook Wompi inválido: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            log.error("Error procesando webhook Wompi", e);
            return ResponseEntity.badRequest().build();
        }
    }
}

