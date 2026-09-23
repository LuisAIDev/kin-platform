package com.kinplatform.billing.dashboard;

import java.math.BigDecimal;

public record EpsPerformance(
        String epsNit,
        String epsName,
        BigDecimal facturadoCop,
        BigDecimal glosasCop,
        BigDecimal carteraCop,
        long glosasCount,
        Long avgPaymentDays
) {
}
