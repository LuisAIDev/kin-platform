package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityPolicyTest {

    private static final SourceTrust SECONDARY = SourceTrust.SECONDARY;
    private static final SourceTrust UNVERIFIED = SourceTrust.UNVERIFIED;
    private static final SourceTrust OFFICIAL = SourceTrust.OFFICIAL_PUBLIC;

    private KnowledgeCandidate candidate(Map<String, String> meta) {
        return new KnowledgeCandidate(
            "Dato verificado de mercado colombiano con contexto suficiente. ".repeat(6),
            "src-1", "Fuente", "https://example.com/report",
            OffsetDateTime.now().minusDays(10), "application/json",
            meta == null ? Map.of() : meta);
    }

    private KnowledgeCandidate candidateAt(OffsetDateTime publishedAt) {
        return new KnowledgeCandidate("Contenido de prueba con texto suficiente para validar.",
            "src-1", "Fuente", "https://example.com/report", publishedAt,
            "application/json", Map.of());
    }

    @Test
    void minimumTrust_confianzaBaja_deberiaRechazar() {
        var rule = new MinimumTrustQualityRule();

        var decision = rule.evaluate(candidate(Map.of()), UNVERIFIED, QualityPolicyConfig.production());

        assertTrue(decision.rejected());
        assertEquals(PolicyCategory.QUALITY, decision.category());
        assertEquals(PolicyVerdict.REJECT, decision.verdict());
    }

    @Test
    void minimumTrust_confianzaSuficiente_deberiaPermitir() {
        var rule = new MinimumTrustQualityRule();

        assertTrue(rule.evaluate(candidate(Map.of()), OFFICIAL, QualityPolicyConfig.production()).allowed());
        assertTrue(rule.evaluate(candidate(Map.of()), SECONDARY, QualityPolicyConfig.production()).allowed());
    }

    @Test
    void maxAge_antiguo_deberiaRechazar() {
        var rule = new MaxAgeQualityRule();
        var config = new QualityPolicyConfig(null, Duration.ofDays(30), Set.of(), Set.of(), Set.of(), false);

        var decision = rule.evaluate(candidateAt(OffsetDateTime.now().minusDays(90)), SECONDARY, config);

        assertTrue(decision.rejected());
    }

    @Test
    void maxAge_fresco_deberiaPermitir() {
        var rule = new MaxAgeQualityRule();

        assertTrue(rule.evaluate(candidateAt(OffsetDateTime.now()), SECONDARY,
            QualityPolicyConfig.defaults()).allowed());
    }

    @Test
    void maxAge_sinFecha_deberiaPermitir() {
        var rule = new MaxAgeQualityRule();

        assertTrue(rule.evaluate(candidateAt(null), SECONDARY,
            QualityPolicyConfig.defaults()).allowed());
    }

    @Test
    void language_declaradoNoPermitido_deberiaRechazar() {
        var rule = new AllowedLanguageQualityRule();

        var decision = rule.evaluate(candidate(Map.of(PolicyKeys.META_LANGUAGE, "pt")),
            SECONDARY, QualityPolicyConfig.production());

        assertTrue(decision.rejected());
    }

    @Test
    void language_declaradoPermitido_deberiaPermitir() {
        var rule = new AllowedLanguageQualityRule();

        assertTrue(rule.evaluate(candidate(Map.of(PolicyKeys.META_LANGUAGE, "ES")),
            SECONDARY, QualityPolicyConfig.production()).allowed());
    }

    @Test
    void language_ausente_deberiaDegradar() {
        var rule = new AllowedLanguageQualityRule();

        var decision = rule.evaluate(candidate(Map.of()), SECONDARY, QualityPolicyConfig.production());

        assertTrue(decision.degraded());
        assertEquals("ignorar_idioma", decision.action());
    }

    @Test
    void language_sinRestriccion_deberiaPermitir() {
        var rule = new AllowedLanguageQualityRule();

        assertTrue(rule.evaluate(candidate(Map.of(PolicyKeys.META_LANGUAGE, "xx")),
            SECONDARY, QualityPolicyConfig.dev()).allowed());
    }

    @Test
    void sourceType_noPermitido_deberiaRechazar() {
        var rule = new AllowedSourceTypeQualityRule();
        var config = new QualityPolicyConfig(null, null, Set.of(), Set.of("government"), Set.of(), false);

        var decision = rule.evaluate(candidate(Map.of(SourceValidator.META_SOURCE_TYPE, "web_search")),
            SECONDARY, config);

        assertTrue(decision.rejected());
    }

    @Test
    void sourceType_ausente_deberiaDegradar() {
        var rule = new AllowedSourceTypeQualityRule();
        var config = new QualityPolicyConfig(null, null, Set.of(), Set.of("government"), Set.of(), false);

        var decision = rule.evaluate(candidate(Map.of()), SECONDARY, config);

        assertTrue(decision.degraded());
        assertEquals("ignorar_tipo_fuente", decision.action());
    }

    @Test
    void sourceType_permitido_deberiaPermitir() {
        var rule = new AllowedSourceTypeQualityRule();
        var config = new QualityPolicyConfig(null, null, Set.of(), Set.of("government"), Set.of(), false);

        assertTrue(rule.evaluate(candidate(Map.of(SourceValidator.META_SOURCE_TYPE, "government")),
            SECONDARY, config).allowed());
    }

    @Test
    void license_requeridaAusente_deberiaRechazar() {
        var rule = new AllowedLicenseQualityRule();

        var decision = rule.evaluate(candidate(Map.of()), SECONDARY, QualityPolicyConfig.enterprise());

        assertTrue(decision.rejected());
        assertTrue(decision.reasons().stream().anyMatch(r -> r.contains("Licencia")));
    }

    @Test
    void license_noPermitida_deberiaRechazar() {
        var rule = new AllowedLicenseQualityRule();

        var decision = rule.evaluate(candidate(Map.of(PolicyKeys.META_LICENSE, "cc-by-nc")),
            SECONDARY, QualityPolicyConfig.enterprise());

        assertTrue(decision.rejected());
    }

    @Test
    void license_permitida_deberiaPermitir() {
        var rule = new AllowedLicenseQualityRule();

        assertTrue(rule.evaluate(candidate(Map.of(PolicyKeys.META_LICENSE, "CC-BY")),
            SECONDARY, QualityPolicyConfig.enterprise()).allowed());
    }

    @Test
    void license_ausenteSinRequisito_deberiaPermitir() {
        var rule = new AllowedLicenseQualityRule();

        assertTrue(rule.evaluate(candidate(Map.of()), SECONDARY, QualityPolicyConfig.production()).allowed());
    }

    @Test
    void rules_deberianDeclararNombreYCategoria() {
        assertEquals("ConfianzaMinima", new MinimumTrustQualityRule().name());
        assertEquals("AntiguedadMaxima", new MaxAgeQualityRule().name());
        assertEquals("IdiomaPermitido", new AllowedLanguageQualityRule().name());
        assertEquals("TipoDeFuentePermitido", new AllowedSourceTypeQualityRule().name());
        assertEquals("LicenciaPermitida", new AllowedLicenseQualityRule().name());
        assertEquals(PolicyCategory.QUALITY, new MinimumTrustQualityRule().category());
        assertEquals(PolicyCategory.QUALITY, new AllowedLicenseQualityRule().category());
    }

    private static final class SourceTypeMeta {
        private static final String META = "source_type";
    }
}
