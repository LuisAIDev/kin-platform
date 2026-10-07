package com.kinplatform.kin.medical.billing.rips.generator;

import com.kinplatform.kin.medical.billing.contract.EpsContract;
import com.kinplatform.kin.medical.billing.contract.EpsContractRepository;
import com.kinplatform.kin.medical.billing.contract.TariffCups;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
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
class AtGeneratorTest {

    @Mock
    private EpsContractRepository contractRepository;

    @InjectMocks
    private AtGenerator generator;

    @Test
    void generate_returnsRecordsForOtherServices() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        RipsGenerationContext context = new RipsGenerationContext(
                orgId, contractId,
                LocalDate.now().minusMonths(1), LocalDate.now(),
                List.of(),
                List.of(new FakeOrder("MEDICAMENTO", "M01"),
                        new FakeOrder("INSUMO", "I01"),
                        new FakeOrder("DISPOSITIVO", "D01"),
                        new FakeOrder("PROCEDIMIENTO", "P01")),
                List.of(), List.of(),
                Map.of("M01", tariff("M01", "1000"),
                        "I01", tariff("I01", "2000"),
                        "D01", tariff("D01", "3000")),
                Map.of());

        List<RipsRecord> records = generator.generate(context);

        assertEquals(3, records.size());
        assertEquals("ORDER", records.get(0).getSourceEntityType());
        assertNotNull(records.get(0).getRipsLineData());
    }

    @Test
    void generate_handlesMedicamentoInsumoDispositivo() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        RipsGenerationContext context = new RipsGenerationContext(
                orgId, contractId,
                LocalDate.now().minusMonths(1), LocalDate.now(),
                List.of(), List.of(new FakeOrder("MEDICAMENTO", "M01")), List.of(), List.of(),
                Map.of("M01", tariff("M01", "1000")), Map.of());

        List<RipsRecord> records = generator.generate(context);

        assertEquals(1, records.size());
        assertTrue(records.get(0).getRipsLineData().contains("codigo_servicio"));
        assertTrue(records.get(0).getRipsLineData().contains("MEDICAMENTO"));
    }

    @Test
    void supports_onlyAtType() {
        assertTrue(generator.supports(RipsBatch.RipsType.AT));
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

    public static class FakeOrder {
        private final String orderType;
        private final String cupsCode;

        public FakeOrder(String orderType, String cupsCode) {
            this.orderType = orderType;
            this.cupsCode = cupsCode;
        }

        public UUID getId() { return UUID.randomUUID(); }
        public String getOrderType() { return orderType; }
        public String getCupsCode() { return cupsCode; }
        public String getOrderDate() { return "2026-01-15"; }
        public String getDescription() { return "Servicio " + orderType; }
        public int getQuantity() { return 2; }
        public String getServiceType() { return orderType; }
        public String getPatientDocumentType() { return "CC"; }
        public String getPatientDocumentNumber() { return "123456"; }
    }
}


