package com.kinplatform.kin.medical.billing.dashboard;

import com.kinplatform.kin.medical.billing.cartera.AccountsReceivable;
import com.kinplatform.kin.medical.billing.cartera.AccountsReceivableRepository;
import com.kinplatform.kin.medical.billing.cartera.AgingCalculator;
import com.kinplatform.kin.medical.billing.contract.EpsContract;
import com.kinplatform.kin.medical.billing.contract.EpsContractRepository;
import com.kinplatform.kin.medical.billing.fev.FevRipsInvoice;
import com.kinplatform.kin.medical.billing.fev.FevRipsInvoiceRepository;
import com.kinplatform.kin.medical.billing.glosa.Glosa;
import com.kinplatform.kin.medical.billing.glosa.GlosaRepository;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatchRepository;
import com.kinplatform.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingDashboardService {

    private static final Set<RipsBatch.BatchStatus> RIPS_PENDING = EnumSet.of(
            RipsBatch.BatchStatus.GENERATING, RipsBatch.BatchStatus.VALIDATING, RipsBatch.BatchStatus.VALID);

    private final FevRipsInvoiceRepository invoiceRepository;
    private final GlosaRepository glosaRepository;
    private final AccountsReceivableRepository receivableRepository;
    private final RipsBatchRepository batchRepository;
    private final EpsContractRepository contractRepository;
    private final AgingCalculator agingCalculator;

    public BillingKpisResponse kpis(UUID contractId, String periodo) {
        UUID organizationId = TenantContext.get();
        YearMonth month = parsePeriod(periodo);

        List<FevRipsInvoice> invoices = invoiceRepository.findByOrganizationId(organizationId).stream()
                .filter(inv -> contractId == null || contractId.equals(inv.getContractId()))
                .filter(inv -> inMonth(inv.getIssueDate(), month))
                .toList();
        List<Glosa> glosas = glosaRepository.findByOrganizationId(organizationId).stream()
                .filter(g -> contractId == null || contractId.equals(g.getContractId()))
                .toList();
        List<AccountsReceivable> receivables = receivableRepository.findByOrganizationId(organizationId).stream()
                .filter(ar -> contractId == null || contractId.equals(ar.getContractId()))
                .toList();
        List<RipsBatch> batches = batchRepository.findByOrganizationId(organizationId).stream()
                .filter(b -> contractId == null || contractId.equals(b.getContractId()))
                .toList();

        BigDecimal facturado = sum(invoices, FevRipsInvoice::getTotalValueCop);
        BigDecimal glosasValue = sum(glosas, Glosa::getGlosaValueCop);
        BigDecimal cartera = receivables.stream().map(agingCalculator::pending).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal carteraVencida = receivables.stream()
                .filter(ar -> ar.getDaysOverdue() != null && ar.getDaysOverdue() > 0)
                .map(agingCalculator::pending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recaudado = sum(receivables, AccountsReceivable::getPaidValueCop);

        long ripsPendientes = batches.stream().filter(b -> RIPS_PENDING.contains(b.getStatus())).count();
        long fevAceptadas = invoices.stream()
                .filter(inv -> inv.getStatus() == FevRipsInvoice.InvoiceStatus.ACCEPTED).count();
        long fevRechazadas = invoices.stream()
                .filter(inv -> inv.getStatus() == FevRipsInvoice.InvoiceStatus.REJECTED).count();

        return new BillingKpisResponse(facturado, glosasValue, rate(glosasValue, facturado),
                cartera, carteraVencida, ripsPendientes, fevAceptadas, fevRechazadas, recaudado);
    }

    public List<CashFlowPoint> cashFlow(int months) {
        UUID organizationId = TenantContext.get();
        int window = Math.max(1, Math.min(months, 24));
        List<FevRipsInvoice> invoices = invoiceRepository.findByOrganizationId(organizationId);
        List<AccountsReceivable> receivables = receivableRepository.findByOrganizationId(organizationId);

        YearMonth current = YearMonth.now();
        List<CashFlowPoint> points = new ArrayList<>();
        for (int i = window - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal facturado = invoices.stream()
                    .filter(inv -> inMonth(inv.getIssueDate(), month))
                    .map(FevRipsInvoice::getTotalValueCop)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal recaudado = receivables.stream()
                    .filter(ar -> inMonth(ar.getLastPaymentDate(), month))
                    .map(AccountsReceivable::getPaidValueCop)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            points.add(new CashFlowPoint(month.toString(), facturado, recaudado, movingAverage(points, facturado)));
        }
        return points;
    }

    public List<EpsPerformance> epsPerformance() {
        UUID organizationId = TenantContext.get();
        List<FevRipsInvoice> invoices = invoiceRepository.findByOrganizationId(organizationId);
        List<Glosa> glosas = glosaRepository.findByOrganizationId(organizationId);
        List<AccountsReceivable> receivables = receivableRepository.findByOrganizationId(organizationId);

        Map<UUID, EpsContract> contracts = new HashMap<>();
        Set<UUID> contractIds = new java.util.HashSet<>();
        invoices.forEach(i -> contractIds.add(i.getContractId()));
        glosas.forEach(g -> contractIds.add(g.getContractId()));
        receivables.forEach(a -> contractIds.add(a.getContractId()));
        for (UUID id : contractIds) {
            if (id != null) {
                contractRepository.findByIdAndOrganizationId(id, organizationId)
                        .ifPresent(c -> contracts.put(id, c));
            }
        }

        List<EpsPerformance> result = new ArrayList<>();
        for (UUID contractId : contractIds) {
            EpsContract contract = contracts.get(contractId);
            BigDecimal facturado = sum(invoices.stream()
                    .filter(i -> contractId.equals(i.getContractId())).toList(), FevRipsInvoice::getTotalValueCop);
            BigDecimal glosasValue = sum(glosas.stream()
                    .filter(g -> contractId.equals(g.getContractId())).toList(), Glosa::getGlosaValueCop);
            long glosasCount = glosas.stream().filter(g -> contractId.equals(g.getContractId())).count();
            BigDecimal cartera = receivables.stream()
                    .filter(ar -> contractId.equals(ar.getContractId()))
                    .map(agingCalculator::pending)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Long avgDays = averagePaymentDays(receivables.stream()
                    .filter(ar -> contractId.equals(ar.getContractId())).toList());
            result.add(new EpsPerformance(
                    contract != null ? contract.getEpsNit() : null,
                    contract != null ? contract.getEpsName() : null,
                    facturado, glosasValue, cartera, glosasCount, avgDays));
        }
        return result;
    }

    private YearMonth parsePeriod(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(periodo);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private boolean inMonth(LocalDate date, YearMonth month) {
        return date != null && YearMonth.from(date).equals(month);
    }

    private <T> BigDecimal sum(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
        return items.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rate(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal movingAverage(List<CashFlowPoint> previous, BigDecimal fallback) {
        List<BigDecimal> last = previous.stream()
                .skip(Math.max(0, previous.size() - 3))
                .map(CashFlowPoint::facturadoCop)
                .toList();
        if (last.isEmpty()) {
            return fallback;
        }
        BigDecimal total = last.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(last.size()), 2, RoundingMode.HALF_UP);
    }

    private Long averagePaymentDays(List<AccountsReceivable> receivables) {
        List<Long> days = receivables.stream()
                .filter(ar -> ar.getLastPaymentDate() != null && ar.getInvoiceDate() != null)
                .map(ar -> ChronoUnit.DAYS.between(ar.getInvoiceDate(), ar.getLastPaymentDate()))
                .toList();
        if (days.isEmpty()) {
            return null;
        }
        return Math.round(days.stream().mapToLong(Long::longValue).average().orElse(0));
    }
}


