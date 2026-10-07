package com.kinplatform.kin.health.physician.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.audit.api.AuditService;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import com.kinplatform.common.user.PhysicianVerificationStatus;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PhysicianApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    private PhysicianApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PhysicianApplicationService(userRepository, auditService);
    }

    private User user(UUID id, UserRole role, PhysicianVerificationStatus status, boolean emailVerified) {
        return User.builder()
                .id(id)
                .email("medico@kin.com")
                .fullName("Dr. Test")
                .role(role)
                .emailVerified(emailVerified)
                .physicianVerificationStatus(status)
                .healthDataConsent(true)
                .build();
    }

    private PhysicianApplicationRequest validRequest() {
        return PhysicianApplicationRequest.builder()
                .licenseNumber("CEDULA-12345")
                .specialty("Medicina Interna")
                .country("México")
                .phone("555-1234")
                .healthDataConsent(true)
                .build();
    }

    // ------------------------------------------------------------------
    // Solicitud exitosa
    // ------------------------------------------------------------------

    @Test
    void freeConEmailVerificado_puedeSolicitar() {
        UUID targetId = UUID.randomUUID();
        var targetUser = user(targetId, UserRole.FREE, null, true);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user(targetId, UserRole.FREE, null, true)));

        PhysicianVerificationStatus status = service.requestApplication(targetId, validRequest());

        assertEquals(PhysicianVerificationStatus.PENDING, status);
        verify(auditService).logAccessFromPrincipal(
                eq(AuditAction.PHYSICIAN_APPLICATION_REQUESTED),
                eq(com.kinplatform.common.audit.domain.AuditResourceType.USER),
                any(),
                eq(null),
                argThat(details -> "CEDULA-12345".equals(details.get("licenseNumber"))
                        && "Medicina Interna".equals(details.get("specialty"))
                        && "México".equals(details.get("country"))
                        && details.get("requestedAt") != null));
    }

    @Test
    void premiumConEmailVerificado_puedeSolicitar() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(user(UUID.randomUUID(), UserRole.PREMIUM, null, true)));

        PhysicianVerificationStatus status = service.requestApplication(UUID.randomUUID(), validRequest());

        assertEquals(PhysicianVerificationStatus.PENDING, status);
    }

    @Test
    void patientConEmailVerificado_puedeSolicitar() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(user(UUID.randomUUID(), UserRole.PATIENT, null, true)));

        PhysicianVerificationStatus status = service.requestApplication(UUID.randomUUID(), validRequest());

        assertEquals(PhysicianVerificationStatus.PENDING, status);
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    @Test
    void sinEmailVerificado_noPuedeSolicitar() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(user(UUID.randomUUID(), UserRole.FREE, null, false)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.requestApplication(UUID.randomUUID(), validRequest()));

        assertTrue(ex.getMessage().contains("correo electrónico debe estar verificado"));
        verify(auditService, never()).logAccessFromPrincipal(any(), any(), any(), any(), any());
    }

    @Test
    void conSolicitudPendiente_noPuedeSolicitarOtra() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(
                User.builder().id(UUID.randomUUID()).email("a@kin.com").physicianVerificationStatus(PhysicianVerificationStatus.PENDING).emailVerified(true).build()));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.requestApplication(UUID.randomUUID(), validRequest()));

        assertTrue(ex.getMessage().contains("pendiente de revisión"));
    }

    @Test
    void conSolicitudAprobada_noPuedeSolicitarOtra() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(
                User.builder().id(UUID.randomUUID()).email("a@kin.com").physicianVerificationStatus(PhysicianVerificationStatus.APPROVED).emailVerified(true).build()));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.requestApplication(UUID.randomUUID(), validRequest()));

        assertTrue(ex.getMessage().contains("ya está aprobada"));
    }

@Test
    void rechazado_puedeReSolicitar() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(
                User.builder().id(UUID.randomUUID()).email("a@kin.com").physicianVerificationStatus(PhysicianVerificationStatus.REJECTED).emailVerified(true).build()));

        PhysicianVerificationStatus status = service.requestApplication(UUID.randomUUID(), validRequest());

        assertEquals(PhysicianVerificationStatus.PENDING, status);
    }

    // ------------------------------------------------------------------
    // Consulta de estado
    // ------------------------------------------------------------------

    @Test
    void getApplicationStatus_sinSolicitud_deberiaDevolverNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(
                User.builder().id(UUID.randomUUID()).email("a@kin.com").physicianVerificationStatus(null).role(com.kinplatform.common.user.UserRole.FREE).build()));

        var response = service.getApplicationStatus(UUID.randomUUID());

        assertEquals("NOT_FOUND", response.status());
    }

    @Test
    void getApplicationStatus_pendiente_deberiaDevolverPENDING() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(
                User.builder().id(UUID.randomUUID()).email("a@kin.com").physicianVerificationStatus(PhysicianVerificationStatus.PENDING).role(com.kinplatform.common.user.UserRole.FREE).build()));

        var response = service.getApplicationStatus(UUID.randomUUID());

        assertEquals("PENDING", response.status());
        assertEquals("FREE", response.role());
    }

    @Test
    void getApplicationStatus_aprobado_deberiaDevolverAPPROVED() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(
                User.builder().id(UUID.randomUUID()).email("a@kin.com").physicianVerificationStatus(PhysicianVerificationStatus.APPROVED).role(com.kinplatform.common.user.UserRole.PHYSICIAN).build()));

        var response = service.getApplicationStatus(UUID.randomUUID());

        assertEquals("APPROVED", response.status());
        assertEquals("PHYSICIAN", response.role());
    }

    @Test
    void getApplicationStatus_rechazado_deberiaDevolverREJECTED() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.of(
                User.builder().id(UUID.randomUUID()).email("a@kin.com").physicianVerificationStatus(PhysicianVerificationStatus.REJECTED).role(com.kinplatform.common.user.UserRole.FREE).build()));

        var response = service.getApplicationStatus(UUID.randomUUID());

        assertEquals("REJECTED", response.status());
    }

    @Test
    void getApplicationStatus_usuarioInexistente_deberiaFallar() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.getApplicationStatus(UUID.randomUUID()));
    }
}


