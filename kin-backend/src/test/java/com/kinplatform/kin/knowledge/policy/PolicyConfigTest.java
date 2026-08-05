package com.kinplatform.kin.knowledge.policy;

import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyConfigTest {

    @Test
    void defaults_deberiaCompletarConfiguracionesAusentes() {
        var config = new PolicyConfig(null, null, null, null, null);

        assertNotNull(config.query());
        assertNotNull(config.provider());
        assertNotNull(config.quality());
        assertNotNull(config.cost());
        assertNotNull(config.context());
    }

    @Test
    void defaults_deberiaTenerValoresBase() {
        var config = PolicyConfig.defaults();

        assertTrue(config.query().stableTopics().contains("scrum"));
        assertTrue(config.query().cacheFirst());
        assertEquals(5, config.provider().maxProviders());
        assertEquals(SourceTrust.UNVERIFIED, config.quality().minTrust());
        assertEquals(10, config.cost().maxQueries());
        assertEquals(10, config.context().maxFragments());
    }

    @Test
    void dev_deberiaSerPermisivo() {
        var config = PolicyConfig.dev();

        assertEquals(50, config.cost().maxQueries());
        assertEquals(20, config.context().maxFragments());
        assertTrue(config.quality().allowedLanguages().isEmpty());
        assertTrue(config.query().stableTopics().isEmpty());
    }

    @Test
    void production_deberiaSerConservador() {
        var config = PolicyConfig.production();

        assertEquals(SourceTrust.SECONDARY, config.quality().minTrust());
        assertTrue(config.quality().allowedLanguages().contains("es"));
        assertTrue(config.provider().excludedSourceTypes().contains("social"));
    }

    @Test
    void testing_deberiaSerEstrictoYDeterminista() {
        var config = PolicyConfig.testing();

        assertFalse(config.query().cacheFirst());
        assertEquals(2, config.provider().maxProviders());
        assertEquals(2, config.cost().maxQueries());
        assertEquals(3, config.context().maxFragments());
        assertTrue(config.quality().requireLicense());
    }

    @Test
    void enterprise_deberiaSerEstrictoEnCalidad() {
        var config = PolicyConfig.enterprise();

        assertTrue(config.quality().requireLicense());
        assertTrue(config.quality().allowedLicenses().contains("cc-by"));
        assertEquals(10, config.provider().maxProviders());
        assertEquals(100, config.cost().maxQueries());
    }

    @Test
    void providerConfig_prioridadNormalizadaEnMinusculas() {
        var config = new ProviderPolicyConfig(Set.of(), 5, Map.of("Government", 100));

        assertEquals(100, config.priorityOf("government"));
        assertEquals(100, config.priorityOf("GOVERNMENT"));
        assertEquals(0, config.priorityOf("desconocido"));
    }

    @Test
    void providerConfig_prioridadNula_trataComoCero() {
        var prioridades = new java.util.HashMap<String, Integer>();
        prioridades.put("web_search", null);

        var config = new ProviderPolicyConfig(null, 3, prioridades);

        assertTrue(config.excludedSourceTypes().isEmpty());
        assertEquals(0, config.priorityOf("web_search"));
    }

    @Test
    void queryConfig_ventanaPorDefecto() {
        var config = new QueryPolicyConfig(null, true, null);

        assertTrue(config.stableTopics().isEmpty());
        assertEquals(QueryPolicyConfig.DEFAULT_CACHE_TTL, config.cacheTtl());
    }

    @Test
    void qualityConfig_valoresPorDefecto() {
        var config = new QualityPolicyConfig(null, null, null, null, null, false);

        assertEquals(SourceTrust.UNVERIFIED, config.minTrust());
        assertEquals(QualityPolicyConfig.DEFAULT_MAX_AGE, config.maxAge());
        assertTrue(config.allowedLanguages().isEmpty());
    }

    @Test
    void costConfig_timeoutPorDefecto() {
        var config = new CostPolicyConfig(1, 1, null);

        assertEquals(CostPolicyConfig.DEFAULT_TIMEOUT, config.maxTimeout());
        assertEquals(Duration.ofSeconds(15), CostPolicyConfig.production().maxTimeout());
    }

    @Test
    void configs_deberianAcotarValoresNegativos() {
        var provider = new ProviderPolicyConfig(Set.of(), -5, Map.of());
        var cost = new CostPolicyConfig(-1, -1, null);
        var context = new ContextPolicyConfig(-1, -1, -1, -1);

        assertEquals(0, provider.maxProviders());
        assertEquals(0, cost.maxQueries());
        assertEquals(0, cost.maxExternalCalls());
        assertEquals(0, context.maxFragments());
        assertEquals(0, context.tokenBudget());
        assertEquals(0, context.maxContextSize());
    }
}
