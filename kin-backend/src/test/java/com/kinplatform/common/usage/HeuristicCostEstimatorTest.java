package com.kinplatform.common.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class HeuristicCostEstimatorTest {

    @Test
    void estimateInputTokens_caracteresDivididoCuatro() {
        var estimator = new HeuristicCostEstimator(BigDecimal.ONE, BigDecimal.ONE, 4096);
        // 80 caracteres -> 20 tokens
        String user = "a".repeat(80);
        assertEquals(20L, estimator.estimateInputTokens(user, List.of()));
    }

    @Test
    void estimateInputTokens_contabilizaHistorial() {
        var estimator = new HeuristicCostEstimator(BigDecimal.ONE, BigDecimal.ONE, 4096);
        String user = "a".repeat(40);
        List<String> history = List.of("b".repeat(40));
        assertEquals(20L, estimator.estimateInputTokens(user, history));
    }

    @Test
    void estimateInputTokens_minimoUno() {
        var estimator = new HeuristicCostEstimator(BigDecimal.ONE, BigDecimal.ONE, 4096);
        assertEquals(1L, estimator.estimateInputTokens("ab", List.of()));
    }

    @Test
    void costOf_aplicaPreciosPorMillon() {
        var estimator = new HeuristicCostEstimator(new BigDecimal("1.00"), new BigDecimal("2.00"), 4096);
        // 1.000.000 input @ $1/1M = $1.00 ; 500.000 output @ $2/1M = $1.00
        BigDecimal cost = estimator.costOf(1_000_000, 500_000);
        assertEquals(new BigDecimal("2.000000"), cost);
    }

    @Test
    void estimate_usaTopeDeSalidaConfigurado() {
        var estimator = new HeuristicCostEstimator(new BigDecimal("1.00"), new BigDecimal("1.00"), 4_096);
        // 40 chars input -> 10 tokens @ $1/1M = 0.000010 ; output 4096 @ $1/1M = 0.004096
        BigDecimal estimate = estimator.estimate("a".repeat(40), List.of());
        assertEquals(new BigDecimal("0.004106"), estimate);
    }

    @Test
    void preciosCero_devuelvenCero() {
        var estimator = new HeuristicCostEstimator(BigDecimal.ZERO, BigDecimal.ZERO, 4096);
        assertEquals(0, estimator.costOf(1_000_000, 1_000_000).compareTo(BigDecimal.ZERO));
        assertEquals(0, estimator.estimate("abc", List.of()).compareTo(BigDecimal.ZERO));
    }
}



