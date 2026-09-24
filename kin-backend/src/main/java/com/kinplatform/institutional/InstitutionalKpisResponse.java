package com.kinplatform.institutional;

import com.kinplatform.billing.dashboard.BillingKpisResponse;

import java.util.Map;

public record InstitutionalKpisResponse(
        long branchCount,
        long totalMembers,
        long activeMembers,
        Map<String, Long> membersByRole,
        BillingKpisResponse billing
) {
}
