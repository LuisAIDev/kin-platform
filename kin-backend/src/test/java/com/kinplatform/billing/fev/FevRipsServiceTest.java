package com.kinplatform.billing.fev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.billing.rips.model.RipsRecord;
import com.kinplatform.billing.rips.model.RipsRecordRepository;
import com.kinplatform.common.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FevRipsServiceTest {

    @Mock
    private FevRipsInvoiceRepository invoiceRepository;

    @Mock
    private RipsBatchRepository batchRepository;

    @Mock
    private EpsContractRepository contractRepository;

    @Mock
    private RipsRecordRepository recordRepository;

    @Mock
    private XmlSigner xmlSigner;

    @Mock
    private DianClient dianClient;

    @Mock
    private ContingencyManager contingencyManager;

    @Spy
    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @InjectMocks
    private FevRipsService service;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void generateInvoiceFromBatch_createsDraftInvoiceWithSummedTotal() {
        UUID orgId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        TenantContext.set(orgId);

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch(orgId, contractId, batchId)));
        when(contractRepository.findByIdAndOrganizationId(contractId, orgId))
                .thenReturn(Optional.of(contract(orgId, contractId, 5L)));
        when(recordRepository.findByBatchIdOrderBySequenceNumber(batchId)).thenReturn(List.of(
                RipsRecord.builder().ripsLineData("{\"valor_total\":1000}").build(),
                RipsRecord.builder().ripsLineData("{\"valorConsulta\":2000}").build()));
        when(invoiceRepository.save(any(FevRipsInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FevRipsInvoice invoice = service.generateInvoiceFromBatch(batchId);

        assertEquals(6L, invoice.getInvoiceSequence());
        assertEquals("FEV0000000006", invoice.getInvoiceNumber());
        assertEquals(new BigDecimal("3000"), invoice.getTotalValueCop());
        assertEquals(FevRipsInvoice.InvoiceStatus.DRAFT, invoice.getStatus());
        assertEquals(batchId, invoice.getBatchId());
        assertEquals(orgId, invoice.getOrganizationId());
    }

    @Test
    void generateInvoiceFromBatch_throwsWhenBatchNotInTenant() {
        UUID orgId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        TenantContext.set(orgId);

        when(batchRepository.findById(batchId))
                .thenReturn(Optional.of(batch(UUID.randomUUID(), UUID.randomUUID(), batchId)));

        assertThrows(EntityNotFoundException.class, () -> service.generateInvoiceFromBatch(batchId));
    }

    @Test
    void sign_marksInvoiceSignedWithCufe() {
        UUID orgId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        TenantContext.set(orgId);

        FevRipsInvoice invoice = FevRipsInvoice.builder()
                .id(id).organizationId(orgId).invoiceNumber("FEV1")
                .issueDate(LocalDate.now()).totalValueCop(BigDecimal.TEN)
                .status(FevRipsInvoice.InvoiceStatus.DRAFT).build();
        when(invoiceRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(invoice));
        when(xmlSigner.sign(anyString())).thenReturn(new XmlSigner.Signature("<signed/>", "CUFE123", "qr"));
        when(invoiceRepository.save(any(FevRipsInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FevRipsInvoice signed = service.sign(id);

        assertEquals(FevRipsInvoice.InvoiceStatus.SIGNED, signed.getStatus());
        assertEquals("CUFE123", signed.getDianCufe());
        assertNotNull(signed.getSignedXmlPath());
    }

    @Test
    void sendToDian_marksAcceptedOnSuccessfulResponse() {
        UUID orgId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        TenantContext.set(orgId);

        FevRipsInvoice invoice = FevRipsInvoice.builder()
                .id(id).organizationId(orgId).status(FevRipsInvoice.InvoiceStatus.SIGNED).build();
        when(invoiceRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(invoice));
        when(dianClient.send(any(FevRipsInvoice.class)))
                .thenReturn(new DianClient.DianResponse(true, "CUFE123", "00", "ok", "<DianResponse/>"));
        when(invoiceRepository.save(any(FevRipsInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FevRipsInvoice sent = service.sendToDian(id);

        assertEquals(FevRipsInvoice.InvoiceStatus.ACCEPTED, sent.getStatus());
        assertEquals("CUFE123", sent.getDianCufe());
        assertNotNull(sent.getDianResponseXml());
    }

    @Test
    void sendToDian_throwsWhenInvoiceNotSigned() {
        UUID orgId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        TenantContext.set(orgId);

        FevRipsInvoice invoice = FevRipsInvoice.builder()
                .id(id).organizationId(orgId).status(FevRipsInvoice.InvoiceStatus.DRAFT).build();
        when(invoiceRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(invoice));

        assertThrows(IllegalStateException.class, () -> service.sendToDian(id));
    }

    @Test
    void getStatus_returnsInvoiceForTenant() {
        UUID orgId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        TenantContext.set(orgId);

        FevRipsInvoice invoice = FevRipsInvoice.builder().id(id).organizationId(orgId).build();
        when(invoiceRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(invoice));

        assertEquals(id, service.getStatus(id).getId());
    }

    @Test
    void getStatus_throwsWhenNotFound() {
        UUID orgId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        TenantContext.set(orgId);

        when(invoiceRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getStatus(id));
    }

    @Test
    void enterContingency_delegatesToManager() {
        UUID orgId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        TenantContext.set(orgId);

        FevRipsInvoice invoice = FevRipsInvoice.builder().id(id).organizationId(orgId).build();
        when(invoiceRepository.findByIdAndOrganizationId(id, orgId)).thenReturn(Optional.of(invoice));
        when(contingencyManager.enterContingency(any(FevRipsInvoice.class), anyString())).thenReturn(invoice);

        assertNotNull(service.enterContingency(id, "DIAN caida"));
    }

    private RipsBatch batch(UUID orgId, UUID contractId, UUID batchId) {
        return RipsBatch.builder()
                .id(batchId)
                .organizationId(orgId)
                .contractId(contractId)
                .ripsType(RipsBatch.RipsType.AC)
                .build();
    }

    private EpsContract contract(UUID orgId, UUID contractId, Long sequence) {
        return EpsContract.builder()
                .id(contractId)
                .organizationId(orgId)
                .epsNit("890900123")
                .epsName("TEST EPS")
                .regimen(EpsContract.Regimen.CONTRIBUTIVO)
                .startDate(LocalDate.now())
                .status(EpsContract.ContractStatus.ACTIVE)
                .billingCycle(EpsContract.BillingCycle.MONTHLY)
                .paymentTermsDays(30)
                .dianPrefix("FEV")
                .dianCurrentSequence(sequence)
                .build();
    }
}
