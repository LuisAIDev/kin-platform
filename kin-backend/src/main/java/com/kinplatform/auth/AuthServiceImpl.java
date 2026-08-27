package com.kinplatform.auth;

import com.kinplatform.auth.dto.AuthResponse;
import com.kinplatform.auth.dto.LoginRequest;
import com.kinplatform.auth.dto.PatientRegisterRequest;
import com.kinplatform.auth.dto.PhysicianRegisterRequest;
import com.kinplatform.auth.dto.RegisterRequest;
import com.kinplatform.auth.dto.UserDTO;
import com.kinplatform.auth.email.EmailSender;
import com.kinplatform.auth.verification.EmailVerificationTokenService;
import com.kinplatform.auth.verification.VerifyEmailOutcome;
import com.kinplatform.common.security.JwtService;
import com.kinplatform.user.PhysicianVerificationStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationTokenService tokenService;
    private final EmailSender emailSender;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        var email = normalize(request.getEmail());
        validatePasswordStrength(request.getPassword());
        requireFullName(request.getFullName());

        // Política anti-enumeración: si el email ya existe, se responde con la
        // MISMA respuesta genérica que un registro nuevo (201, sin token, sin
        // verificación). No se revela si el correo existe, ni el estado de la
        // cuenta, ni si está verificado.
        if (userRepository.existsByEmail(email)) {
            return genericRegisterResponse(email, request.getFullName().trim(), UserRole.FREE, null);
        }

        var user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(UserRole.FREE)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        sendVerification(user);

        return genericRegisterResponse(user.getEmail(), user.getFullName(), UserRole.FREE, null);
    }

    @Override
    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        var email = normalize(request.getEmail());
        validatePasswordStrength(request.getPassword());
        requireFullName(request.getFullName());
        requireHealthConsent(request.getHealthDataConsent());

        if (userRepository.existsByEmail(email)) {
            return genericRegisterResponse(email, request.getFullName().trim(), UserRole.PATIENT, null);
        }

        var user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(UserRole.PATIENT)
                .emailVerified(false)
                .dateOfBirth(request.getDateOfBirth())
                .sex(request.getSex() == null ? null : request.getSex().trim())
                .phone(request.getPhone() == null ? null : request.getPhone().trim())
                .healthDataConsent(true)
                .build();

        user = userRepository.save(user);

        sendVerification(user);

        return genericRegisterResponse(user.getEmail(), user.getFullName(), UserRole.PATIENT, null);
    }

    @Override
    @Transactional
    public AuthResponse registerPhysician(PhysicianRegisterRequest request) {
        var email = normalize(request.getEmail());
        validatePasswordStrength(request.getPassword());
        requireFullName(request.getFullName());
        requireHealthConsent(request.getHealthDataConsent());
        String license = validateLicenseNumber(request.getLicenseNumber());

        if (userRepository.existsByEmail(email)) {
            return genericRegisterResponse(
                    email, request.getFullName().trim(), UserRole.PHYSICIAN, PhysicianVerificationStatus.PENDING);
        }

        var user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(UserRole.PHYSICIAN)
                .emailVerified(false)
                .licenseNumber(license)
                .specialty(request.getSpecialty().trim())
                .country(request.getCountry().trim())
                .phone(request.getPhone() == null ? null : request.getPhone().trim())
                .physicianVerificationStatus(PhysicianVerificationStatus.PENDING)
                .healthDataConsent(true)
                .build();

        user = userRepository.save(user);

        sendVerification(user);

        return genericRegisterResponse(
                user.getEmail(), user.getFullName(), UserRole.PHYSICIAN, PhysicianVerificationStatus.PENDING);
    }

    private static AuthResponse genericRegisterResponse(
            String email, String fullName, UserRole role, PhysicianVerificationStatus verificationStatus) {
        return AuthResponse.builder()
                .email(email)
                .fullName(fullName)
                .role(role.name())
                .emailVerified(false)
                .verificationStatus(verificationStatus == null ? null : verificationStatus.name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var email = request.getEmail().toLowerCase().trim();

        var user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            // TRADE-OFF CONSCIENTE (UX vs anti-enumeración): este error revela que
            // el correo existe y no está verificado. Es un comportamiento aprobado
            // por producto para guiar al usuario legítimo hacia la pantalla de
            // verificación; el abuso se mitiga con rate limiting. No cambiar sin
            // autorización.
            throw new EmailVerificationRequiredException("Tu correo electrónico aún no ha sido verificado. "
                    + "Revisa tu bandeja de entrada para activar tu cuenta.");
        }

        if (user.getRole() == UserRole.PHYSICIAN
                && (user.getPhysicianVerificationStatus() == PhysicianVerificationStatus.PENDING
                        || user.getPhysicianVerificationStatus() == PhysicianVerificationStatus.REJECTED)) {
            throw new PhysicianPendingReviewException("Tu cuenta está siendo verificada por nuestro equipo. "
                    + "Recibirás un correo cuando sea aprobada.");
        }

        var token = jwtService.generateToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .emailVerified(true)
                .verificationStatus(
                        user.getPhysicianVerificationStatus() == null
                                ? null
                                : user.getPhysicianVerificationStatus().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String refreshAccessToken(String refreshToken) {
        return jwtService.refreshAccessToken(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(String token) {
        if (token == null || !jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        var email = jwtService.extractEmail(token);

        var user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .credits(user.getCredits())
                .emailVerified(user.getEmailVerified())
                .verificationStatus(
                        user.getPhysicianVerificationStatus() == null
                                ? null
                                : user.getPhysicianVerificationStatus().name())
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            jwtService.blacklistToken(token);
        }
    }

    @Override
    @Transactional
    public VerifyEmailOutcome verifyEmail(String token) {
        return tokenService.verify(token);
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        var normalized = email.toLowerCase().trim();
        var user = userRepository.findByEmail(normalized).orElse(null);
        if (user == null) {
            return;
        }
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }
        if (tokenService.isWithinCooldown(user.getId())) {
            return;
        }
        sendVerification(user);
    }

    private void sendVerification(User user) {
        String token = tokenService.createForUser(user);
        String link = baseUrl() + "/verify-email?token=" + token;
        emailSender.sendVerificationEmail(user.getEmail(), user.getFullName(), link);
    }

    private String baseUrl() {
        return frontendBaseUrl == null || frontendBaseUrl.isBlank() ? "http://localhost:3000" : frontendBaseUrl;
    }

    private static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }

        long classes = java.util.stream.Stream.of(
                        password.matches(".*[a-z].*"),
                        password.matches(".*[A-Z].*"),
                        password.matches(".*\\d.*"),
                        password.matches(".*[^A-Za-z0-9].*"))
                .filter(Boolean::booleanValue)
                .count();

        if (classes < 3) {
            throw new IllegalArgumentException(
                    "La contraseña debe combinar letras mayúsculas, minúsculas, números o símbolos");
        }
    }

    private static String normalize(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    private static void requireFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
    }

    private static void requireHealthConsent(Boolean consent) {
        if (!Boolean.TRUE.equals(consent)) {
            throw new IllegalArgumentException(
                    "Debes aceptar el consentimiento para el tratamiento de tus datos de salud");
        }
    }

    /**
     * Valida el formato de la cédula profesional (4-20 caracteres alfanuméricos,
     * opcionalmente con guiones, normalizados a mayúsculas). Evita valores vacíos
     * o con caracteres inválidos.
     */
    private static String validateLicenseNumber(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new IllegalArgumentException("El número de cédula profesional es obligatorio");
        }
        String normalized = licenseNumber.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9-]{4,20}")) {
            throw new IllegalArgumentException(
                    "El número de cédula profesional no es válido (4-20 caracteres alfanuméricos)");
        }
        return normalized;
    }
}
