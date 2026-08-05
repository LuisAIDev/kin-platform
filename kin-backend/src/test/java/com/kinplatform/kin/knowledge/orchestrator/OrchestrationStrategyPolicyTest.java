package com.kinplatform.kin.knowledge.orchestrator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrchestrationStrategyPolicyTest {

    @Test
    void cacheFirst_deberiaPreferirCache() {
        var policy = new CacheFirstPolicy();

        assertTrue(policy.prefersCache());
        assertFalse(policy.offlineOnly());
        assertFalse(policy.failureIsFatal());
        assertTrue(policy.strategy() == OrchestrationStrategy.CACHE_FIRST);
    }

    @Test
    void providerFirst_deberiaNoPreferirCache() {
        var policy = new ProviderFirstPolicy();

        assertFalse(policy.prefersCache());
        assertFalse(policy.failureIsFatal());
        assertTrue(policy.strategy() == OrchestrationStrategy.PROVIDER_FIRST);
    }

    @Test
    void hybrid_deberiaCombinar() {
        var policy = new HybridPolicy();

        assertFalse(policy.prefersCache());
        assertFalse(policy.offlineOnly());
        assertFalse(policy.failureIsFatal());
        assertTrue(policy.strategy() == OrchestrationStrategy.HYBRID);
    }

    @Test
    void localFirst_deberiaPriorizarLocal() {
        var policy = new LocalFirstPolicy();

        assertFalse(policy.prefersCache());
        assertFalse(policy.failureIsFatal());
        assertTrue(policy.strategy() == OrchestrationStrategy.LOCAL_FIRST);
    }

    @Test
    void internetFirst_deberiaPriorizarInternet() {
        var policy = new InternetFirstPolicy();

        assertFalse(policy.offlineOnly());
        assertFalse(policy.failureIsFatal());
        assertTrue(policy.strategy() == OrchestrationStrategy.INTERNET_FIRST);
    }

    @Test
    void failFast_deberiaSerFatal() {
        var policy = new FailFastPolicy();

        assertTrue(policy.failureIsFatal());
        assertFalse(policy.prefersCache());
        assertFalse(policy.offlineOnly());
        assertTrue(policy.strategy() == OrchestrationStrategy.FAIL_FAST);
    }

    @Test
    void graceful_deberiaDegradar() {
        var policy = new GracefulDegradationPolicy();

        assertFalse(policy.failureIsFatal());
        assertTrue(policy.strategy() == OrchestrationStrategy.GRACEFUL_DEGRADATION);
    }

    @Test
    void offlineMode_deberiaSerSoloLocal() {
        var policy = new OfflineModePolicy();

        assertTrue(policy.offlineOnly());
        assertFalse(policy.failureIsFatal());
        assertFalse(policy.prefersCache());
        assertTrue(policy.strategy() == OrchestrationStrategy.OFFLINE_MODE);
    }
}
