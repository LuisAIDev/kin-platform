package com.kinplatform.common.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.kinplatform.common.auth.dto.AuthResponse;
import com.kinplatform.common.auth.dto.LoginRequest;
import com.kinplatform.common.auth.dto.UserDTO;
import com.kinplatform.common.auth.email.EmailSender;
import com.kinplatform.common.auth.verification.EmailVerificationTokenService;
import com.kinplatform.common.security.JwtService;
import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.UserSubscriptionRepository;
import com.kinplatform.common.user.PhysicianVerificationStatus;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Compatibilidad del campo derivado {@code verticalAccess} (Commit 1).
 *
 * <p>Verifica que {@code AuthServiceImpl.computeVerticalAccess(User)} se expone
 * correctamente tanto en {@code login} como en {@code getCurrentUser} según el
 * rol y la capacidad profesional del usuario. El cálculo es DERIVADO: nunca se
 * persiste en la entidad {@code User}.</p>
 */
@ExtendWith(MockitoExtension.class)
class VerticalAccessTest {

    private static final String EMAIL = "user@kin.com";
    private static final String TOKEN = "jwt-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailVerificationTokenService tokenService;

    @Mock
    private EmailSender emailSender;

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, tokenService, emailSender, planRepository, subscriptionRepository);
    }

    private User user(UserRole role, PhysicianVerificationStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(role)
                .emailVerified(true)
                .physicianVerificationStatus(status)
                .build();
    }

    private AuthResponse login(User user) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString(), anyString(), anyString())).thenReturn(TOKEN);
        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");
        return authService.login(req);
    }

    private UserDTO me(User user) {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        return authService.getCurrentUser(TOKEN);
    }

    // ------------------------------------------------------------------
    // login()
    // ------------------------------------------------------------------

    @Test
    void login_free_deberiaExponerSoloEmpresas() {
        assertEquals(List.of("empresas"), login(user(UserRole.FREE, null)).getVerticalAccess());
    }

    @Test
    void login_premium_deberiaExponerSoloEmpresas() {
        assertEquals(List.of("empresas"), login(user(UserRole.PREMIUM, null)).getVerticalAccess());
    }

    @Test
    void login_facilitador_deberiaExponerSoloEmpresas() {
        assertEquals(List.of("empresas"), login(user(UserRole.FACILITADOR, null)).getVerticalAccess());
    }

    @Test
    void login_admin_deberiaExponerSoloEmpresas() {
        assertEquals(List.of("empresas"), login(user(UserRole.ADMIN, null)).getVerticalAccess());
    }

    @Test
    void login_patient_deberiaExponerSoloMedical() {
        assertEquals(List.of("medical"), login(user(UserRole.PATIENT, null)).getVerticalAccess());
    }

    @Test
    void login_physician_deberiaExponerSoloMedical() {
        assertEquals(List.of("medical"), login(user(UserRole.PHYSICIAN, null)).getVerticalAccess());
    }

    @Test
    void login_freeAprobado_deberiaExponerEmpresasYMedical() {
        assertEquals(
                List.of("empresas", "medical"),
                login(user(UserRole.FREE, PhysicianVerificationStatus.APPROVED)).getVerticalAccess());
    }

    @Test
    void login_freeConSolicitudPendiente_deberiaExponerSoloEmpresas() {
        assertEquals(
                List.of("empresas"),
                login(user(UserRole.FREE, PhysicianVerificationStatus.PENDING)).getVerticalAccess());
    }

    // ------------------------------------------------------------------
    // getCurrentUser()
    // ------------------------------------------------------------------

    @Test
    void me_free_deberiaExponerSoloEmpresas() {
        assertEquals(List.of("empresas"), me(user(UserRole.FREE, null)).getVerticalAccess());
    }

    @Test
    void me_admin_deberiaExponerSoloEmpresas() {
        assertEquals(List.of("empresas"), me(user(UserRole.ADMIN, null)).getVerticalAccess());
    }

    @Test
    void me_patient_deberiaExponerSoloMedical() {
        assertEquals(List.of("medical"), me(user(UserRole.PATIENT, null)).getVerticalAccess());
    }

    @Test
    void me_physician_deberiaExponerSoloMedical() {
        assertEquals(List.of("medical"), me(user(UserRole.PHYSICIAN, null)).getVerticalAccess());
    }

    @Test
    void me_freeAprobado_deberiaExponerEmpresasYMedical() {
        assertEquals(
                List.of("empresas", "medical"),
                me(user(UserRole.FREE, PhysicianVerificationStatus.APPROVED)).getVerticalAccess());
    }
}


