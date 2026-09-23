package com.kinplatform.billing.rips;

import com.kinplatform.billing.rips.generator.RipsGenerationOrchestrator;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.common.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RipsGenerationService {

    private final RipsGenerationOrchestrator orchestrator;
    private final RipsBatchRepository batchRepository;

    @Transactional
    public RipsBatch generate(UUID contractId, LocalDate periodStart, LocalDate periodEnd, RipsBatch.RipsType type) {
        return orchestrator.execute(contractId, periodStart, periodEnd, type);
    }

    @Transactional
    public List<RipsBatch> generateAll(UUID contractId, LocalDate periodStart, LocalDate periodEnd) {
        return Arrays.stream(RipsBatch.RipsType.values())
                .map(t -> orchestrator.execute(contractId, periodStart, periodEnd, t))
                .toList();
    }

    public List<RipsBatch> findByContract(UUID contractId) {
        return batchRepository.findByContractIdAndStatus(contractId, RipsBatch.BatchStatus.VALID);
    }

    public List<RipsBatch> findByPeriod(UUID contractId, LocalDate start, LocalDate end) {
        return batchRepository.findByOrganizationIdAndPeriodStartBetween(TenantContext.get(), start, end);
    }
}