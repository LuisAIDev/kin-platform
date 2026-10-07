package com.kinplatform.kin.medical.billing.dashboard;

import java.math.BigDecimal;

public record BillingKpisResponse(
        BigDecimal totalFacturadoCop,
        BigDecimal totalGlosasCop,
        BigDecimal glosaRate,
        BigDecimal totalCarteraCop,
        BigDecimal carteraVencidaCop,
        long ripsPendientes,
        long fevAceptadas,
        long fevRechazadas,
        BigDecimal recaudadoCop
) {
}

