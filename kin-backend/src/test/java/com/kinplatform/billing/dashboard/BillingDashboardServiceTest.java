package com.kinplatform.billing.dashboard;

import com.kinplatform.billing.cartera.AccountsReceivable;
import com.kinplatform.billing.cartera.AccountsReceivableRepository;
import com.kinplatform.billing.cartera.AgingCalculator;
import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.billing.fev.FevRipsInvoice;
import com.kinplatform.billing.fev.FevRipsInvoiceRepository;
import com.kinplatform.billing.glosa.Glosa;
import com.kinplatform.billing.glosa.GlosaRepository;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.common.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillingDashboardServiceTest {

    private FevRipsInvoiceRepository invoiceRepository;
    private GlosaRepository glosaRepository;
    private AccountsReceivableRepository receivableRepository;
    private RipsBatchRepository batchRepository;
    private EpsContractRepository contractRepository;
    private BillingDashboardService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID contractId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(FevRipsInvoiceRepository.class);
        glosaRepository = mock(GlosaRepository.class);
        receivableRepository = mock(AccountsReceivableRepository.class);
        batchRepository = mock(RipsBatchRepository.class);
        contractRepository = mock(EpsContractRepository.class);
        service = new BillingDashboardService(invoiceRepository, glosaRepository, receivableRepository,
                batchRepository, contractRepository, new AgingCalculator(null));
        TenantContext.set(orgId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void kpis_aggregatesInvoicesGlosasCarteraAndRips() {
        when(invoiceRepository.findByOrganizationId(orgId)).thenReturn(List.of(
                invoice(contractId, new BigDecimal("100000"), FevRipsInvoice.InvoiceStatus.ACCEPTED),
                invoice(contractId, new BigDecimal("50000"), FevRipsInvoice.InvoiceStatus.REJECTED)));
        when(glosaRepository.findByOrganizationId(orgId)).thenReturn(List.of(
                Glosa.builder().contractId(contractId).glosaValueCop(new BigDecimal("20000")).build()));
        when(receivableRepository.findByOrganizationId(orgId)).thenReturn(List.of(
                receivable(contractId, new BigDecimal("100000"), new BigDecimal("30000"), 40)));
        when(batchRepository.findByOrganizationId(orgId)).thenReturn(List.of(
                RipsBatch.builder().contractId(contractId).status(RipsBatch.BatchStatus.VALID).build()));

        BillingKpisResponse kpis = service.kpis(null, null);

        assertEquals(new BigDecimal("150000"), kpis.totalFacturadoCop());
        assertEquals(new BigDecimal("20000"), kpis.totalGlosasCop());
        assertEquals(new BigDecimal("0.1333"), kpis.glosaRate());
        assertEquals(new BigDecimal("70000"), kpis.totalCarteraCop());
        assertEquals(new BigDecimal("70000"), kpis.carteraVencidaCop());
        assertEquals(1, kpis.ripsPendientes());
        assertEquals(1, kpis.fevAceptadas());
        assertEquals(1, kpis.fevRechazadas());
        assertEquals(new BigDecimal("30000"), kpis.recaudadoCop());
    }

    @Test
    void kpis_filtersByContract() {
        UUID other = UUID.randomUUID();
        when(invoiceRepository.findByOrganizationId(orgId)).thenReturn(List.of(
                invoice(contractId, new BigDecimal("100000"), FevRipsInvoice.InvoiceStatus.ACCEPTED),
                invoice(other, new BigDecimal("999999"), FevRipsInvoice.InvoiceStatus.ACCEPTED)));
        when(glosaRepository.findByOrganizationId(orgId)).thenReturn(List.of());
        when(receivableRepository.findByOrganizationId(orgId)).thenReturn(List.of());
        when(batchRepository.findByOrganizationId(orgId)).thenReturn(List.of());

        BillingKpisResponse kpis = service.kpis(contractId, null);

        assertEquals(new BigDecimal("100000"), kpis.totalFacturadoCop());
    }

    @Test
    void cashFlow_returnsRequestedNumberOfMonths() {
        when(invoiceRepository.findByOrganizationId(orgId)).thenReturn(List.of());
        when(receivableRepository.findByOrganizationId(orgId)).thenReturn(List.of());

        List<CashFlowPoint> points = service.cashFlow(6);

        assertEquals(6, points.size());
        assertEquals(YearMonth.now().toString(), points.get(5).month());
    }

    @Test
    void epsPerformance_groupsByContract() {
        when(invoiceRepository.findByOrganizationId(orgId)).thenReturn(List.of(
                invoice(contractId, new BigDecimal("100000"), FevRipsInvoice.InvoiceStatus.ACCEPTED)));
        when(glosaRepository.findByOrganizationId(orgId)).thenReturn(List.of(
                Glosa.builder().contractId(contractId).glosaValueCop(new BigDecimal("5000")).build()));
        when(receivableRepository.findByOrganizationId(orgId)).thenReturn(List.of(
                receivable(contractId, new BigDecimal("100000"), new BigDecimal("40000"), 10)));
        when(contractRepository.findByIdAndOrganizationId(contractId, orgId)).thenReturn(Optional.of(
                EpsContract.builder().id(contractId).epsNit("890900123").epsName("TEST EPS").build()));

        List<EpsPerformance> performance = service.epsPerformance();

        assertEquals(1, performance.size());
        EpsPerformance eps = performance.get(0);
        assertEquals("890900123", eps.epsNit());
        assertEquals(new BigDecimal("100000"), eps.facturadoCop());
        assertEquals(new BigDecimal("5000"), eps.glosasCop());
        assertEquals(1, eps.glosasCount());
        assertEquals(new BigDecimal("60000"), eps.carteraCop());
    }

    @Test
    void epsPerformance_computesAveragePaymentDays() {
        when(invoiceRepository.findByOrganizationId(orgId)).thenReturn(List.of());
        when(glosaRepository.findByOrganizationId(orgId)).thenReturn(List.of());
        AccountsReceivable paid = AccountsReceivable.builder()
                .contractId(contractId)
                .invoiceDate(LocalDate.now().minusDays(50))
                .lastPaymentDate(LocalDate.now())
                .totalValueCop(new BigDecimal("1000")).paidValueCop(new BigDecimal("1000"))
                .build();
        when(receivableRepository.findByOrganizationId(orgId)).thenReturn(List.of(paid));

        List<EpsPerformance> performance = service.epsPerformance();

        assertEquals(50L, performance.get(0).avgPaymentDays());
    }

    private FevRipsInvoice invoice(UUID contract, BigDecimal total, FevRipsInvoice.InvoiceStatus status) {
        return FevRipsInvoice.builder()
                .id(UUID.randomUUID()).organizationId(orgId).contractId(contract)
                .issueDate(LocalDate.now()).totalValueCop(total).status(status).build();
    }

    private AccountsReceivable receivable(UUID contract, BigDecimal total, BigDecimal paid, int daysOverdue) {
        return AccountsReceivable.builder()
                .id(UUID.randomUUID()).organizationId(orgId).contractId(contract)
                .dueDate(LocalDate.now().minusDays(daysOverdue))
                .totalValueCop(total).paidValueCop(paid).daysOverdue(daysOverdue)
                .agingBucket("31-60").status(AccountsReceivable.ArStatus.OVERDUE).build();
    }
}
