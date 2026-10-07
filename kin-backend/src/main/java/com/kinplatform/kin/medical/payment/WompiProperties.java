package com.kinplatform.kin.medical.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales y URLs de Wompi (Bancolombia) para pagos locales en Colombia.
 *
 * <p>Todos los valores llegan por variables de entorno; nunca se versionan
 * secretos. La validación de presencia NO se hace con {@code @NotBlank} a
 * propósito: KIN debe arrancar aunque Wompi no esté configurado (Stripe sigue
 * siendo la pasarela por defecto). El fallo, si falta configuración, ocurre en
 * el momento de crear un checkout o de validar un webhook, con un mensaje
 * explícito en lugar de impedir el arranque de toda la aplicación.</p>
 *
 * <p>Las llaves {@code pub_prod_*}, {@code prv_prod_*} y {@code prod_integrity_*}
 * viven en el entorno. El {@code eventsSecret} (secreto de eventos) es distinto
 * de la llave privada y se usa para validar el checksum del webhook.</p>
 */
@ConfigurationProperties(prefix = "payment.wompi")
public class WompiProperties {

    /** Llave pública del comercio (pub_prod_*). */
    private String publicKey;

    /** Llave privada del comercio (prv_prod_*). Solo para llamadas server-side a la API. */
    private String privateKey;

    /** Secreto de integridad (prod_integrity_*) para firmar transacciones. */
    private String integritySecret;

    /** Secreto de eventos para validar el checksum del webhook. Distinto de la llave privada. */
    private String eventsSecret;

    /** Base de la API server-side (sin /v1). */
    private String baseUrl = "https://production.wompi.co";

    /** URL del Web Checkout al que se redirige al usuario. */
    private String checkoutUrl = "https://checkout.wompi.co/p/";

    /** Si es true, usa el entorno sandbox de Wompi. */
    private boolean sandbox = false;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getIntegritySecret() {
        return integritySecret;
    }

    public void setIntegritySecret(String integritySecret) {
        this.integritySecret = integritySecret;
    }

    public String getEventsSecret() {
        return eventsSecret;
    }

    public void setEventsSecret(String eventsSecret) {
        this.eventsSecret = eventsSecret;
    }

    public String getBaseUrl() {
        return sandbox ? "https://sandbox.wompi.co" : baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }
}

