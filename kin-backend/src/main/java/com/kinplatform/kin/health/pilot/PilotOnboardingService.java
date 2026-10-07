package com.kinplatform.kin.health.pilot;

import com.kinplatform.kin.health.physician.api.PhysicianService;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Onboarding del grupo piloto (fase piloto).
 *
 * <p>Crea de forma idempotente los usuarios del piloto (pacientes y médicos)
 * con una contraseña común, los marca como verificados y activos, y asigna
 * pacientes a médicos. Diseñado para ser disparado por un administrador vía
 * endpoint; los datos son deterministas (correos predefinidos). No debe usarse
 * con datos reales en producción permanente.</p>
 */
@Service
public class PilotOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(PilotOnboardingService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhysicianService physicianService;

    public PilotOnboardingService(
            UserRepository userRepository, PasswordEncoder passwordEncoder, PhysicianService physicianService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.physicianService = physicianService;
    }

    /**
     * Crea el grupo piloto y devuelve el resumen.
     *
     * @param patients  correos de pacientes (roles PATIENT)
     * @param physicians correos de médicos (roles PHYSICIAN)
     * @param password   contraseña común para todos los usuarios
     * @param assignments pares {@code (patientEmail, physicianEmail)} a asignar
     */
    @Transactional
    public PilotSetupResult setup(
            List<String> patients, List<String> physicians, String password, List<Assignment> assignments) {
        List<String> createdUsers = new ArrayList<>();
        for (String email : patients) {
            createdUsers.add(createUser(email, UserRole.PATIENT, password));
        }
        for (String email : physicians) {
            createdUsers.add(createUser(email, UserRole.PHYSICIAN, password));
        }

        int assigned = 0;
        for (Assignment a : assignments) {
            UUID patientId = userRepository
                    .findByEmail(a.patientEmail().toLowerCase().trim())
                    .map(User::getId)
                    .orElse(null);
            UUID physicianId = userRepository
                    .findByEmail(a.physicianEmail().toLowerCase().trim())
                    .map(User::getId)
                    .orElse(null);
            if (patientId == null || physicianId == null) {
                log.warn(
                        "PilotOnboardingService: asignación omitida (usuario inexistente): {} → {}",
                        a.patientEmail(),
                        a.physicianEmail());
                continue;
            }
            physicianService.assignPatient(physicianId, patientId);
            assigned++;
        }

        log.info(
                "PilotOnboardingService: grupo piloto creado ({} usuarios, {} asignaciones)",
                createdUsers.size(),
                assigned);
        return new PilotSetupResult(createdUsers.size(), assigned);
    }

    private String createUser(String email, UserRole role, String password) {
        String normalized = email.toLowerCase().trim();
        Optional<User> existing = userRepository.findByEmail(normalized);
        if (existing.isPresent()) {
            log.info("PilotOnboardingService: usuario ya existe, se marca activo/verificado: {}", normalized);
            User user = existing.get();
            user.setRole(role);
            user.setIsActive(true);
            user.setEmailVerified(true);
            userRepository.save(user);
            return normalized;
        }
        User user = User.builder()
                .email(normalized)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(friendlyName(normalized))
                .role(role)
                .isActive(true)
                .emailVerified(true)
                .build();
        userRepository.save(user);
        return normalized;
    }

    private static String friendlyName(String email) {
        String local = email.contains("@") ? email.split("@")[0] : email;
        String[] parts = local.split("[._-]");
        var sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /** Resultado del setup. */
    public record PilotSetupResult(int usersCreated, int assignmentsCreated) {}

    /** Par de asignación paciente → médico. */
    public record Assignment(String patientEmail, String physicianEmail) {}
}

