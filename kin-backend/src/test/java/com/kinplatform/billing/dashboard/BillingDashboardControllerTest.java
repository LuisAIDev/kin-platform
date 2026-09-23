package com.kinplatform.billing.dashboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BillingDashboardControllerTest {

    @Mock
    private BillingDashboardService billingDashboardService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new BillingDashboardController(billingDashboardService)).build();
    }

    @Test
    void kpis_returnsAggregatedNumbers() throws Exception {
        when(billingDashboardService.kpis(isNull(), isNull())).thenReturn(new BillingKpisResponse(
                new BigDecimal("150000"), new BigDecimal("20000"), new BigDecimal("0.1333"),
                new BigDecimal("70000"), new BigDecimal("70000"), 1, 1, 1, new BigDecimal("30000")));

        mockMvc().perform(get("/billing/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFacturadoCop").isNumber())
                .andExpect(jsonPath("$.ripsPendientes").value(1));
    }

    @Test
    void cashFlow_returnsPoints() throws Exception {
        when(billingDashboardService.cashFlow(anyInt())).thenReturn(List.of(
                new CashFlowPoint("2026-08", new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("1000")),
                new CashFlowPoint("2026-09", new BigDecimal("2000"), new BigDecimal("1500"), new BigDecimal("1500"))));

        mockMvc().perform(get("/billing/dashboard/cash-flow").param("months", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].month").value("2026-09"));
    }

    @Test
    void epsPerformance_returnsList() throws Exception {
        when(billingDashboardService.epsPerformance()).thenReturn(List.of(
                new EpsPerformance("890900123", "TEST EPS", new BigDecimal("100000"),
                        new BigDecimal("5000"), new BigDecimal("60000"), 1, 50L)));

        mockMvc().perform(get("/billing/dashboard/eps-performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].epsNit").value("890900123"))
                .andExpect(jsonPath("$[0].glosasCount").value(1));
    }
}
