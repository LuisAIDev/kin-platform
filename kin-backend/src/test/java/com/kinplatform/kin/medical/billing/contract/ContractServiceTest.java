package com.kinplatform.kin.medical.billing.contract;

import com.kinplatform.common.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ContractServiceTest {

    @Mock
    private EpsContractRepository contractRepository;

    @Mock
    private TariffCupsRepository tariffRepository;

    @Mock
    private CopayRuleRepository copayRepository;

    @Mock
    private TariffImportService importService;

    private ContractValidator validator;

    @InjectMocks
    private ContractService contractService;

    private UUID orgId1;
    private UUID orgId2;

    @BeforeEach
    void setUp() {
        orgId1 = UUID.randomUUID();
        orgId2 = UUID.randomUUID();
        TenantContext.set(orgId1);
        validator = new ContractValidator(contractRepository);
        contractService = new ContractService(contractRepository, tariffRepository, copayRepository, validator, importService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createContract_seteaOrganizationIdCorrecto() {
        ContractService.CreateContractRequest request = new ContractService.CreateContractRequest(
                "890900123", "COLSANITAS", "CONTRIBUTIVO", "CT-001",
                java.time.LocalDate.now(), null, "MONTHLY", 60,
                "factura@test.com", "3001234567", "FEV", "RES-001"
        );

        EpsContract saved = EpsContract.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId1)
                .epsNit("890900123")
                .epsName("COLSANITAS")
                .regimen(EpsContract.Regimen.CONTRIBUTIVO)
                .contractNumber("CT-001")
                .startDate(java.time.LocalDate.now())
                .status(EpsContract.ContractStatus.ACTIVE)
                .billingCycle(EpsContract.BillingCycle.MONTHLY)
                .paymentTermsDays(60)
                .contactEmail("factura@test.com")
                .contactPhone("3001234567")
                .dianPrefix("FEV")
                .dianResolutionNumber("RES-001")
                .dianCurrentSequence(0L)
                .build();

        when(contractRepository.save(any(EpsContract.class))).thenReturn(saved);

        EpsContract result = contractService.create(request);

        assertNotNull(result.getId());
        assertEquals(orgId1, result.getOrganizationId());
        assertEquals("890900123", result.getEpsNit());
        assertEquals(EpsContract.ContractStatus.ACTIVE, result.getStatus());
        verify(contractRepository).save(argThat(c -> c.getOrganizationId().equals(orgId1)));
    }

    @Test
    void findAll_soloDevuelveContratosDeLaOrganizacionActual() {
        Page<EpsContract> page = new PageImpl<>(List.of(
                EpsContract.builder().id(UUID.randomUUID()).organizationId(orgId1).epsNit("890900123").epsName("COLSANITAS").build(),
                EpsContract.builder().id(UUID.randomUUID()).organizationId(orgId1).epsNit("890300456").epsName("SURA").build()
        ));

        when(contractRepository.findByOrganizationId(eq(orgId1), any(PageRequest.class))).thenReturn(page);

        Page<EpsContract> result = contractService.findAll(PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(c -> c.getOrganizationId().equals(orgId1)));
        verify(contractRepository).findByOrganizationId(eq(orgId1), any(PageRequest.class));
    }

    @Test
    void findById_contratoDeOtraOrganizacion_lanza404() {
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findByIdAndOrganizationId(contractId, orgId1)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> 
            contractService.findById(contractId)
        );
    }

    @Test
    void createContract_mismoNitMismaOrg_lanzaDuplicateException() {
        when(contractRepository.existsByOrganizationIdAndEpsNit(eq(orgId1), eq("890900123"))).thenReturn(true);

        ContractService.CreateContractRequest request = new ContractService.CreateContractRequest(
                "890900123", "COLSANITAS", "CONTRIBUTIVO", "CT-001",
                java.time.LocalDate.now(), null, "MONTHLY", 60,
                "test@test.com", "3001234567", "FEV", "RES-001"
        );

        assertThrows(IllegalArgumentException.class, () -> contractService.create(request));
    }

    @Test
    void createContract_mismoNitDistintaOrg_permitido() {
        when(contractRepository.existsByOrganizationIdAndEpsNit(eq(orgId2), eq("890900123"))).thenReturn(false);

        EpsContract saved = EpsContract.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId2)
                .epsNit("890900123")
                .epsName("COLSANITAS ORG2")
                .build();

        when(contractRepository.save(any())).thenReturn(saved);

        TenantContext.set(orgId2);
        ContractService.CreateContractRequest request = new ContractService.CreateContractRequest(
                "890900123", "COLSANITAS ORG2", "CONTRIBUTIVO", "CT-002",
                java.time.LocalDate.now(), null, "MONTHLY", 60,
                "test@test.com", "3001234567", "FEV", "RES-002"
        );

        EpsContract result = contractService.create(request);

        assertNotNull(result.getId());
        assertEquals(orgId2, result.getOrganizationId());
        verify(contractRepository).save(any());
    }

    @Test
    void importTariffs_validaTenantYImporta() throws Exception {
        UUID contractId = UUID.randomUUID();
        EpsContract contract = EpsContract.builder().id(contractId).organizationId(orgId1).build();

        when(contractRepository.findByIdAndOrganizationId(contractId, orgId1)).thenReturn(Optional.of(contract));
        when(importService.importTariffs(eq(contractId), any())).thenReturn(new TariffImportResult(1, 0, 0, List.of()));

        byte[] excel = createTestExcel();
        MultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "test.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel);

        TariffImportResult result = contractService.importTariffs(contractId, file);

        assertEquals(1, result.getCreated());
        assertEquals(0, result.getErrors());
        verify(importService).importTariffs(eq(contractId), any());
    }

    @Test
    void importTariffs_contratoDeOtraOrganizacion_lanza404() throws Exception {
        UUID contractId = UUID.randomUUID();
        when(contractRepository.findByIdAndOrganizationId(contractId, orgId2)).thenReturn(Optional.empty());

        byte[] excel = createTestExcel();
        MultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "test.xlsx", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel);

        TenantContext.set(orgId2);

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> 
            contractService.importTariffs(contractId, file)
        );
    }

    private byte[] createTestExcel() throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = workbook.createSheet("Tarifarios");
            var header = sheet.createRow(0);
            String[] headers = {"CUPS_CODE", "DESCRIPTION", "UNIT_PRICE_COPS", "CUPS_CATEGORY", "REQUIRES_AUTH", "AUTH_VALIDITY_DAYS", "EFFECTIVE_FROM", "EFFECTIVE_TO"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("890201");
            row.createCell(1).setCellValue("CONSULTA GENERAL");
            row.createCell(2).setCellValue(45000);
            row.createCell(3).setCellValue("CONSULTA");
            row.createCell(4).setCellValue(false);
            row.createCell(5).setCellValue("");
            row.createCell(6).setCellValue(java.time.LocalDate.now().toString());
            row.createCell(7).setCellValue("");
            var bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
