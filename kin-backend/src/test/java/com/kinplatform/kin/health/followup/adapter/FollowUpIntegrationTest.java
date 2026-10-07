package com.kinplatform.kin.health.followup.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Integración del seguimiento de pacientes (ADR-033) con PostgreSQL real
 * (Testcontainers): flujo completo (crear plan → tarea → completar →
 * evolución) y permisos por relación ACTIVE sobre HTTP real con JWT.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "jwt.secret=a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm",
            "springdotenv.enabled=false"
        })
@ActiveProfiles("test")
class FollowUpIntegrationTest extends PostgresTestSupport {

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
    void flujoCompleto_planTareaEvolucion() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.PATIENT, "paciente-" + uuid() + "@test.com");
        physicianPatientRepository.assign(
                PhysicianPatientAssignment.of(physician.getId(), patient.getId(), OffsetDateTime.now()));
        String physicianToken = token(physician);
        String patientToken = token(patient);

        // 1. Crear plan.
        var createPlan = post(physicianToken, "/health/followup/plans", "{\"patientId\":\""
                + patient.getId() + "\",\"title\":\"Control de presión\",\"frequency\":\"DAILY\","
                + "\"startDate\":\"" + OffsetDateTime.now() + "\"}");
        assertEquals(201, createPlan.statusCode());
        String planId = json(createPlan).get("planId").asText();

        // 2. Añadir tarea.
        var addTask = post(physicianToken, "/health/followup/plans/" + planId + "/tasks",
                "{\"description\":\"Tomar medicación\",\"dueDate\":\""
                        + OffsetDateTime.now().plusDays(1) + "\"}");
        assertEquals(201, addTask.statusCode());
        String taskId = json(addTask).get("taskId").asText();

        // 3. El paciente ve el plan activo.
        var active = get(patientToken, "/health/followup/plans/active");
        assertEquals(200, active.statusCode());
        assertTrue(active.body().contains(planId));

        // 4. El paciente completa la tarea.
        var complete = post(patientToken, "/health/followup/tasks/" + taskId + "/complete", "{}");
        assertEquals(200, complete.statusCode());
        assertEquals("COMPLETED", json(complete).get("status").asText());

        // 5. El médico registra evolución y la consulta.
        var evolution = post(physicianToken, "/health/followup/patients/" + patient.getId() + "/evolution",
                "{\"symptoms\":\"Mejoría\",\"vitals\":{\"presion\":\"120/80\"},\"medicationAdherence\":true}");
        assertEquals(201, evolution.statusCode());

        var history = get(physicianToken, "/health/followup/patients/" + patient.getId() + "/evolution");
        assertEquals(200, history.statusCode());
        assertTrue(history.body().contains("Mejoría"));

        // 6. El paciente ve su propio historial (solo lectura).
        var ownHistory = get(patientToken, "/health/followup/evolution");
        assertEquals(200, ownHistory.statusCode());
        assertTrue(ownHistory.body().contains("Mejoría"));
    }

    @Test
    void crearPlan_conRelacionPendiente_deberiaDenegar() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.PATIENT, "paciente-" + uuid() + "@test.com");
        physicianPatientRepository.assign(
                PhysicianPatientAssignment.invitation(physician.getId(), patient.getId(), physician.getId(),
                        OffsetDateTime.now()));
        String physicianToken = token(physician);

        var createPlan = post(physicianToken, "/health/followup/plans", "{\"patientId\":\""
                + patient.getId() + "\",\"title\":\"Plan\",\"frequency\":\"WEEKLY\","
                + "\"startDate\":\"" + OffsetDateTime.now() + "\"}");

        assertEquals(403, createPlan.statusCode());
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

    private HttpRequest getRequest(String url, String token) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
    }

    private HttpRequest jsonRequest(String method, String url, String token, String body) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpResponse<String> get(String token, String path) throws Exception {
        return httpClient.send(getRequest(baseUrl + path, token), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String token, String path, String body) throws Exception {
        return httpClient.send(jsonRequest("POST", baseUrl + path, token, body), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private static String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}

