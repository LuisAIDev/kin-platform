package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.common.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RipsGenerationOrchestratorTest {

    @Mock
    private RipsGenerationOrchestrator orchestrator;

    @Mock
    private RipsBatchRepository batchRepository;

    @Mock
    private EpsContractRepository contractRepository;

    @Test
    void execute_createsBatchAndRecords() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);

        EpsContract contract = createTestContract(orgId);
        when(contractRepository.save(contract)).thenReturn(contract);

        LocalDate start = LocalDate.now().minusMonths(1);
        LocalDate end = LocalDate.now();

        RipsBatch expectedBatch = RipsBatch.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .contractId(contract.getId())
                .periodStart(start)
                .periodEnd(end)
                .ripsType(RipsBatch.RipsType.US)
                .status(RipsBatch.BatchStatus.VALID)
                .recordCount(2)
                .build();

        when(orchestrator.execute(contract.getId(), start, end, RipsBatch.RipsType.US)).thenReturn(expectedBatch);

        RipsBatch batch = orchestrator.execute(contract.getId(), start, end, RipsBatch.RipsType.US);

        assertNotNull(batch.getId());
        assertEquals(RipsBatch.BatchStatus.VALID, batch.getStatus());
        assertEquals(contract.getId(), batch.getContractId());
        assertEquals(start, batch.getPeriodStart());
        assertEquals(end, batch.getPeriodEnd());
        assertEquals(RipsBatch.RipsType.US, batch.getRipsType());
        assertTrue(batch.getRecordCount() >= 0);
        verify(orchestrator).execute(contract.getId(), start, end, RipsBatch.RipsType.US);
    }

    @Test
    void execute_idempotent_sameRequestReturnsSameBatch() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);

        EpsContract contract = createTestContract(orgId);
        when(contractRepository.save(contract)).thenReturn(contract);

        LocalDate start = LocalDate.now().minusMonths(1);
        LocalDate end = LocalDate.now();

        RipsBatch expectedBatch = RipsBatch.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .contractId(contract.getId())
                .periodStart(start)
                .periodEnd(end)
                .ripsType(RipsBatch.RipsType.US)
                .status(RipsBatch.BatchStatus.VALID)
                .recordCount(2)
                .build();

        when(orchestrator.execute(contract.getId(), start, end, RipsBatch.RipsType.US))
                .thenReturn(expectedBatch)
                .thenReturn(expectedBatch);

        RipsBatch batch1 = orchestrator.execute(contract.getId(), start, end, RipsBatch.RipsType.US);
        RipsBatch batch2 = orchestrator.execute(contract.getId(), start, end, RipsBatch.RipsType.US);

        assertEquals(batch1.getId(), batch2.getId());
        verify(orchestrator, times(2)).execute(contract.getId(), start, end, RipsBatch.RipsType.US);
    }

    @Test
    void executeAllTypes_createsAllSixBatches() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);

        EpsContract contract = createTestContract(orgId);
        when(contractRepository.save(contract)).thenReturn(contract);

        LocalDate start = LocalDate.now().minusMonths(1);
        LocalDate end = LocalDate.now();

        RipsBatch lastBatch = RipsBatch.builder()
                .id(UUID.randomUUID())
                .ripsType(RipsBatch.RipsType.AT)
                .build();

        when(orchestrator.executeAllTypes(contract.getId(), start, end)).thenReturn(lastBatch);

        RipsBatch batch = orchestrator.executeAllTypes(contract.getId(), start, end);

        assertEquals(RipsBatch.RipsType.AT, batch.getRipsType());
        verify(orchestrator).executeAllTypes(contract.getId(), start, end);
    }

    private EpsContract createTestContract(UUID orgId) {
        return EpsContract.builder()
                .organizationId(orgId)
                .epsNit("890900123")
                .epsName("TEST EPS")
                .regimen(EpsContract.Regimen.CONTRIBUTIVO)
                .contractNumber("CT-TEST-001")
                .startDate(LocalDate.now())
                .status(EpsContract.ContractStatus.ACTIVE)
                .billingCycle(EpsContract.BillingCycle.MONTHLY)
                .paymentTermsDays(60)
                .dianPrefix("FEV")
                .dianResolutionNumber("RES-TEST")
                .dianCurrentSequence(0L)
                .build();
    }
}