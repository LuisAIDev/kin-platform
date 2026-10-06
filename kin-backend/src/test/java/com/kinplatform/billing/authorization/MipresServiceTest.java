package com.kinplatform.billing.authorization;

import com.kinplatform.billing.rips.model.RipsBatch;
import com.kinplatform.billing.rips.model.RipsBatchRepository;
import com.kinplatform.billing.contract.EpsContract;
import com.kinplatform.billing.contract.EpsContractRepository;
import com.kinplatform.common.security.TenantContext;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import com.kinplatform.common.audit.api.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MipresServiceTest {

    @Mock
    private MipresClient mipresClient;

    @Mock
    private MipresPrescriptionRepository prescriptionRepository;

    @Mock
    private MipresSupplyRepository supplyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MipresAuthorizationService authService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MipresService mipresService;

    private UUID orgId;
    private UUID contractId;
    private UUID patientId;
    private UUID userId;
    private String userEmail = "test@kin.com";
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        contractId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.set(orgId);

        when(authService.getOrganizationNit(orgId)).thenReturn("123456789");

        var user = User.builder()
                .id(userId)
                .email(userEmail)
                .role(UserRole.IPS_ADMIN)
                .build();
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        auth = new UsernamePasswordAuthenticationToken(userEmail, null);
    }

    @Test
    void createPrescription_success() {
        String authNumber = "AUTH-2024-001";
        MipresClient.AuthorizationData authData = new MipresClient.AuthorizationData(
                authNumber, "890201", 10, java.math.BigDecimal.valueOf(50000), "I10");

        when(mipresClient.consultarAutorizacion(authNumber, contractId)).thenReturn(Optional.of(authData));
        when(prescriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var prescription = mipresService.createPrescription(
                auth,
                new MipresService.CreatePrescriptionRequest(authNumber, contractId, patientId));

        assertNotNull(prescription);
        assertEquals(authNumber, prescription.getPrescriptionNumber());
        assertEquals(MipresPrescription.PrescriptionStatus.AUTHORIZED, prescription.getStatus());
        assertEquals("890201", prescription.getCupsCode());
        verify(prescriptionRepository).save(any());
    }

    @Test
    void createPrescription_notFound_throwsException() {
        String authNumber = "AUTH-999999";
        when(mipresClient.consultarAutorizacion(authNumber, contractId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                mipresService.createPrescription(
                        auth,
                        new MipresService.CreatePrescriptionRequest(authNumber, contractId, patientId))
        );
    }

    @Test
    void reportSupply_success() {
        String authNumber = "AUTH-2024-001";
        MipresPrescription prescription = MipresPrescription.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .contractId(contractId)
                .patientId(patientId)
                .prescriptionNumber(authNumber)
                .status(MipresPrescription.PrescriptionStatus.AUTHORIZED)
                .cupsCode("890201")
                .build();

        when(prescriptionRepository.findByPrescriptionNumber(authNumber))
                .thenReturn(Optional.of(prescription));
        when(mipresClient.reportarUso(eq(authNumber), eq(contractId), anyString(), anyInt(), any()))
                .thenReturn(new MipresClient.ConsumptionResult(true, null));
        when(supplyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var supply = mipresService.reportSupply(
                auth,
                new MipresService.ReportSupplyRequest(authNumber, "890201", 5,
                        java.math.BigDecimal.valueOf(50000),
                        java.math.BigDecimal.valueOf(10000),
                        "LOTE-123", LocalDate.now().plusMonths(12)));

        assertNotNull(supply);
        assertEquals(authNumber, supply.getPrescriptionNumber());
        assertEquals(MipresSupply.SupplyStatus.REPORTED, supply.getStatus());
        assertNotNull(supply.getSupplyId());
        verify(supplyRepository).save(any());
    }

    @Test
    void reportSupply_invalidAuth_throwsException() {
        String authNumber = "AUTH-999";
        MipresPrescription prescription = MipresPrescription.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .contractId(contractId)
                .patientId(patientId)
                .prescriptionNumber(authNumber)
                .status(MipresPrescription.PrescriptionStatus.AUTHORIZED)
                .cupsCode("890201")
                .build();

        when(prescriptionRepository.findByPrescriptionNumber(authNumber))
                .thenReturn(Optional.of(prescription));
        when(mipresClient.reportarUso(anyString(), any(), anyString(), anyInt(), any()))
                .thenReturn(new MipresClient.ConsumptionResult(false, "Error MIPRES"));

        assertThrows(IllegalStateException.class, () ->
                mipresService.reportSupply(
                        auth,
                        new MipresService.ReportSupplyRequest(authNumber, "890201", 1,
                                java.math.BigDecimal.valueOf(1000),
                                java.math.BigDecimal.valueOf(1000),
                                null, null))
        );
    }

    @Test
    void anularSupply_success() {
        UUID supplyId = UUID.randomUUID();
        MipresSupply supply = MipresSupply.builder()
                .id(supplyId)
                .organizationId(orgId)
                .status(MipresSupply.SupplyStatus.REPORTED)
                .build();

        when(supplyRepository.findById(supplyId)).thenReturn(Optional.of(supply));
        when(supplyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = mipresService.anularSupply(
                auth,
                supplyId, "Motivo de prueba");

        assertEquals(MipresSupply.SupplyStatus.ANULLED, result.getStatus());
        verify(supplyRepository).save(any());
    }

    @Test
    void anularSupply_alreadyAnulled_throwsException() {
        UUID supplyId = UUID.randomUUID();
        MipresSupply supply = MipresSupply.builder()
                .id(supplyId)
                .organizationId(orgId)
                .status(MipresSupply.SupplyStatus.ANULLED)
                .build();

        when(supplyRepository.findById(supplyId)).thenReturn(Optional.of(supply));

        assertThrows(IllegalStateException.class, () ->
                mipresService.anularSupply(auth, supplyId, "Motivo")
        );
    }
}
