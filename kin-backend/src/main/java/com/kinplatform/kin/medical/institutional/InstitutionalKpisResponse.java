package com.kinplatform.kin.medical.institutional;

import com.kinplatform.kin.medical.billing.dashboard.BillingKpisResponse;

import java.util.Map;

public record InstitutionalKpisResponse(
        long branchCount,
        long totalMembers,
        long activeMembers,
        Map<String, Long> membersByRole,
        BillingKpisResponse billing
) {
}


