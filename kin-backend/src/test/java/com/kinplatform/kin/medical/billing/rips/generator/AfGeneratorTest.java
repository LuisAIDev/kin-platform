package com.kinplatform.kin.medical.billing.rips.generator;

import com.kinplatform.kin.medical.billing.contract.EpsContract;
import com.kinplatform.kin.medical.billing.contract.EpsContractRepository;
import com.kinplatform.kin.medical.billing.rips.generator.RipsGenerationContext;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AfGeneratorTest {

    @Mock
    private EpsContractRepository contractRepository;

    @InjectMocks
    private AfGenerator generator;

    @Test
    void generate_returnsAfRecordWithContractData() {
        UUID orgId = UUID.randomUUID();
        EpsContract contract = EpsContract.builder()
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
        when(contractRepository.findById(contract.getId())).thenReturn(java.util.Optional.of(contract));

        RipsGenerationContext context = new RipsGenerationContext(
                orgId,
                contract.getId(),
                LocalDate.now().minusMonths(1),
                LocalDate.now(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of()
        );

        List<RipsRecord> records = generator.generate(context);

        assertEquals(1, records.size());
        assertEquals("CONTRACT", records.get(0).getSourceEntityType());
        assertEquals(contract.getId(), records.get(0).getSourceEntityId());
    }

    @Test
    void supports_onlyAfType() {
        assertTrue(generator.supports(com.kinplatform.kin.medical.billing.rips.model.RipsBatch.RipsType.AF));
        assertFalse(generator.supports(com.kinplatform.kin.medical.billing.rips.model.RipsBatch.RipsType.US));
    }
}


