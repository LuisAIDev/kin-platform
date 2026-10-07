package com.kinplatform.kin.medical.billing.rips.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AcGeneratorTest {

    @Mock
    private EpsContractRepository contractRepository;

    @Spy
    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @InjectMocks
    private AcGenerator generator;

    @Test
    void generate_returnsRecordsForEncounters() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        List<RipsRecord> records = generator.generate(context(orgId, contractId,
                List.of(encounter("CONSULTA", "890201", "Z00", "J01"))));

        assertEquals(1, records.size());
        assertEquals("ENCOUNTER", records.get(0).getSourceEntityType());
        assertNotNull(records.get(0).getRipsLineData());
    }

    @Test
    void generate_calculatesValueFromTariff() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        List<RipsRecord> records = generator.generate(context(orgId, contractId,
                List.of(encounter("CONSULTA", "890201", "Z00", ""))));

        assertTrue(records.get(0).getRipsLineData().contains("890201"));
        assertTrue(records.get(0).getRipsLineData().contains("50000"));
    }

    @Test
    void generate_ripsLineData_neverEmpty() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        RipsRecord record = generator.generate(context(orgId, contractId,
                List.of(encounter("CONSULTA", "890201", "Z00", "")))).get(0);

        assertThat(record.getRipsLineData())
                .isNotNull()
                .isNotEqualTo("{}")
                .contains("fechaConsulta")
                .contains("2026");
    }

    @Test
    void generate_ripsLineData_usesIso8601ForDates() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(java.util.Optional.of(contract(orgId, contractId)));

        RipsRecord record = generator.generate(context(orgId, contractId,
                List.of(encounter("CONSULTA", "890201", "Z00", "")))).get(0);

        assertThat(record.getRipsLineData()).containsPattern("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    void supports_onlyAcType() {
        assertTrue(generator.supports(RipsBatch.RipsType.AC));
        assertFalse(generator.supports(RipsBatch.RipsType.AP));
        assertFalse(generator.supports(RipsBatch.RipsType.US));
    }

    private RipsGenerationContext context(UUID orgId, UUID contractId, List<Object> encounters) {
        return new RipsGenerationContext(
                orgId, contractId,
                LocalDate.now().minusMonths(1), LocalDate.now(),
                encounters, List.of(), List.of(), List.of(),
                Map.of("890201", tariff("890201", "50000")), Map.of());
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

    private FakeEncounter encounter(String type, String cups, String diagnosis, String related) {
        return new FakeEncounter(type, cups, "2026-01-15", diagnosis, related, "CC", "123456");
    }

    public static class FakeEncounter {
        private final String encounterType;
        private final String cupsCode;
        private final String encounterDate;
        private final String diagnosisCode;
        private final String relatedDiagnosisCode;
        private final String patientDocumentType;
        private final String patientDocumentNumber;

        public FakeEncounter(String encounterType, String cupsCode, String encounterDate, String diagnosisCode,
                             String relatedDiagnosisCode, String patientDocumentType, String patientDocumentNumber) {
            this.encounterType = encounterType;
            this.cupsCode = cupsCode;
            this.encounterDate = encounterDate;
            this.diagnosisCode = diagnosisCode;
            this.relatedDiagnosisCode = relatedDiagnosisCode;
            this.patientDocumentType = patientDocumentType;
            this.patientDocumentNumber = patientDocumentNumber;
        }

        public UUID getId() { return UUID.randomUUID(); }
        public String getEncounterType() { return encounterType; }
        public String getCupsCode() { return cupsCode; }
        public String getEncounterDate() { return encounterDate; }
        public String getDiagnosisCode() { return diagnosisCode; }
        public String getRelatedDiagnosisCode() { return relatedDiagnosisCode; }
        public String getPatientDocumentType() { return patientDocumentType; }
        public String getPatientDocumentNumber() { return patientDocumentNumber; }
    }
}


