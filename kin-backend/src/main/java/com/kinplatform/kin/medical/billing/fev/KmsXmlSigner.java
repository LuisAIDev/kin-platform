package com.kinplatform.kin.medical.billing.fev;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Firma XML productiva con certificado X.509 desde HSM / AWS KMS.
 *
 * PENDIENTE DE IMPLEMENTACION (Dia 7+): requiere credenciales KMS, alias de
 * certificado DIAN y canonicalizacion C14N. No incluir secretos en el repo.
 */
@Component
@Profile("prod")
public class KmsXmlSigner implements XmlSigner {

    @Override
    public Signature sign(String xml) {
        throw new UnsupportedOperationException(
                "Firma productiva KMS/HSM no implementada. Configurar certificado X.509 DIAN.");
    }
}

