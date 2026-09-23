package com.kinplatform.billing.cartera;

import com.kinplatform.billing.fev.FevRipsInvoice;
import com.kinplatform.billing.fev.FevRipsInvoiceRepository;
import com.kinplatform.common.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CarteraServiceTest {

    private AccountsReceivableRepository receivableRepository;
    private FevRipsInvoiceRepository invoiceRepository;
    private CarteraService service;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        receivableRepository = mock(AccountsReceivableRepository.class);
        invoiceRepository = mock(FevRipsInvoiceRepository.class);
        service = new CarteraService(
                receivableRepository,
                invoiceRepository,
                new AgingCalculator(receivableRepository),
                new ProvisionEngine(),
                new CollectionWorkflow());
        TenantContext.set(orgId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void registerInvoice_createsReceivableWithAgingAndProvision() {
        UUID contractId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        FevRipsInvoice invoice = FevRipsInvoice.builder()
                .id(invoiceId).organizationId(orgId).contractId(contractId)
                .issueDate(LocalDate.now().minusDays(60))
                .dueDate(LocalDate.now().minusDays(40))
                .totalValueCop(new BigDecimal("100000"))
                .build();
        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, orgId)).thenReturn(Optional.of(invoice));
        when(receivableRepository.findByFevInvoiceIdAndOrganizationId(invoiceId, orgId)).thenReturn(Optional.empty());
        when(receivableRepository.save(any(AccountsReceivable.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountsReceivable ar = service.registerInvoice(invoiceId);

        assertEquals(orgId, ar.getOrganizationId());
        assertEquals(invoiceId, ar.getFevInvoiceId());
        assertEquals(new BigDecimal("100000"), ar.getTotalValueCop());
        assertEquals(AgingCalculator.BUCKET_31_60, ar.getAgingBucket());
        assertEquals(new BigDecimal("0.1000"), ar.getProvisionRate());
        assertEquals(AccountsReceivable.ArStatus.OVERDUE, ar.getStatus());
    }

    @Test
    void registerInvoice_isIdempotentWhenAlreadyExists() {
        UUID invoiceId = UUID.randomUUID();
        FevRipsInvoice invoice = FevRipsInvoice.builder().id(invoiceId).organizationId(orgId)
                .issueDate(LocalDate.now()).totalValueCop(BigDecimal.TEN).build();
        AccountsReceivable existing = AccountsReceivable.builder().id(UUID.randomUUID()).build();
        when(invoiceRepository.findByIdAndOrganizationId(invoiceId, orgId)).thenReturn(Optional.of(invoice));
        when(receivableRepository.findByFevInvoiceIdAndOrganizationId(invoiceId, orgId))
                .thenReturn(Optional.of(existing));

        AccountsReceivable ar = service.registerInvoice(invoiceId);

        assertSame(existing, ar);
        verify(receivableRepository, never()).save(any());
    }

    @Test
    void registerPayment_marksPaidWhenFullPayment() {
        UUID id = UUID.randomUUID();
        AccountsReceivable ar = AccountsReceivable.builder()
                .id(id).organizationId(orgId)
                .dueDate(LocalDate.now().plusDays(5))
                .totalValueCop(new BigDecimal("100000"))
                .paidValueCop(BigDecimal.ZERO)
                .build();
        when(receivableRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(ar));
        when(receivableRepository.save(any(AccountsReceivable.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountsReceivable result = service.registerPayment(id, new BigDecimal("100000"), LocalDate.now());

        assertEquals(AccountsReceivable.ArStatus.PAID, result.getStatus());
        assertEquals(new BigDecimal("100000"), result.getPaidValueCop());
    }

    @Test
    void summary_aggregatesPendingAndProvision() {
        AccountsReceivable overdue = AccountsReceivable.builder()
                .id(UUID.randomUUID()).organizationId(orgId)
                .totalValueCop(new BigDecimal("100000")).paidValueCop(BigDecimal.ZERO)
                .daysOverdue(40).agingBucket(AgingCalculator.BUCKET_31_60)
                .status(AccountsReceivable.ArStatus.OVERDUE).build();
        AccountsReceivable current = AccountsReceivable.builder()
                .id(UUID.randomUUID()).organizationId(orgId)
                .totalValueCop(new BigDecimal("50000")).paidValueCop(BigDecimal.ZERO)
                .daysOverdue(0).agingBucket(AgingCalculator.CURRENT)
                .status(AccountsReceivable.ArStatus.PENDING).build();
        when(receivableRepository.findByOrganizationId(orgId)).thenReturn(List.of(overdue, current));

        CarteraSummaryResponse summary = service.summary();

        assertEquals(new BigDecimal("150000"), summary.totalPendingCop());
        assertEquals(new BigDecimal("10000.00"), summary.totalProvisionCop());
        assertEquals(1, summary.overdueCount());
    }
}
