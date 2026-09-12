package com.kinplatform.kin.health.scheduling.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.security.JwtService;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integración de la agenda y disponibilidad (ADR-034) con PostgreSQL real
 * (Testcontainers): disponibilidad → slots → reserva → confirmación → traslape
 * (409) sobre HTTP real con JWT.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "jwt.secret=a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm",
            "springdotenv.enabled=false"
        })
@ActiveProfiles("test")
class SchedulingIntegrationTest extends PostgresTestSupport {

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
    void flujoCompleto_disponibilidadReservaConfirmacionYTraslape() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.PATIENT, "paciente-" + uuid() + "@test.com");
        physicianPatientRepository.assign(
                PhysicianPatientAssignment.of(physician.getId(), patient.getId(), OffsetDateTime.now()));
        String physicianToken = token(physician);
        String patientToken = token(patient);

        // 1. El médico define disponibilidad los lunes de 09:00 a 10:00 (30 min → 2 slots).
        var availability = post(physicianToken, "/health/scheduling/availability",
                "{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"slotDurationMinutes\":30}");
        assertEquals(200, availability.statusCode());

        LocalDate monday = nextMonday();

        // 2. El paciente ve los slots de su médico para ese lunes.
        var slots = get(patientToken, "/health/scheduling/physicians/" + physician.getId() + "/slots?date=" + monday);
        assertEquals(200, slots.statusCode());
        assertEquals(2, objectMapper.readTree(slots.body()).size());
        String firstSlot = objectMapper.readTree(slots.body()).get(0).asText();

        // 3. El paciente solicita la cita en el primer slot.
        var request = post(patientToken, "/health/scheduling/appointments/request",
                "{\"physicianId\":\"" + physician.getId() + "\",\"scheduledAt\":\"" + firstSlot
                        + "\",\"durationMinutes\":30,\"reason\":\"Control\"}");
        assertEquals(201, request.statusCode());
        String appointmentId = json(request).get("id").asText();

        // 4. El médico la confirma.
        var confirm = put(physicianToken, "/health/scheduling/appointments/" + appointmentId + "/confirm");
        assertEquals(200, confirm.statusCode());
        assertEquals("CONFIRMADA", json(confirm).get("status").asText());

        // 5. La cita aparece en "próximas" del paciente.
        var upcoming = get(patientToken, "/health/scheduling/appointments/upcoming");
        assertEquals(200, upcoming.statusCode());
        assertTrue(upcoming.body().contains(appointmentId));

        // 6. El slot ocupado ya no aparece disponible.
        var slotsAfter = get(patientToken, "/health/scheduling/physicians/" + physician.getId() + "/slots?date=" + monday);
        assertEquals(1, objectMapper.readTree(slotsAfter.body()).size());

        // 7. Solicitar el mismo slot de nuevo → 409 (traslape).
        var duplicate = post(patientToken, "/health/scheduling/appointments/request",
                "{\"physicianId\":\"" + physician.getId() + "\",\"scheduledAt\":\"" + firstSlot
                        + "\",\"durationMinutes\":30,\"reason\":\"Duplicada\"}");
        assertEquals(409, duplicate.statusCode());
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
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name(), user.getPlatform());
    }

    private HttpResponse<String> get(String token, String path) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String token, String path, String body) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String token, String path) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + token)
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private static LocalDate nextMonday() {
        int today = LocalDate.now().getDayOfWeek().getValue();
        return LocalDate.now().plusDays((8 - today) % 7);
    }

    private static String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
