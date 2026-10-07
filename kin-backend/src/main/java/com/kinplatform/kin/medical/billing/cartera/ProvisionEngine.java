package com.kinplatform.kin.medical.billing.cartera;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Politica de provision de cartera por bucket de maduracion.
 */
@Component
public class ProvisionEngine {

    private static final Map<String, BigDecimal> RATES = Map.of(
            AgingCalculator.CURRENT, new BigDecimal("0.0000"),
            AgingCalculator.BUCKET_1_30, new BigDecimal("0.0500"),
            AgingCalculator.BUCKET_31_60, new BigDecimal("0.1000"),
            AgingCalculator.BUCKET_61_90, new BigDecimal("0.2000"),
            AgingCalculator.BUCKET_91_180, new BigDecimal("0.3500"),
            AgingCalculator.BUCKET_180_PLUS, new BigDecimal("0.5000")
    );

    public BigDecimal rateFor(String agingBucket) {
        return RATES.getOrDefault(agingBucket, BigDecimal.ZERO);
    }

    public BigDecimal provisionValue(BigDecimal pendingValue, String agingBucket) {
        BigDecimal pending = pendingValue == null ? BigDecimal.ZERO : pendingValue;
        return pending.multiply(rateFor(agingBucket)).setScale(2, RoundingMode.HALF_UP);
    }
}

