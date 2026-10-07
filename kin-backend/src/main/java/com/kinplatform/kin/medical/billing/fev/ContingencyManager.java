package com.kinplatform.kin.medical.billing.fev;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Gestion de contingencia segun Resolucion DIAN 000042/2020:
 * ante falla del servicio, la factura se emite en modo contingencia con
 * plazo de 72 horas para transmitirla a la DIAN.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContingencyManager {

    public static final Duration CONTINGENCY_WINDOW = Duration.ofHours(72);

    private final FevRipsInvoiceRepository repository;

    @Transactional
    public FevRipsInvoice enterContingency(FevRipsInvoice invoice, String reason) {
        invoice.markContingency(reason, OffsetDateTime.now().plus(CONTINGENCY_WINDOW));
        log.warn("FEV {} en contingencia: {}", invoice.getId(), reason);
        return repository.save(invoice);
    }

    public boolean isDeadlineExpired(FevRipsInvoice invoice) {
        return invoice.getContingencyDeadline() != null
                && invoice.getContingencyDeadline().isBefore(OffsetDateTime.now());
    }

    @Transactional
    public FevRipsInvoice retry(FevRipsInvoice invoice) {
        invoice.setStatus(FevRipsInvoice.InvoiceStatus.DRAFT);
        invoice.setContingencyReason(null);
        invoice.setContingencyDeadline(null);
        return repository.save(invoice);
    }

    @Transactional
    public int retryExpired() {
        List<FevRipsInvoice> expired = repository.findByStatusAndContingencyDeadlineBefore(
                FevRipsInvoice.InvoiceStatus.CONTINGENCY, OffsetDateTime.now());
        expired.forEach(this::retry);
        return expired.size();
    }
}

