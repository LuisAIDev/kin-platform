package com.kinplatform.billing.fev;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Cliente DIAN simulado para desarrollo y tests.
 *
 * NO contacta el sandbox real de la DIAN. La integracion real (envio SOAP/REST,
 * firma, validacion de CUFE y polling de estado) vive en {@link DianHttpClient}.
 */
@Component
@Profile("!prod")
@Slf4j
public class DianStubClient implements DianClient {

    @Override
    public DianResponse send(FevRipsInvoice invoice) {
        String cufe = invoice.getDianCufe() != null ? invoice.getDianCufe() : "STUB-CUFE";
        String responseXml = "<DianResponse><StatusCode>00</StatusCode><IsValid>true</IsValid>"
                + "<DocumentKey>" + cufe + "</DocumentKey></DianResponse>";
        log.debug("Envio simulado a DIAN (STUB) invoice={} cufe={}", invoice.getId(), cufe);
        return new DianResponse(true, cufe, "00", "Aceptado (STUB)", responseXml);
    }

    @Override
    public DianStatusResponse queryStatus(String cufe) {
        return new DianStatusResponse(cufe, "ACCEPTED", "Estado simulado (STUB)");
    }
}
