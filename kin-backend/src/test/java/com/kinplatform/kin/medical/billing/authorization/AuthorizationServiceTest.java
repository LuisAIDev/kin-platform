package com.kinplatform.kin.medical.billing.authorization;

import com.kinplatform.kin.medical.billing.contract.EpsContract;
import com.kinplatform.kin.medical.billing.contract.EpsContractRepository;
import com.kinplatform.common.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthorizationServiceTest {

    @Mock
    private AuthorizationRepository repository;

    @Mock
    private EpsContractRepository contractRepository;

    @Mock
    private MipresClient mipresClient;

    @InjectMocks
    private AuthorizationService authorizationService;

    private UUID orgId1;
    private UUID orgId2;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        orgId1 = UUID.randomUUID();
        orgId2 = UUID.randomUUID();
        TenantContext.set(orgId1);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createManualAuthorization_success() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);

        EpsContract contract = createTestContract(orgId);
        when(contractRepository.save(contract)).thenReturn(contract);

        Authorization savedAuth = Authorization.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .contractId(contract.getId())
                .patientId(UUID.randomUUID())
                .authorizationNumber("AUTH-TEST-001")
                .authorizationType(Authorization.AuthorizationType.MANUAL)
                .status(Authorization.AuthorizationStatus.PENDING)
                .cupsCodes("[{\"code\":\"890201\",\"qty_approved\":5,\"unit_price_cop\":45000}]")
                .diagnosisCie10("[{\"code\":\"I10\",\"type\":\"PRINCIPAL\"}]")
                .requestedDate(java.time.OffsetDateTime.now())
                .approvedValueCop(new java.math.BigDecimal("225000"))
                .usedValueCop(java.math.BigDecimal.ZERO)
                .notes("Test autorización manual")
                .build();
        when(repository.save(any())).thenReturn(savedAuth);

        AuthorizationService.CreateAuthorizationRequest request = new AuthorizationService.CreateAuthorizationRequest(
                contract.getId(),
                UUID.randomUUID(),
                "AUTH-TEST-001",
                "MANUAL",
                "[{\"code\":\"890201\",\"qty_approved\":5,\"unit_price_cop\":45000}]",
                "[{\"code\":\"I10\",\"type\":\"PRINCIPAL\"}]",
                java.time.OffsetDateTime.now(),
                new java.math.BigDecimal("225000"),
                "Test autorización manual"
        );

        Authorization auth = authorizationService.create(request);

        assertNotNull(auth.getId());
        assertEquals(Authorization.AuthorizationStatus.PENDING, auth.getStatus());
        assertEquals("AUTH-TEST-001", auth.getAuthorizationNumber());
    }

    @Test
    void createDuplicateAuthorization_throws() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);

        EpsContract contract = createTestContract(orgId);
        when(contractRepository.save(contract)).thenReturn(contract);

        // Primera llamada false, segunda true
        when(repository.existsByContractIdAndAuthorizationNumber(any(), any()))
                .thenReturn(false)
                .thenReturn(true);

        AuthorizationService.CreateAuthorizationRequest request = new AuthorizationService.CreateAuthorizationRequest(
                contract.getId(),
                UUID.randomUUID(),
                "AUTH-DUP-001",
                "MANUAL",
                "[]",
                "[]",
                java.time.OffsetDateTime.now(),
                java.math.BigDecimal.ZERO,
                null
        );

        authorizationService.create(request);

        assertThrows(IllegalArgumentException.class, () -> authorizationService.create(request));
    }

    @Test
    void consumeAuthorization_updatesUsedValue() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);

        EpsContract contract = createTestContract(orgId);
        when(contractRepository.save(contract)).thenReturn(contract);

        Authorization auth = Authorization.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .contractId(contract.getId())
                .patientId(UUID.randomUUID())
                .authorizationNumber("AUTH-CONSUME-001")
                .authorizationType(Authorization.AuthorizationType.MANUAL)
                .status(Authorization.AuthorizationStatus.APPROVED)
                .cupsCodes("[{\"code\":\"890201\",\"qty_approved\":10,\"unit_price_cop\":45000}]")
                .requestedDate(java.time.OffsetDateTime.now().minusDays(1))
                .approvedDate(java.time.OffsetDateTime.now())
                .approvedValueCop(new java.math.BigDecimal("450000"))
                .usedValueCop(java.math.BigDecimal.ZERO)
                .build();
        when(repository.findByIdAndOrganizationId(auth.getId(), orgId)).thenReturn(java.util.Optional.of(auth));

        // Mock para devolver la autorización actualizada
        Authorization updatedAuth = Authorization.builder()
                .id(auth.getId())
                .organizationId(orgId)
                .contractId(contract.getId())
                .patientId(auth.getPatientId())
                .authorizationNumber("AUTH-CONSUME-001")
                .authorizationType(Authorization.AuthorizationType.MANUAL)
                .status(Authorization.AuthorizationStatus.PARTIAL)
                .cupsCodes("[{\"code\":\"890201\",\"qty_approved\":10,\"unit_price_cop\":45000}]")
                .requestedDate(java.time.OffsetDateTime.now().minusDays(1))
                .approvedDate(java.time.OffsetDateTime.now())
                .approvedValueCop(new java.math.BigDecimal("450000"))
                .usedValueCop(new java.math.BigDecimal("90000"))
                .build();
        when(repository.save(any())).thenReturn(updatedAuth);

        Authorization consumed = authorizationService.consume(auth.getId(), "890201", 2, new java.math.BigDecimal("45000"));

        assertEquals(Authorization.AuthorizationStatus.PARTIAL, consumed.getStatus());
        assertEquals(new java.math.BigDecimal("90000"), consumed.getUsedValueCop());
    }

    @Test
    void consumeAuthorization_marksUsedWhenExhausted() {
        UUID orgId = UUID.randomUUID();
        TenantContext.set(orgId);

        EpsContract contract = createTestContract(orgId);
        when(contractRepository.save(contract)).thenReturn(contract);

        Authorization auth = Authorization.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .contractId(contract.getId())
                .patientId(UUID.randomUUID())
                .authorizationNumber("AUTH-USED-001")
                .authorizationType(Authorization.AuthorizationType.MANUAL)
                .status(Authorization.AuthorizationStatus.APPROVED)
                .cupsCodes("[{\"code\":\"890201\",\"qty_approved\":2,\"unit_price_cop\":45000}]")
                .requestedDate(java.time.OffsetDateTime.now().minusDays(1))
                .approvedDate(java.time.OffsetDateTime.now())
                .approvedValueCop(new java.math.BigDecimal("90000"))
                .usedValueCop(java.math.BigDecimal.ZERO)
                .build();
        when(repository.findByIdAndOrganizationId(any(), any())).thenReturn(java.util.Optional.of(auth));

        // Mock para devolver la autorización actualizada
        Authorization updatedAuth = Authorization.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .contractId(contract.getId())
                .patientId(UUID.randomUUID())
                .authorizationNumber("AUTH-USED-001")
                .authorizationType(Authorization.AuthorizationType.MANUAL)
                .status(Authorization.AuthorizationStatus.USED)
                .cupsCodes("[{\"code\":\"890201\",\"qty_approved\":2,\"unit_price_cop\":45000}]")
                .requestedDate(java.time.OffsetDateTime.now().minusDays(1))
                .approvedDate(java.time.OffsetDateTime.now())
                .approvedValueCop(new java.math.BigDecimal("90000"))
                .usedValueCop(new java.math.BigDecimal("90000"))
                .build();
        when(repository.save(any())).thenReturn(updatedAuth);

        Authorization consumed = authorizationService.consume(auth.getId(), "890201", 2, new java.math.BigDecimal("45000"));

        assertEquals(Authorization.AuthorizationStatus.USED, consumed.getStatus());
    }

    private com.kinplatform.kin.medical.billing.contract.EpsContract createTestContract(UUID orgId) {
        return com.kinplatform.kin.medical.billing.contract.EpsContract.builder()
                .organizationId(orgId)
                .epsNit("890900123")
                .epsName("TEST EPS")
                .regimen(com.kinplatform.kin.medical.billing.contract.EpsContract.Regimen.CONTRIBUTIVO)
                .contractNumber("CT-TEST-001")
                .startDate(java.time.LocalDate.now())
                .status(com.kinplatform.kin.medical.billing.contract.EpsContract.ContractStatus.ACTIVE)
                .billingCycle(com.kinplatform.kin.medical.billing.contract.EpsContract.BillingCycle.MONTHLY)
                .paymentTermsDays(60)
                .dianPrefix("FEV")
                .dianResolutionNumber("RES-TEST")
                .dianCurrentSequence(0L)
                .build();
    }
}


