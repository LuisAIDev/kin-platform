package com.kinplatform.billing.rips.generator;

import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.billing.contract.TariffCups;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuGeneratorTest {

    @Mock
    private EpsContractRepository contractRepository;

    @InjectMocks
    private AuGenerator generator;

    @Test
    void generate_returnsRecordsForEmergencyEncounters() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        FakeEncounter urgencia = new FakeEncounter("URGENCIA");
        FakeEncounter consulta = new FakeEncounter("CONSULTA");

        RipsGenerationContext context = new RipsGenerationContext(
                orgId, contractId,
                LocalDate.now().minusMonths(1), LocalDate.now(),
                List.of(urgencia, consulta), List.of(), List.of(), List.of(),
                Map.of("890201", tariff("890201", "80000")), Map.of());

        List<RipsRecord> records = generator.generate(context);

        assertEquals(1, records.size());
        assertEquals("ENCOUNTER", records.get(0).getSourceEntityType());
    }

    @Test
    void generate_ripsLineDataNeverNull() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        RipsGenerationContext context = new RipsGenerationContext(
                orgId, contractId,
                LocalDate.now().minusMonths(1), LocalDate.now(),
                List.of(new FakeEncounter("URGENCIA")), List.of(), List.of(), List.of(),
                Map.of(), Map.of());

        List<RipsRecord> records = generator.generate(context);

        assertEquals(1, records.size());
        assertNotNull(records.get(0).getRipsLineData());
        assertTrue(records.get(0).getRipsLineData().contains("fecha_urgencia"));
    }

    @Test
    void supports_onlyAuType() {
        assertTrue(generator.supports(RipsBatch.RipsType.AU));
        assertFalse(generator.supports(RipsBatch.RipsType.AP));
        assertFalse(generator.supports(RipsBatch.RipsType.US));
    }

    private EpsContract contract(UUID orgId, UUID contractId) {
        return EpsContract.builder()
                .id(contractId)
                .organizationId(orgId)
                .epsNit("890900123")
                .epsName("TEST EPS")
                .regimen(EpsContract.Regimen.CONTRIBUTIVO)
                .startDate(LocalDate.now())
                .status(EpsContract.ContractStatus.ACTIVE)
                .billingCycle(EpsContract.BillingCycle.MONTHLY)
                .paymentTermsDays(60)
                .dianPrefix("FEV")
                .dianCurrentSequence(0L)
                .build();
    }

    private TariffCups tariff(String cupsCode, String price) {
        return TariffCups.builder()
                .cupsCode(cupsCode)
                .cupsVersion("2026")
                .unitPriceCop(new BigDecimal(price))
                .requiresAuth(false)
                .effectiveFrom(LocalDate.now().minusDays(1))
                .build();
    }

    public static class FakeEncounter {
        private final String encounterType;

        public FakeEncounter(String encounterType) {
            this.encounterType = encounterType;
        }

        public UUID getId() { return UUID.randomUUID(); }
        public String getEncounterType() { return encounterType; }
        public String getPatientDocumentType() { return "CC"; }
        public String getPatientDocumentNumber() { return "123456"; }
        public String getEncounterDate() { return "2026-01-15"; }
        public String getUrgencyReason() { return "DOLOR_TORACICO"; }
        public String getDischargeDiagnosisCode() { return "R07"; }
        public String getDischargeDestination() { return "1"; }
        public String getDischargeStatus() { return "1"; }
        public String getCupsCode() { return "890201"; }
    }
}
