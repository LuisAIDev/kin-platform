package com.kinplatform.billing.rips;

import com.kinplatform.billing.rips.generator.RipsGenerationOrchestrator;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RipsGenerationService {

    private final RipsGenerationOrchestrator orchestrator;
    private final RipsBatchRepository batchRepository;
    private final RipsZipService zipService;

    @Transactional
    public RipsBatch generate(UUID organizationId, UUID contractId, LocalDate periodStart, LocalDate periodEnd, RipsBatch.RipsType type) {
        return orchestrator.execute(organizationId, contractId, periodStart, periodEnd, type);
    }

    @Transactional
    public List<RipsBatch> generateAll(UUID organizationId, UUID contractId, LocalDate periodStart, LocalDate periodEnd) {
        return Arrays.stream(RipsBatch.RipsType.values())
            .map(t -> orchestrator.execute(organizationId, contractId, periodStart, periodEnd, t))
            .toList();
    }

    @Transactional
    public RipsBatch generateAllTypes(UUID organizationId, UUID contractId, LocalDate periodStart, LocalDate periodEnd) {
        log.info("Generando todos los tipos RIPS para contrato {} período {}-{}", 
            contractId, periodStart, periodEnd);
        return orchestrator.executeAllTypes(organizationId, contractId, periodStart, periodEnd);
    }

    @Transactional(readOnly = true)
    public byte[] downloadAsZip(UUID organizationId, UUID batchId) {
        RipsBatch batch = batchRepository.findByIdAndOrganizationId(batchId, organizationId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        if (batch.getStatus() != RipsBatch.BatchStatus.VALID
            && batch.getStatus() != RipsBatch.BatchStatus.SENT_TO_DIAN
            && batch.getStatus() != RipsBatch.BatchStatus.ACCEPTED) {
            throw new IllegalStateException("Solo se pueden descargar batches VALID, SENT_TO_DIAN o ACCEPTED");
        }

        return zipService.createZip(batch);
    }

    @Transactional
    public RipsBatch retry(UUID organizationId, UUID batchId) {
        RipsBatch batch = batchRepository.findByIdAndOrganizationId(batchId, organizationId)
            .orElseThrow(() -> new BatchNotFoundException(batchId));

        if (batch.getStatus() != RipsBatch.BatchStatus.INVALID) {
            throw new IllegalStateException("Solo se pueden reintentar batches INVALID");
        }

        log.info("Reintentando generación RIPS para batch {}", batchId);
        return orchestrator.execute(
            organizationId, batch.getContractId(),
            batch.getPeriodStart(), batch.getPeriodEnd(), batch.getRipsType());
    }

    public List<RipsBatch> findByContract(UUID contractId) {
        return batchRepository.findByContractIdAndStatus(contractId, RipsBatch.BatchStatus.VALID);
    }

    public List<RipsBatch> findByPeriod(UUID contractId, LocalDate start, LocalDate end) {
        return batchRepository.findByOrganizationIdAndPeriodStartBetween(TenantContext.get(), start, end);
    }
}