package com.kinplatform.billing.fev;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Cliente productivo DIAN (sandbox/produccion).
 *
 * PENDIENTE DE IMPLEMENTACION (Dia 7+): requiere certificado habilitado,
 * resolucion de facturacion activa y URL del WSDL/servicio DIAN. No incluir
 * credenciales en el repo.
 */
@Component
@Profile("prod")
public class DianHttpClient implements DianClient {

    @Override
    public DianResponse send(FevRipsInvoice invoice) {
        throw new UnsupportedOperationException(
                "Envio productivo a DIAN no implementado. Configurar certificado y servicio DIAN.");
    }

    @Override
    public DianStatusResponse queryStatus(String cufe) {
        throw new UnsupportedOperationException(
                "Consulta productiva de estado DIAN no implementada.");
    }
}
