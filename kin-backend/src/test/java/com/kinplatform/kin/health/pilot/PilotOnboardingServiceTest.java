package com.kinplatform.kin.health.pilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.api.PhysicianService;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PilotOnboardingServiceTest {

    private final Map<String, User> users = new LinkedHashMap<>();

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private InMemoryPhysicianRepositories physicianRepos;

    @BeforeEach
    void setUp() {
        users.clear();
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.findByEmail(any()))
                .thenAnswer(inv -> Optional.ofNullable(users.get(normalize(inv.getArgument(0)))));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(UUID.randomUUID());
            }
            users.put(normalize(u.getEmail()), u);
            return u;
        });
        physicianRepos = new InMemoryPhysicianRepositories();
    }

    private static String normalize(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    private static HealthQuotaPort healthQuotaPort() {
        return new HealthQuotaPort() {
            @Override
            public Integer getMaxTriagesPerMonth(UUID userId) {
                return null;
            }

            @Override
            public Integer getMaxPatients(UUID physicianId) {
                return null;
            }

            @Override
            public Integer getTrialDays(UUID userId) {
                return null;
            }

            @Override
            public Integer getTriagesUsed(UUID userId) {
                return 0;
            }
        };
    }

    private PilotOnboardingService service() {
        var auditProps = new com.kinplatform.kin.health.audit.config.AuditProperties();
        auditProps.setEnabled(false);
        var physicianService = new PhysicianService(
                physicianRepos.patientRepository(),
                physicianRepos.alertRepository(),
                null,
                null,
                userRepository,
                new PhysicianProperties(),
                new com.kinplatform.kin.health.physician.access.RelationshipAccessValidator(
                        physicianRepos.patientRepository()),
                new com.kinplatform.kin.health.audit.api.AuditService(null, null, null, auditProps),
                healthQuotaPort());
        return new PilotOnboardingService(userRepository, passwordEncoder, physicianService);
    }

    @Test
    void setup_deberiaCrearUsuariosYAsignaciones() {
        var result = service()
                .setup(
                        List.of("paciente@kin.com"),
                        List.of("medico@kin.com"),
                        "password123",
                        List.of(new PilotOnboardingService.Assignment("paciente@kin.com", "medico@kin.com")));

        assertEquals(2, result.usersCreated());
        assertEquals(1, result.assignmentsCreated());
        assertEquals(UserRole.PATIENT, users.get("paciente@kin.com").getRole());
        assertEquals(UserRole.PHYSICIAN, users.get("medico@kin.com").getRole());
        assertTrue(users.get("paciente@kin.com").getEmailVerified());
        UUID patientId = users.get("paciente@kin.com").getId();
        assertTrue(physicianRepos.patientRepository().findAllPatientIds().contains(patientId));
    }

    @Test
    void setup_conUsuarioExistente_deberiaActualizarRolYVerificado() {
        users.put(
                "paciente@kin.com",
                User.builder()
                        .id(UUID.randomUUID())
                        .email("paciente@kin.com")
                        .role(UserRole.FREE)
                        .isActive(false)
                        .emailVerified(false)
                        .build());

        var result = service().setup(List.of("paciente@kin.com"), List.of("medico@kin.com"), "password123", List.of());

        assertEquals(2, result.usersCreated());
        assertEquals(UserRole.PATIENT, users.get("paciente@kin.com").getRole());
        assertTrue(users.get("paciente@kin.com").getEmailVerified());
        assertTrue(users.get("paciente@kin.com").getIsActive());
    }

    @Test
    void setup_conAsignacionAInexistente_deberiaOmitirla() {
        var result = service()
                .setup(
                        List.of("paciente@kin.com"),
                        List.of("medico@kin.com"),
                        "password123",
                        List.of(new PilotOnboardingService.Assignment("x@kin.com", "y@kin.com")));

        assertEquals(0, result.assignmentsCreated());
        assertEquals(2, result.usersCreated());
    }
}
