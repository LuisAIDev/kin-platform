package com.kinplatform.billing.dashboard;

import java.math.BigDecimal;

public record CashFlowPoint(
        String month,
        BigDecimal facturadoCop,
        BigDecimal recaudadoCop,
        BigDecimal projectedCop
) {
}
