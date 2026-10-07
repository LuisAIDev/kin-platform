package com.kinplatform.common.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.auth.dto.AuthResponse;
import com.kinplatform.common.auth.dto.LoginRequest;
import com.kinplatform.common.auth.dto.PatientRegisterRequest;
import com.kinplatform.common.auth.dto.PhysicianRegisterRequest;
import com.kinplatform.common.auth.dto.RegisterRequest;
import com.kinplatform.common.auth.dto.UserDTO;
import com.kinplatform.common.auth.email.EmailSender;
import com.kinplatform.common.auth.verification.EmailVerificationTokenService;
import com.kinplatform.common.auth.verification.VerifyEmailOutcome;
import com.kinplatform.common.security.JwtService;
import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.UserSubscriptionRepository;
import com.kinplatform.common.user.PhysicianVerificationStatus;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

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

    private RegisterRequest registerRequest() {
        var req = new RegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("KINpass123!a");
        req.setFullName("KIN User");
        return req;
    }

    private User unverifiedUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .emailVerified(false)
                .build();
    }

    private User verifiedUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .emailVerified(true)
                .build();
    }

    @Test
    void register_deberiaCrearUsuarioNoVerificadoYNoEntregarToken() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("KINpass123!a")).thenReturn("hashed");
        var user = unverifiedUser();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenService.createForUser(user)).thenReturn("verify-token");

        AuthResponse response = authService.register(registerRequest());

        assertNull(response.getToken());
        assertFalse(response.getEmailVerified());
        assertEquals(EMAIL, response.getEmail());
        assertEquals("FREE", response.getRole());
        verify(tokenService).createForUser(user);
        verify(emailSender)
                .sendVerificationEmail(EMAIL, "KIN User", "http://localhost:3000/verify-email/confirm?token=verify-token");
    }

    @Test
    void register_conEmailExistente_deberiaResponderGenericoSinEnumeracion() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        AuthResponse response = authService.register(registerRequest());

        assertNull(response.getToken());
        assertFalse(response.getEmailVerified());
        assertEquals(EMAIL, response.getEmail());
        verify(tokenService, never()).createForUser(any(User.class));
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void register_passwordCorta_deberiaFallar() {
        var req = registerRequest();
        req.setPassword("corta");

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_passwordDe7Caracteres_deberiaFallar() {
        var req = registerRequest();
        req.setPassword("Ab1!234");

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_passwordDe8CaracteresConVariacion_deberiaSerAceptada() {
        var req = registerRequest();
        req.setPassword("Passw0rd!");
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed");
        var user = unverifiedUser();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenService.createForUser(user)).thenReturn("verify-token");

        authService.register(req);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_passwordSinVariacion_deberiaFallar() {
        var req = registerRequest();
        req.setPassword("abcdefghijklmn");

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void login_conCredencialesValidasYVerificado_deberiaDevolverToken() {
        var user = verifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user.getId(), EMAIL, "FREE", "EMPRESAS")).thenReturn(TOKEN);

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");

        AuthResponse response = authService.login(req);

        assertEquals(TOKEN, response.getToken());
        assertTrue(response.getEmailVerified());
    }

    @Test
    void login_noVerificado_deberiaLanzarEmailVerificationRequired() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");

        var ex = assertThrows(EmailVerificationRequiredException.class, () -> authService.login(req));

        assertTrue(ex.getMessage().contains("no ha sido verificado"));
    }

    @Test
    void login_conPasswordIncorrecto_deberiaLanzar() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("wrong");

        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    void login_conEmailInexistente_deberiaLanzar() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");

        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    void getCurrentUser_conTokenValido_deberiaDevolverDatosConEmailVerified() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.PREMIUM)
                .credits(10)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDTO dto = authService.getCurrentUser(TOKEN);

        assertEquals(EMAIL, dto.getEmail());
        assertEquals("PREMIUM", dto.getRole());
        assertTrue(dto.getEmailVerified());
    }

    @Test
    void getCurrentUser_conTokenInvalido_deberiaLanzar() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.getCurrentUser(TOKEN));
    }

    @Test
    void getCurrentUser_conUsuarioInexistente_deberiaLanzar() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.getCurrentUser(TOKEN));
    }

    @Test
    void logout_deberiaBlacklistearElToken() {
        authService.logout(TOKEN);

        verify(jwtService).blacklistToken(TOKEN);
    }

    @Test
    void logout_conTokenNulo_noDeberiaBlacklistear() {
        authService.logout(null);

        verify(jwtService, never()).blacklistToken(anyString());
    }

    @Test
    void register_deberiaMinusculizarElEmail() {
        when(userRepository.existsByEmail("user@kin.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        var user = User.builder()
                .id(UUID.randomUUID())
                .email("user@kin.com")
                .passwordHash("hashed")
                .fullName("KIN User")
                .role(UserRole.FREE)
                .emailVerified(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenService.createForUser(user)).thenReturn("verify-token");

        var req = registerRequest();
        req.setEmail("USER@KIN.COM");

        authService.register(req);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyEmail_deberiaDelegarEnElTokenService() {
        when(tokenService.verify("tok")).thenReturn(VerifyEmailOutcome.SUCCESS);

        assertEquals(VerifyEmailOutcome.SUCCESS, authService.verifyEmail("tok"));
    }

    @Test
    void verifyEmail_noDeberiaGenerarJwtNiCrearSesion() {
        when(tokenService.verify("tok")).thenReturn(VerifyEmailOutcome.SUCCESS);

        authService.verifyEmail("tok");

        verify(jwtService, never()).generateToken(any(), anyString(), anyString(), anyString());
    }

    @Test
    void resend_conUsuarioNoVerificadoYFueraDeCooldown_deberiaEnviar() {
        var user = unverifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenService.isWithinCooldown(user.getId())).thenReturn(false);
        when(tokenService.createForUser(user)).thenReturn("new-token");

        ResendVerificationStatus result = authService.resendVerification(EMAIL);

        assertEquals(ResendVerificationStatus.SENT, result);
        verify(emailSender)
                .sendVerificationEmail(EMAIL, "KIN User", "http://localhost:3000/verify-email/confirm?token=new-token");
    }

    @Test
    void resend_conEmailInexistente_deberiaDevolverNO_ACCOUNT() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResendVerificationStatus result = authService.resendVerification(EMAIL);

        assertEquals(ResendVerificationStatus.NO_ACCOUNT, result);
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resend_conUsuarioVerificado_deberiaDevolverALREADY_VERIFIED() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));

        ResendVerificationStatus result = authService.resendVerification(EMAIL);

        assertEquals(ResendVerificationStatus.ALREADY_VERIFIED, result);
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resend_dentroDeCooldown_deberiaDevolverCOOLDOWN() {
        var user = unverifiedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenService.isWithinCooldown(user.getId())).thenReturn(true);

        ResendVerificationStatus result = authService.resendVerification(EMAIL);

        assertEquals(ResendVerificationStatus.COOLDOWN, result);
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resend_emailVacio_deberiaDevolverNO_ACCOUNT() {
        assertEquals(ResendVerificationStatus.NO_ACCOUNT, authService.resendVerification("  "));
    }

    // ---------- Auto-registro vertical Salud ----------

    private PatientRegisterRequest patientRequest() {
        var req = new PatientRegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("KINpass123!a");
        req.setFullName("Ana Paciente");
        req.setDateOfBirth(LocalDate.of(1990, 5, 15));
        req.setSex("FEMENINO");
        req.setPhone("+34600000000");
        req.setHealthDataConsent(true);
        return req;
    }

    private PhysicianRegisterRequest physicianRequest() {
        var req = new PhysicianRegisterRequest();
        req.setEmail(EMAIL);
        req.setPassword("KINpass123!a");
        req.setFullName("Dr. García");
        req.setLicenseNumber("cedula-12345");
        req.setSpecialty("Medicina Interna");
        req.setCountry("España");
        req.setPhone("+34600000000");
        req.setHealthDataConsent(true);
        return req;
    }

    @Test
    void registerPatient_deberiaCrearPacienteEnviarVerificacionYSinToken() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("KINpass123!a")).thenReturn("hashed");
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("Ana Paciente")
                .role(UserRole.PATIENT)
                .emailVerified(false)
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .sex("FEMENINO")
                .phone("+34600000000")
                .healthDataConsent(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenService.createForUser(user)).thenReturn("verify-token");

        AuthResponse response = authService.registerPatient(patientRequest());

        assertNull(response.getToken());
        assertFalse(response.getEmailVerified());
        assertEquals("PATIENT", response.getRole());
        verify(userRepository)
                .save(argThat(u -> u.getRole() == UserRole.PATIENT && Boolean.TRUE.equals(u.getHealthDataConsent())));
        verify(emailSender)
                .sendVerificationEmail(EMAIL, "Ana Paciente", "http://localhost:3000/verify-email/confirm?token=verify-token");
    }

    @Test
    void registerPatient_sinConsentimiento_deberiaFallar() {
        var req = patientRequest();
        req.setHealthDataConsent(false);

        assertThrows(IllegalArgumentException.class, () -> authService.registerPatient(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerPatient_conEmailExistente_deberiaResponderGenericoSinEnumeracion() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        AuthResponse response = authService.registerPatient(patientRequest());

        assertNull(response.getToken());
        assertEquals("PATIENT", response.getRole());
        verify(userRepository, never()).save(any(User.class));
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void registerPhysician_deberiaCrearMedicoPendienteEnviarVerificacion() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("KINpass123!a")).thenReturn("hashed");
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("Dr. García")
                .role(UserRole.PHYSICIAN)
                .emailVerified(false)
                .licenseNumber("CEDULA-12345")
                .specialty("Medicina Interna")
                .country("España")
                .physicianVerificationStatus(PhysicianVerificationStatus.PENDING)
                .healthDataConsent(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenService.createForUser(user)).thenReturn("verify-token");

        AuthResponse response = authService.registerPhysician(physicianRequest());

        assertNull(response.getToken());
        assertFalse(response.getEmailVerified());
        assertEquals("PHYSICIAN", response.getRole());
        assertEquals("PENDING", response.getVerificationStatus());
        assertEquals(AuthResponse.STATE_NEW_REGISTRATION, response.getState());
        verify(userRepository)
                .save(argThat(u -> u.getRole() == UserRole.PHYSICIAN
                        && u.getPhysicianVerificationStatus() == PhysicianVerificationStatus.PENDING
                        && "CEDULA-12345".equals(u.getLicenseNumber())));
        verify(emailSender)
                .sendVerificationEmail(EMAIL, "Dr. García", "http://localhost:3000/verify-email/confirm?token=verify-token");
    }

    @Test
    void registerPhysician_emailExistenteVerificado_deberiaResponder201SinEnviar() {
        var existing = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .fullName("Dr. Existente")
                .role(UserRole.FREE)
                .emailVerified(true)
                .physicianVerificationStatus(null)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        AuthResponse response = authService.registerPhysician(physicianRequest());

        assertEquals(AuthResponse.STATE_ACCOUNT_ALREADY_VERIFIED, response.getState());
        assertTrue(response.getEmailVerified());
        assertEquals("FREE", response.getRole());
        verify(userRepository, never()).save(any(User.class));
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void registerPhysician_emailExistenteNoVerificado_deberiaResponder201SinEnviar() {
        var existing = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .fullName("Dr. Existente")
                .role(UserRole.FREE)
                .emailVerified(false)
                .physicianVerificationStatus(null)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        AuthResponse response = authService.registerPhysician(physicianRequest());

        assertEquals(AuthResponse.STATE_ACCOUNT_NOT_VERIFIED, response.getState());
        assertFalse(response.getEmailVerified());
        verify(userRepository, never()).save(any(User.class));
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void registerPhysician_emailConSolicitudPendiente_deberiaResponderPENDING() {
        var existing = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .fullName("Dr. Existente")
                .role(UserRole.PATIENT)
                .emailVerified(true)
                .physicianVerificationStatus(PhysicianVerificationStatus.PENDING)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        AuthResponse response = authService.registerPhysician(physicianRequest());

        assertEquals(AuthResponse.STATE_PHYSICIAN_PENDING, response.getState());
        assertEquals("PENDING", response.getVerificationStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerPhysician_emailConSolicitudAprobada_deberiaResponderAPPROVED() {
        var existing = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .fullName("Dr. Existente")
                .role(UserRole.FREE)
                .emailVerified(true)
                .physicianVerificationStatus(PhysicianVerificationStatus.APPROVED)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        AuthResponse response = authService.registerPhysician(physicianRequest());

        assertEquals(AuthResponse.STATE_PHYSICIAN_APPROVED, response.getState());
        assertTrue(response.isPhysicianCapability());
    }

    @Test
    void registerPhysician_emailConSolicitudRechazada_deberiaResponderREJECTED() {
        var existing = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .fullName("Dr. Existente")
                .role(UserRole.FREE)
                .emailVerified(true)
                .physicianVerificationStatus(PhysicianVerificationStatus.REJECTED)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        AuthResponse response = authService.registerPhysician(physicianRequest());

        assertEquals(AuthResponse.STATE_PHYSICIAN_REJECTED, response.getState());
        assertFalse(response.isPhysicianCapability());
    }

    @Test
    void registerPhysician_cedulaInvalida_deberiaFallar() {
        var req = physicianRequest();
        req.setLicenseNumber("¡inválida!");

        assertThrows(IllegalArgumentException.class, () -> authService.registerPhysician(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerPhysician_sinConsentimiento_deberiaFallar() {
        var req = physicianRequest();
        req.setHealthDataConsent(false);

        assertThrows(IllegalArgumentException.class, () -> authService.registerPhysician(req));
        verify(userRepository, never()).save(any(User.class));
    }

    private User physicianUser(PhysicianVerificationStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("Dr. García")
                .role(UserRole.PHYSICIAN)
                .emailVerified(true)
                .physicianVerificationStatus(status)
                .build();
    }

    private LoginRequest loginRequest() {
        var req = new LoginRequest();
        req.setEmail(EMAIL);
        req.setPassword("password123");
        return req;
    }

    @Test
    void login_medicoPendiente_deberiaLanzarPendingReview() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(physicianUser(PhysicianVerificationStatus.PENDING)));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        var ex = assertThrows(PhysicianPendingReviewException.class, () -> authService.login(loginRequest()));

        assertTrue(ex.getMessage().contains("siendo verificada"));
    }

    @Test
    void login_medicoRechazado_deberiaLanzarPendingReview() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(physicianUser(PhysicianVerificationStatus.REJECTED)));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        assertThrows(PhysicianPendingReviewException.class, () -> authService.login(loginRequest()));
    }

    @Test
    void login_medicoAprobado_deberiaDevolverToken() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(physicianUser(PhysicianVerificationStatus.APPROVED)));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(TOKEN);

        AuthResponse response = authService.login(loginRequest());

        assertEquals(TOKEN, response.getToken());
        assertEquals("APPROVED", response.getVerificationStatus());
    }

    @Test
    void login_medicoPilotoSinEstado_deberiaDevolverToken() {
        // M�dicos del piloto/admin tienen verificationStatus null  equivalen a aprobados.
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(physicianUser(null)));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(TOKEN);

        AuthResponse response = authService.login(loginRequest());

        assertEquals(TOKEN, response.getToken());
        assertNull(response.getVerificationStatus());
    }

    // ------------------------------------------------------------------
    // physicianCapability expuesta en login (Alternativa B)
    // ------------------------------------------------------------------

    @Test
    void login_freeAprobado_deberiaExponerPhysicianCapabilityTrue() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("Dr. Free Aprobado")
                .role(UserRole.FREE)
                .emailVerified(true)
                .physicianVerificationStatus(PhysicianVerificationStatus.APPROVED)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString(), anyString(), anyString())).thenReturn(TOKEN);

        AuthResponse response = authService.login(loginRequest());

        assertTrue(response.isPhysicianCapability());
    }

    @Test
    void login_freePendiente_deberiaExponerPhysicianCapabilityFalse() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .passwordHash("hashed")
                .fullName("Dr. Free Pendiente")
                .role(UserRole.FREE)
                .emailVerified(true)
                .physicianVerificationStatus(PhysicianVerificationStatus.PENDING)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString(), anyString(), anyString())).thenReturn(TOKEN);

        AuthResponse response = authService.login(loginRequest());

        assertFalse(response.isPhysicianCapability());
    }

    @Test
    void login_medicoAprobado_deberiaExponerPhysicianCapabilityTrue() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(physicianUser(PhysicianVerificationStatus.APPROVED)));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(any(UUID.class), anyString(), anyString(), anyString())).thenReturn(TOKEN);

        AuthResponse response = authService.login(loginRequest());

        assertTrue(response.isPhysicianCapability());
    }

    // ------------------------------------------------------------------
    // physicianCapability expuesta en /auth/me (getCurrentUser)
    // ------------------------------------------------------------------

    @Test
    void getCurrentUser_freeAprobado_deberiaExponerPhysicianCapabilityTrue() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .role(UserRole.FREE)
                .emailVerified(true)
                .physicianVerificationStatus(PhysicianVerificationStatus.APPROVED)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDTO dto = authService.getCurrentUser(TOKEN);

        assertTrue(dto.isPhysicianCapability());
    }

    @Test
    void getCurrentUser_medicoPendiente_deberiaExponerPhysicianCapabilityFalse() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .role(UserRole.PHYSICIAN)
                .emailVerified(true)
                .physicianVerificationStatus(PhysicianVerificationStatus.PENDING)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDTO dto = authService.getCurrentUser(TOKEN);

        assertFalse(dto.isPhysicianCapability());
    }

    @Test
    void getCurrentUser_medicoLegacyNull_deberiaExponerPhysicianCapabilityTrue() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        var user = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .role(UserRole.PHYSICIAN)
                .emailVerified(true)
                .physicianVerificationStatus(null)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        UserDTO dto = authService.getCurrentUser(TOKEN);

        assertTrue(dto.isPhysicianCapability());
    }
}


