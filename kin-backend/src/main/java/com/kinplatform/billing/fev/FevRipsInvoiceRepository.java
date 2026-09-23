package com.kinplatform.billing.fev;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FevRipsInvoiceRepository extends JpaRepository<FevRipsInvoice, UUID> {

    Optional<FevRipsInvoice> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<FevRipsInvoice> findByOrganizationId(UUID organizationId);

    Optional<FevRipsInvoice> findByOrganizationIdAndInvoicePrefixAndInvoiceSequence(
            UUID organizationId, String invoicePrefix, Long invoiceSequence);

    Optional<FevRipsInvoice> findByBatchIdAndOrganizationId(UUID batchId, UUID organizationId);

    List<FevRipsInvoice> findByOrganizationIdAndStatus(UUID organizationId, FevRipsInvoice.InvoiceStatus status);

    List<FevRipsInvoice> findByStatusAndContingencyDeadlineBefore(
            FevRipsInvoice.InvoiceStatus status, OffsetDateTime deadline);
}
