package com.kinplatform.kin.medical.billing.authorization;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MipresAuthorizationService {

    @Value("${mipres.organization-nit:}")
    private String configuredNit;

    public String getOrganizationNit(UUID organizationId) {
        if (configuredNit != null && !configuredNit.isBlank()) {
            return configuredNit;
        }
        // Fallback: en producción el NIT debería venir de configuración
        throw new IllegalStateException("MIPRES_ORGANIZATION_NIT no configurado. Configure mipres.organization-nit en application.yml");
    }
}
