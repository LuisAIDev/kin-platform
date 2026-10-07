package com.kinplatform.common.audit.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.security.JwtService;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integración de la auditoría (ADR-035) con PostgreSQL real (Testcontainers):
 * al acceder un médico al resumen de un paciente se genera un log (vía outbox →
 * relay → listener) consultable por el paciente.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "jwt.secret=a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm",
            "springdotenv.enabled=false",
            "kin.outbox.relay.enabled=true"
        })
@ActiveProfiles("test")
class AuditIntegrationTest extends PostgresTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PhysicianPatientRepository physicianPatientRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }

    @Test
    void accesoDelMedico_alResumenDeberiaGenerarLogConsultable() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.PATIENT, "paciente-" + uuid() + "@test.com");
        physicianPatientRepository.assign(
                PhysicianPatientAssignment.of(physician.getId(), patient.getId(), OffsetDateTime.now()));
        String physicianToken = token(physician);
        String patientToken = token(patient);

        // El médico accede al resumen del paciente (genera un log VIEW_SUMMARY).
        var summary = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/health/physician/patients/" + patient.getId() + "/summary"))
                        .header("Authorization", "Bearer " + physicianToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, summary.statusCode());

        // Espera a que el relay (2 s de polling) entregue el evento al listener.
        String myLogs = null;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            var response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(baseUrl + "/health/audit/my-logs"))
                            .header("Authorization", "Bearer " + patientToken)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            myLogs = response.body();
            if (myLogs.contains("VIEW_SUMMARY")) {
                break;
            }
        }

        assertTrue(myLogs != null && myLogs.contains("VIEW_SUMMARY"),
                "El log VIEW_SUMMARY debería aparecer en los logs del paciente");
    }

    private User saveUser(UserRole role, String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("test-hash")
                .fullName(role == UserRole.PHYSICIAN ? "Dr. Test" : "Paciente Test")
                .role(role)
                .build());
    }

    private String token(User user) {
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name(), "EMPRESAS");
    }

    private static String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}


