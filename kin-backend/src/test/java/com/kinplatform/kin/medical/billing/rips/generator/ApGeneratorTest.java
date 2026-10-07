package com.kinplatform.kin.medical.billing.rips.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.medical.billing.authorization.AuthorizationRepository;
import com.kinplatform.kin.medical.billing.contract.EpsContract;
import com.kinplatform.kin.medical.billing.contract.EpsContractRepository;
import com.kinplatform.kin.medical.billing.contract.TariffCups;
import com.kinplatform.kin.medical.billing.rips.model.RipsBatch;
import com.kinplatform.kin.medical.billing.rips.model.RipsRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApGeneratorTest {

    @Mock
    private EpsContractRepository contractRepository;

    @Mock
    private AuthorizationRepository authRepository;

    @Spy
    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @InjectMocks
    private ApGenerator generator;

    @Test
    void generate_returnsRecordsForProcedimientoOrders() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        List<RipsRecord> records = generator.generate(context(orgId, contractId, List.of(
                order("PROCEDIMIENTO", "890301", "AUTH-001"),
                order("MEDICAMENTO", "890501", ""))));

        assertEquals(1, records.size());
        assertEquals("ORDER", records.get(0).getSourceEntityType());
        assertTrue(records.get(0).getRipsLineData().contains("890301"));
    }

    @Test
    void generate_withAuthorizationNumber_returnsRecord() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        List<RipsRecord> records = generator.generate(context(orgId, contractId,
                List.of(order("PROCEDIMIENTO", "890301", "AUTH-999"))));

        assertEquals(1, records.size());
        assertNotNull(records.get(0).getRipsLineData());
    }

    @Test
    void generate_ripsLineData_neverEmpty() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        RipsRecord record = generator.generate(context(orgId, contractId,
                List.of(order("PROCEDIMIENTO", "890301", "AUTH-001")))).get(0);

        assertThat(record.getRipsLineData())
                .isNotNull()
                .isNotEqualTo("{}")
                .contains("fechaProcedimiento")
                .contains("2026");
    }

    @Test
    void generate_ripsLineData_usesIso8601ForDates() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        RipsRecord record = generator.generate(context(orgId, contractId,
                List.of(order("PROCEDIMIENTO", "890301", "AUTH-001")))).get(0);

        assertThat(record.getRipsLineData()).containsPattern("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void supports_onlyApType() {
        assertTrue(generator.supports(RipsBatch.RipsType.AP));
        assertFalse(generator.supports(RipsBatch.RipsType.AU));
        assertFalse(generator.supports(RipsBatch.RipsType.US));
    }

    private RipsGenerationContext context(UUID orgId, UUID contractId, List<Object> orders) {
        return new RipsGenerationContext(
                orgId, contractId,
                LocalDate.now().minusMonths(1), LocalDate.now(),
                List.of(), orders, List.of(), List.of(),
                Map.of("890301", tariff("890301", "120000")), Map.of());
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

    private FakeOrder order(String type, String cups, String authorization) {
        return new FakeOrder(type, cups, "2026-01-15", "Z00", "1", "1", authorization);
    }

    public static class FakeOrder {
        private final String orderType;
        private final String cupsCode;
        private final String orderDate;
        private final String diagnosisCode;
        private final String ambitoRealizacion;
        private final String finalidadProcedimiento;
        private final String authorizationNumber;

        public FakeOrder(String orderType, String cupsCode, String orderDate, String diagnosisCode,
                         String ambitoRealizacion, String finalidadProcedimiento, String authorizationNumber) {
            this.orderType = orderType;
            this.cupsCode = cupsCode;
            this.orderDate = orderDate;
            this.diagnosisCode = diagnosisCode;
            this.ambitoRealizacion = ambitoRealizacion;
            this.finalidadProcedimiento = finalidadProcedimiento;
            this.authorizationNumber = authorizationNumber;
        }

        public UUID getId() { return UUID.randomUUID(); }
        public String getOrderType() { return orderType; }
        public String getCupsCode() { return cupsCode; }
        public String getOrderDate() { return orderDate; }
        public String getDiagnosisCode() { return diagnosisCode; }
        public String getAmbitoRealizacion() { return ambitoRealizacion; }
        public String getFinalidadProcedimiento() { return finalidadProcedimiento; }
        public String getAuthorizationNumber() { return authorizationNumber; }
        public String getPatientDocumentType() { return "CC"; }
        public String getPatientDocumentNumber() { return "123456"; }
    }
}


