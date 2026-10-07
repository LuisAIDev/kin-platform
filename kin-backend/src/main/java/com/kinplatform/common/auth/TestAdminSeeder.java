package com.kinplatform.common.auth;

import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Fixture EXCLUSIVO del perfil {@code test} (@Profile("test")).
 *
 * <p>Siembra de forma idempotente un usuario administrador de E2E
 * ({@code admin-e2e@kin.test}) con rol {@code ADMIN}, email verificado y cuenta
 * activa, para que la suite de Playwright pueda ejercitar los endpoints
 * {@code /admin/users/physicians/**} (aprobar/rechazar solicitudes de capacidad
 * profesional) sin depender de un usuario admin preexistente.</p>
 *
 * <p>NO existe endpoint público ni hook que conceda rol ADMIN: los admins se
 * crean por bootstrap. Este bean solo existe bajo {@code @Profile("test")} y
 * NUNCA se expone en producción (igual que {@code TestVerificationController}).</p>
 */
@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class TestAdminSeeder implements ApplicationRunner {

    /** Credenciales de E2E documentadas en tests/physician-flow.spec.ts. */
    public static final String ADMIN_EMAIL = "admin-e2e@kin.test";

    public static final String ADMIN_PASSWORD = "TestPass123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        String normalized = ADMIN_EMAIL.toLowerCase().trim();
        Optional<User> existing = userRepository.findByEmail(normalized);
        if (existing.isPresent()) {
            log.info(
                    "TestAdminSeeder: admin de E2E ya existe, se garantiza rol ADMIN/activo/verificado: {}",
                    normalized);
            User user = existing.get();
            boolean changed = false;
            if (user.getRole() != UserRole.ADMIN) {
                user.setRole(UserRole.ADMIN);
                changed = true;
            }
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                user.setIsActive(true);
                changed = true;
            }
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                user.setEmailVerified(true);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
            return;
        }

        User admin = User.builder()
                .email(normalized)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .fullName("Admin E2E")
                .role(UserRole.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .healthDataConsent(true)
                .build();
        userRepository.save(admin);
        log.info("TestAdminSeeder: admin de E2E creado ({}) con rol ADMIN para pruebas de Playwright.", normalized);
    }
}


