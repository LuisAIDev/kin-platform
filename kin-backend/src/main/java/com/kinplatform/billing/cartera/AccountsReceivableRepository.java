package com.kinplatform.billing.cartera;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivable, UUID> {

    Optional<AccountsReceivable> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<AccountsReceivable> findByOrganizationId(UUID organizationId);

    List<AccountsReceivable> findByOrganizationIdAndStatus(UUID organizationId, AccountsReceivable.ArStatus status);

    List<AccountsReceivable> findByOrganizationIdAndAgingBucket(UUID organizationId, String agingBucket);

    Optional<AccountsReceivable> findByFevInvoiceIdAndOrganizationId(UUID fevInvoiceId, UUID organizationId);

    List<AccountsReceivable> findByStatusIn(List<AccountsReceivable.ArStatus> statuses);
}
