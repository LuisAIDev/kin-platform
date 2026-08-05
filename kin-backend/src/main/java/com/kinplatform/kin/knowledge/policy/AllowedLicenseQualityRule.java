package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;

import java.util.Locale;

/**
 * Regla de calidad: valida la licencia del candidato. Si se exige licencia y
 * no está declarada, rechaza; si hay licencias permitidas configuradas y la
 * declarada no está entre ellas, rechaza.
 *
 * <p>Determinista: lectura de {@code meta[license]} (clave aditiva
 * {@link PolicyKeys#META_LICENSE}).</p>
 */
public class AllowedLicenseQualityRule implements QualityRule {

    @Override
    public String name() {
        return "LicenciaPermitida";
    }

    @Override
    public PolicyDecision evaluate(KnowledgeCandidate candidate, SourceTrust trust,
                                   QualityPolicyConfig config) {
        String license = candidate.meta().get(PolicyKeys.META_LICENSE);
        boolean declared = license != null && !license.isBlank();
        if (!declared) {
            if (config.requireLicense()) {
                return PolicyDecision.reject(PolicyCategory.QUALITY, "Licencia no declarada");
            }
            return PolicyDecision.allow(PolicyCategory.QUALITY);
        }
        if (!config.allowedLicenses().isEmpty()
            && !config.allowedLicenses().contains(license.trim().toLowerCase(Locale.ROOT))) {
            return PolicyDecision.reject(PolicyCategory.QUALITY, "Licencia no permitida: " + license);
        }
        return PolicyDecision.allow(PolicyCategory.QUALITY);
    }
}
