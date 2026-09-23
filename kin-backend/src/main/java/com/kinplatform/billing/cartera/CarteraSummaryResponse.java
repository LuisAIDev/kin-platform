package com.kinplatform.billing.cartera;

import java.math.BigDecimal;
import java.util.Map;

public record CarteraSummaryResponse(
        BigDecimal totalPendingCop,
        BigDecimal totalProvisionCop,
        long overdueCount,
        Map<String, BigDecimal> pendingByBucket
) {
}
