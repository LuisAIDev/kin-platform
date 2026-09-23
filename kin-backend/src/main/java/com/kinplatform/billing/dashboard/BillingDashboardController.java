package com.kinplatform.billing.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing/dashboard")
@RequiredArgsConstructor
public class BillingDashboardController {

    private final BillingDashboardService billingDashboardService;

    @GetMapping("/kpis")
    public ResponseEntity<BillingKpisResponse> kpis(@RequestParam(required = false) UUID contractId,
                                                    @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(billingDashboardService.kpis(contractId, periodo));
    }

    @GetMapping("/cash-flow")
    public ResponseEntity<List<CashFlowPoint>> cashFlow(@RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(billingDashboardService.cashFlow(months));
    }

    @GetMapping("/eps-performance")
    public ResponseEntity<List<EpsPerformance>> epsPerformance() {
        return ResponseEntity.ok(billingDashboardService.epsPerformance());
    }
}
