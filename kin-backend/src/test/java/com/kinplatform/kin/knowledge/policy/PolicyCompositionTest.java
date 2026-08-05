package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;
import com.kinplatform.kin.knowledge.engine.SourceValidator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que el Knowledge Policy Engine se compone DESPUÉS de la validación
 * de fuentes (que permanece intacta) sin sustituirla.
 */
class PolicyCompositionTest {

    private final SourceValidator validator = new SourceValidator(
        Set.of("example.com"), Duration.ofDays(365), Set.of("application/json"));

    private final KnowledgePolicyEngine engine = new KnowledgePolicyEngine();

    @Test
    void validadorAceptaPeroPoliticaRechazaPorConfianza() {
        var candidate = candidate(Map.of(), OffsetDateTime.now().minusDays(10));

        var validation = validator.validate(candidate);

        assertTrue(validation.accepted());
        var decision = engine.evaluateQuality(candidate, validation.trust(), QualityPolicyConfig.production());
        assertTrue(decision.rejected());
    }

    @Test
    void validadorAceptaYPoliticaAceptaOficial() {
        var candidate = candidate(
            Map.of(SourceValidator.META_SOURCE_TYPE, "official",
                PolicyKeys.META_LANGUAGE, "es"), OffsetDateTime.now().minusDays(10));

        var validation = validator.validate(candidate);

        assertTrue(validation.accepted());
        assertTrue(engine.evaluateQuality(candidate, validation.trust(),
            QualityPolicyConfig.production()).allowed());
    }

    @Test
    void validadorRechazaYPoliticaNuncaLoVee() {
        var candidateHtml = new KnowledgeCandidate(
            "Dato verificado de mercado colombiano con contexto suficiente. ".repeat(6),
            "src-1", "Fuente", "https://example.com/report",
            OffsetDateTime.now().minusDays(10), "text/html",
            Map.of(SourceValidator.META_SOURCE_TYPE, "official"));

        var validation = validator.validate(candidateHtml);

        assertTrue(validation.reasons().stream().anyMatch(r -> r.contains("contenido")));
    }

    @Test
    void politicaDegradaSinRechazoCuandoFaltaIdioma() {
        var candidate = candidate(Map.of(), OffsetDateTime.now());
        var config = new QualityPolicyConfig(SourceTrust.UNVERIFIED, null,
            Set.of("es"), Set.of(), Set.of(), false);

        var validation = validator.validate(candidate);

        assertTrue(validation.accepted());
        var decision = engine.evaluateQuality(candidate, validation.trust(), config);
        assertTrue(decision.degraded());
    }

    private KnowledgeCandidate candidate(Map<String, String> meta, OffsetDateTime publishedAt) {
        return new KnowledgeCandidate(
            "Dato verificado de mercado colombiano con contexto suficiente. ".repeat(6),
            "src-1", "Fuente", "https://example.com/report", publishedAt,
            "application/json", meta == null ? Map.of() : meta);
    }
}
