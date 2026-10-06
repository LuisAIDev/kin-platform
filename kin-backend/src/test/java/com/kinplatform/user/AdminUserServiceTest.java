package com.kinplatform.user;

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

import com.kinplatform.auth.password.PasswordResetService;
import com.kinplatform.common.audit.api.AuditService;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.UserSubscription;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private AuditService auditService;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, passwordResetService, auditService);
    }

    private User pendingUser(UUID id, UserRole role, UserSubscription subscription, PricingPlan plan) {
        return User.builder()
                .id(id)
                .email("solicitante@kin.com")
                .fullName("Dr. Solicitante")
                .role(role)
                .physicianVerificationStatus(PhysicianVerificationStatus.PENDING)
                .licenseNumber("CEDULA-999")
                .specialty("Medicina Interna")
                .country("México")
                .subscription(subscription)
                .currentPlan(plan)
                .build();
    }

    // ------------------------------------------------------------------
    // Listado cross-rol
    // ------------------------------------------------------------------

    @Test
    void pendingPhysicians_deberiaListarSolicitudesDeCualquierRol() {
        var free = pendingUser(UUID.randomUUID(), UserRole.FREE, null, null);
        var patient = pendingUser(UUID.randomUUID(), UserRole.PATIENT, null, null);
        var physician = pendingUser(UUID.randomUUID(), UserRole.PHYSICIAN, null, null);
        when(userRepository.findByPhysicianVerificationStatus(PhysicianVerificationStatus.PENDING))
                .thenReturn(List.of(free, patient, physician));

        List<PendingPhysicianResponse> result = service.pendingPhysicians();

        assertEquals(3, result.size());
        assertEquals("FREE", result.get(0).getRole());
        assertEquals("PATIENT", result.get(1).getRole());
        assertEquals("PHYSICIAN", result.get(2).getRole());
        verify(userRepository, never())
                .findByRoleAndPhysicianVerificationStatus(any(), any());
    }

    // ------------------------------------------------------------------
    // Aprobación de FREE+PENDING (rol y plan intactos, auditoría)
    // ------------------------------------------------------------------

    @Test
    void aprobarFreePendiente_deberiaPasarAApproved_sinCambiarRolNiPlan() {
        UUID id = UUID.randomUUID();
        var subscription = UserSubscription.builder().id(UUID.randomUUID()).build();
        var plan = PricingPlan.builder().id(UUID.randomUUID()).code("FREE").build();
        var user = pendingUser(id, UserRole.FREE, subscription, plan);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        service.setVerificationStatus(id, PhysicianVerificationStatus.APPROVED);

        assertEquals(PhysicianVerificationStatus.APPROVED, user.getPhysicianVerificationStatus());
        assertEquals(UserRole.FREE, user.getRole());
        assertEquals(subscription, user.getSubscription());
        assertEquals(plan, user.getCurrentPlan());
        verify(userRepository).save(user);
        verify(auditService)
                .logAccessFromPrincipal(
                        eq(AuditAction.PHYSICIAN_APPLICATION_APPROVED),
                        eq(AuditResourceType.USER),
                        eq(id),
                        isNull(),
                        argThat(details -> "PENDING".equals(details.get("previousStatus"))
                                && "APPROVED".equals(details.get("newStatus"))
                                && details.get("reviewedAt") != null));
    }

    @Test
    void aprobarPatientPendiente_deberiaPasarAApproved() {
        UUID id = UUID.randomUUID();
        var user = pendingUser(id, UserRole.PATIENT, null, null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        service.setVerificationStatus(id, PhysicianVerificationStatus.APPROVED);

        assertEquals(PhysicianVerificationStatus.APPROVED, user.getPhysicianVerificationStatus());
        assertEquals(UserRole.PATIENT, user.getRole());
    }

    // ------------------------------------------------------------------
    // Rechazo de FREE+PENDING con motivo
    // ------------------------------------------------------------------

    @Test
    void rechazarFreePendiente_conMotivo_deberiaPasarARejectedYAuditar() {
        UUID id = UUID.randomUUID();
        var user = pendingUser(id, UserRole.FREE, null, null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        service.setVerificationStatus(id, PhysicianVerificationStatus.REJECTED, "Documentación insuficiente.");

        assertEquals(PhysicianVerificationStatus.REJECTED, user.getPhysicianVerificationStatus());
        assertEquals(UserRole.FREE, user.getRole());
        verify(auditService)
                .logAccessFromPrincipal(
                        eq(AuditAction.PHYSICIAN_APPLICATION_REJECTED),
                        eq(AuditResourceType.USER),
                        eq(id),
                        isNull(),
                        argThat(details -> "PENDING".equals(details.get("previousStatus"))
                                && "REJECTED".equals(details.get("newStatus"))
                                && "Documentación insuficiente.".equals(details.get("reason"))));
    }

    // ------------------------------------------------------------------
    // Guardas
    // ------------------------------------------------------------------

    @Test
    void decidirSobreUsuarioSinSolicitudPendiente_deberiaFallar() {
        UUID id = UUID.randomUUID();
        var user = User.builder()
                .id(id)
                .email("ghost@kin.com")
                .role(UserRole.FREE)
                .physicianVerificationStatus(null)
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.setVerificationStatus(id, PhysicianVerificationStatus.APPROVED));

        assertTrue(ex.getMessage().contains("no tiene una solicitud profesional pendiente"));
        verify(userRepository, never()).save(any());
        verify(auditService, never()).logAccessFromPrincipal(any(), any(), any(), any(), any());
    }

    @Test
    void decidirConEstadoPENDING_comoNuevoEstado_deberiaFallar() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.setVerificationStatus(UUID.randomUUID(), PhysicianVerificationStatus.PENDING));

        verify(auditService, never()).logAccessFromPrincipal(any(), any(), any(), any(), any());
    }

    @Test
    void decidirSobreUsuarioInexistente_deberiaFallar() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.setVerificationStatus(id, PhysicianVerificationStatus.APPROVED));
    }
}

