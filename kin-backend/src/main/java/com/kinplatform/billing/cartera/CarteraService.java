package com.kinplatform.billing.cartera;

import com.kinplatform.billing.fev.FevRipsInvoice;
import com.kinplatform.billing.fev.FevRipsInvoiceRepository;
import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarteraService {

    private static final int DEFAULT_PAYMENT_TERMS_DAYS = 30;

    private final AccountsReceivableRepository receivableRepository;
    private final FevRipsInvoiceRepository invoiceRepository;
    private final AgingCalculator agingCalculator;
    private final ProvisionEngine provisionEngine;
    private final CollectionWorkflow collectionWorkflow;

    @Transactional
    public AccountsReceivable registerInvoice(UUID fevInvoiceId) {
        UUID organizationId = TenantContext.get();
        FevRipsInvoice invoice = invoiceRepository.findByIdAndOrganizationId(fevInvoiceId, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Factura FEV no encontrada: " + fevInvoiceId));

        return receivableRepository.findByFevInvoiceIdAndOrganizationId(fevInvoiceId, organizationId)
                .orElseGet(() -> {
                    LocalDate issueDate = invoice.getIssueDate() != null ? invoice.getIssueDate() : LocalDate.now();
                    LocalDate dueDate = invoice.getDueDate() != null
                            ? invoice.getDueDate() : issueDate.plusDays(DEFAULT_PAYMENT_TERMS_DAYS);
                    AccountsReceivable ar = AccountsReceivable.builder()
                            .organizationId(organizationId)
                            .contractId(invoice.getContractId())
                            .fevInvoiceId(invoice.getId())
                            .invoiceDate(issueDate)
                            .dueDate(dueDate)
                            .totalValueCop(invoice.getTotalValueCop() != null
                                    ? invoice.getTotalValueCop() : BigDecimal.ZERO)
                            .paidValueCop(BigDecimal.ZERO)
                            .status(AccountsReceivable.ArStatus.PENDING)
                            .build();
                    agingCalculator.recalculate(ar, LocalDate.now());
                    ar.setProvisionRate(provisionEngine.rateFor(ar.getAgingBucket()));
                    return receivableRepository.save(ar);
                });
    }

    public AccountsReceivable get(UUID id) {
        return receivableRepository.findByIdAndOrganizationId(id, TenantContext.get())
                .orElseThrow(() -> new EntityNotFoundException("Cuenta por cobrar no encontrada: " + id));
    }

    public List<AccountsReceivable> findAll() {
        return receivableRepository.findByOrganizationId(TenantContext.get());
    }

    public List<AccountsReceivable> findByStatus(AccountsReceivable.ArStatus status) {
        return receivableRepository.findByOrganizationIdAndStatus(TenantContext.get(), status);
    }

    @Transactional
    public AccountsReceivable registerPayment(UUID id, BigDecimal amount, LocalDate paymentDate) {
        AccountsReceivable ar = get(id);
        BigDecimal total = ar.getTotalValueCop() == null ? BigDecimal.ZERO : ar.getTotalValueCop();
        BigDecimal paid = (ar.getPaidValueCop() == null ? BigDecimal.ZERO : ar.getPaidValueCop())
                .add(amount == null ? BigDecimal.ZERO : amount);
        if (paid.compareTo(total) > 0) {
            paid = total;
        }
        ar.setPaidValueCop(paid);
        ar.setLastPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
        agingCalculator.recalculate(ar, LocalDate.now());
        ar.setProvisionRate(provisionEngine.rateFor(ar.getAgingBucket()));
        return receivableRepository.save(ar);
    }

    public CollectionWorkflow.CollectionAction nextCollectionAction(UUID id) {
        return collectionWorkflow.nextAction(get(id));
    }

    public CarteraSummaryResponse summary() {
        List<AccountsReceivable> receivables = receivableRepository.findByOrganizationId(TenantContext.get());
        BigDecimal totalPending = BigDecimal.ZERO;
        BigDecimal totalProvision = BigDecimal.ZERO;
        long overdueCount = 0;
        Map<String, BigDecimal> pendingByBucket = new LinkedHashMap<>();

        for (AccountsReceivable ar : receivables) {
            BigDecimal pending = agingCalculator.pending(ar);
            totalPending = totalPending.add(pending);
            totalProvision = totalProvision.add(provisionEngine.provisionValue(pending, ar.getAgingBucket()));
            if (ar.getStatus() == AccountsReceivable.ArStatus.OVERDUE) {
                overdueCount++;
            }
            pendingByBucket.merge(ar.getAgingBucket(), pending, BigDecimal::add);
        }
        return new CarteraSummaryResponse(totalPending, totalProvision, overdueCount, pendingByBucket);
    }
}
