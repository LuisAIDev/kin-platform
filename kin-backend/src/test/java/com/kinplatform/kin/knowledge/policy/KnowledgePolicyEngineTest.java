package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.KnowledgeRequest;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgePolicyEngineTest {

    private final KnowledgePolicyEngine engine = new KnowledgePolicyEngine();

    @Test
    void decideQuery_temaEstable_deberiaSoloModelo() {
        var result = engine.decideQuery(KnowledgeRequest.of("scrum", List.of()), QueryPolicyConfig.defaults());

        assertTrue(result.modelOnly());
    }

    @Test
    void decideQuery_requestNulo_deberiaExterno() {
        var result = engine.decideQuery(null, QueryPolicyConfig.defaults());

        assertTrue(result.consultExternal());
    }

    @Test
    void decideQuery_temaVacio_deberiaExterno() {
        var result = engine.decideQuery(KnowledgeRequest.of("   ", List.of()), QueryPolicyConfig.defaults());

        assertTrue(result.consultExternal());
    }

    @Test
    void decideQuery_configNula_deberiaUsarDefaults() {
        var result = engine.decideQuery(KnowledgeRequest.of("panadería", List.of("pan")), null);

        assertTrue(result.consultExternal());
    }

    @Test
    void decideQuery_sinReglas_deberiaExterno() {
        var empty = new KnowledgePolicyEngine(List.of(), List.of(), List.of(), List.of(), List.of());

        assertTrue(empty.decideQuery(KnowledgeRequest.of("scrum", List.of()), QueryPolicyConfig.defaults())
            .consultExternal());
    }

    @Test
    void motorConReglasNulas_deberiaUsarDefaults() {
        var withDefaults = new KnowledgePolicyEngine(null, null, null, null, null);

        assertTrue(withDefaults.decideQuery(KnowledgeRequest.of("scrum", List.of()),
            QueryPolicyConfig.defaults()).modelOnly());
    }

    @Test
    void selectProviders_ordenYLimite_deberianCumplirse() {
        var selection = engine.selectProviders(
            Set.of("web_search", "government", "document", "statistics"), ProviderPolicyConfig.testing());

        assertEquals("government", selection.allowedTypes().get(0));
        assertEquals(2, selection.allowedTypes().size());
    }

    @Test
    void selectProviders_conjuntoNulo_deberiaVacio() {
        assertTrue(engine.selectProviders(null, ProviderPolicyConfig.defaults()).isEmpty());
    }

    @Test
    void evaluateQuality_confianzaBaja_deberiaRechazar() {
        var candidate = candidate(Map.of());
        var config = QualityPolicyConfig.production();

        var decision = engine.evaluateQuality(candidate, SourceTrust.UNVERIFIED, config);

        assertTrue(decision.rejected());
        assertEquals(PolicyCategory.QUALITY, decision.category());
    }

    @Test
    void evaluateQuality_trustNulo_deberiaTratarNoVerificada() {
        var candidate = candidate(Map.of());
        var config = QualityPolicyConfig.production();

        assertTrue(engine.evaluateQuality(candidate, null, config).rejected());
    }

    @Test
    void evaluateQuality_degradacionSinRechazo_deberiaDegradar() {
        var config = new QualityPolicyConfig(null, null, Set.of("es"), Set.of(), Set.of(), false);

        var decision = engine.evaluateQuality(candidate(Map.of()), SourceTrust.SECONDARY, config);

        assertTrue(decision.degraded());
        assertEquals("ignorar_idioma", decision.action());
    }

    @Test
    void evaluateQuality_recienValido_deberiaPermitir() {
        var config = new QualityPolicyConfig(null, null, Set.of("es"), Set.of(), Set.of(), false);
        var candidate = candidate(Map.of(PolicyKeys.META_LANGUAGE, "es"));

        assertTrue(engine.evaluateQuality(candidate, SourceTrust.SECONDARY, config).allowed());
    }

    @Test
    void evaluateQuality_configNula_deberiaUsarDefaults() {
        assertTrue(engine.evaluateQuality(candidate(Map.of()), SourceTrust.SECONDARY, null).allowed());
    }

    @Test
    void evaluateQuality_sinReglas_deberiaPermitir() {
        var empty = new KnowledgePolicyEngine(List.of(), List.of(), List.of(), List.of(), List.of());

        assertTrue(empty.evaluateQuality(candidate(Map.of()), SourceTrust.UNVERIFIED,
            QualityPolicyConfig.production()).allowed());
    }

    @Test
    void checkBudget_agotado_deberiaRechazarConMotivo() {
        var decision = engine.checkBudget(new CostBudgetUsage(2, 1), CostPolicyConfig.testing());

        assertTrue(decision.rejected());
        assertTrue(decision.reasons().stream().anyMatch(r -> r.contains("consultas")));
    }

    @Test
    void checkBudget_usoNuloYConfigNula_deberiaPermitir() {
        assertTrue(engine.checkBudget(null, null).allowed());
    }

    @Test
    void checkBudget_dentro_deberiaPermitir() {
        assertTrue(engine.checkBudget(new CostBudgetUsage(1, 0), CostPolicyConfig.testing()).allowed());
    }

    @Test
    void checkContext_excedido_deberiaRechazar() {
        assertTrue(engine.checkContext(new ContextBudget(10, 0, 0), ContextPolicyConfig.testing())
            .rejected());
    }

    @Test
    void checkContext_presupuestoNuloYConfigNula_deberiaPermitir() {
        assertTrue(engine.checkContext(null, null).allowed());
    }

    @Test
    void checkContext_dentro_deberiaPermitir() {
        assertTrue(engine.checkContext(new ContextBudget(2, 200, 1000), ContextPolicyConfig.testing())
            .allowed());
    }

    @Test
    void determinismo_mismaEntradaMismaDecision() {
        var request = KnowledgeRequest.of("café colombiano", List.of("colombia"));

        var r1 = engine.decideQuery(request, QueryPolicyConfig.defaults());
        var r2 = engine.decideQuery(request, QueryPolicyConfig.defaults());

        assertEquals(r1, r2);
    }

    @Test
    void policyDecision_fabricasYSeguridadDeNulos() {
        var decision = new PolicyDecision(null, null, null, null);

        assertEquals(PolicyCategory.QUALITY, decision.category());
        assertEquals(PolicyVerdict.ALLOW, decision.verdict());
        assertTrue(decision.reasons().isEmpty());
        assertEquals("", decision.action());
        assertTrue(decision.allowed());
        assertTrue(PolicyDecision.allow(PolicyCategory.QUERY).allowed());
        assertTrue(PolicyDecision.reject(PolicyCategory.COST, "motivo").rejected());
        assertTrue(PolicyDecision.degrade(PolicyCategory.QUALITY, "motivo", "accion").degraded());
    }

    @Test
    void policyKeys_deberianExistir() {
        assertEquals("language", PolicyKeys.META_LANGUAGE);
        assertEquals("license", PolicyKeys.META_LICENSE);
    }

    @Test
    void selectProviders_conEntradasMixtas_deberiaFiltrar() {
        var candidatos = new HashSet<>(Arrays.asList(" ", null, "government", "social"));

        var selection = engine.selectProviders(candidatos, ProviderPolicyConfig.production());

        assertEquals(List.of("government"), selection.allowedTypes());
        assertTrue(selection.rejectedTypes().contains("social"));
    }

    private KnowledgeCandidate candidate(Map<String, String> meta) {
        return new KnowledgeCandidate(
            "Dato verificado de mercado colombiano con contexto suficiente. ".repeat(6),
            "src-1", "Fuente", "https://example.com/report",
            OffsetDateTime.now().minusDays(10), "application/json",
            meta == null ? Map.of() : meta);
    }
}
