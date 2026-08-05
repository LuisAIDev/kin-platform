package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.SourceTrust;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Configuración de políticas de calidad (especificación Fase 2): confianza
 * mínima, antigüedad máxima, idiomas permitidos, tipos de fuente permitidos y
 * licencias permitidas. Complementa a la validación de fuentes (que permanece
 * intacta); nunca la sustituye.
 *
 * <p>Inmutable y determinista; varía por entorno sin tocar el código.</p>
 */
public record QualityPolicyConfig(
    SourceTrust minTrust,
    Duration maxAge,
    Set<String> allowedLanguages,
    Set<String> allowedSourceTypes,
    Set<String> allowedLicenses,
    boolean requireLicense
) {

    public static final Duration DEFAULT_MAX_AGE = Duration.ofDays(365);

    public QualityPolicyConfig {
        minTrust = minTrust == null ? SourceTrust.UNVERIFIED : minTrust;
        maxAge = maxAge == null ? DEFAULT_MAX_AGE : maxAge;
        allowedLanguages = lowerCaseSet(allowedLanguages);
        allowedSourceTypes = lowerCaseSet(allowedSourceTypes);
        allowedLicenses = lowerCaseSet(allowedLicenses);
    }

    public static QualityPolicyConfig defaults() {
        return new QualityPolicyConfig(SourceTrust.UNVERIFIED, DEFAULT_MAX_AGE,
            Set.of(), Set.of(), Set.of(), false);
    }

    public static QualityPolicyConfig dev() {
        return new QualityPolicyConfig(SourceTrust.UNVERIFIED, DEFAULT_MAX_AGE,
            Set.of(), Set.of(), Set.of(), false);
    }

    public static QualityPolicyConfig production() {
        return new QualityPolicyConfig(SourceTrust.SECONDARY, Duration.ofDays(180),
            Set.of("es", "en"), Set.of(), Set.of(), false);
    }

    public static QualityPolicyConfig testing() {
        return new QualityPolicyConfig(SourceTrust.SECONDARY, Duration.ofDays(30),
            Set.of("es"), Set.of(), Set.of(), true);
    }

    public static QualityPolicyConfig enterprise() {
        return new QualityPolicyConfig(SourceTrust.SECONDARY, Duration.ofDays(90),
            Set.of("es", "en"), Set.of(), Set.of("cc-by", "public", "government"), true);
    }

    private static Set<String> lowerCaseSet(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        var out = new LinkedHashSet<String>();
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(out);
    }
}
