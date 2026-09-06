package com.kinplatform.kin.health.physician.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.common.security.JwtService;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integración del ciclo de vida de la relación médico-paciente (V30) con
 * PostgreSQL real (Testcontainers): verifica que la migración V30 crea las
 * columnas de estado/invitación, que el flujo completo
 * (invitar → ver pendientes → aceptar/rechazar) funciona sobre HTTP real con
 * JWT, y que el aislamiento por paciente se mantiene.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "jwt.secret=a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm",
            "springdotenv.enabled=false"
        })
@ActiveProfiles("test")
class PhysicianPatientRelationshipIntegrationTest extends PostgresTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JpaPhysicianPatientRepository patientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }

    @Test
    void migracionV30_deberiaCrearColumnasDeCicloDeVida() {
        var columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = 'physician_patient_assignments'",
                String.class);

        assertTrue(columns.contains("status"), "status no creado por V30");
        assertTrue(columns.contains("invited_by"), "invited_by no creado por V30");
        assertTrue(columns.contains("invited_at"), "invited_at no creado por V30");
        assertTrue(columns.contains("accepted_at"), "accepted_at no creado por V30");
        assertTrue(columns.contains("ended_at"), "ended_at no creado por V30");
        assertTrue(columns.contains("ended_reason"), "ended_reason no creado por V30");

        var check = jdbcTemplate.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                        + "WHERE conname = 'chk_ppa_status'",
                String.class);
        assertTrue(check != null && check.contains("PENDING")
                && check.contains("PENDING_CONSENT")
                && check.contains("ACTIVE") && check.contains("ENDED"),
                "CHECK de estados no permite PENDING_CONSENT (V30+V41)");
    }

    @Test
    void inviteSinConsentimiento_deberiaCrearPendingConsent() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.FREE, "paciente-free-" + uuid() + "@test.com");
        String physicianToken = token(physician);

        // Paciente FREE sin health_data_consent => sin capacidad de paciente:
        // la invitación debe persistir como PENDING_CONSENT (no 409 genérico).
        var invite = httpClient.send(
                jsonRequest("POST", baseUrl + "/health/physician/patients/invite", physicianToken,
                        "{\"patientEmail\":\"" + patient.getEmail() + "\",\"message\":\"Bienvenido\"}"),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, invite.statusCode(), "invitación sin consentimiento no debería devolver 409: " + invite.body());
        assertEquals("PENDING_CONSENT", json(invite).get("status").asText());

        assertEquals(
                RelationshipStatus.PENDING_CONSENT,
                patientRepository.findByPhysicianIdAndPatientId(physician.getId(), patient.getId())
                        .orElseThrow()
                        .status());
        assertFalse(patientRepository.isAssigned(physician.getId(), patient.getId()));
    }

    @Test
    void cicloCompleto_medicoInvita_pacienteAcepta() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.PATIENT, "paciente-" + uuid() + "@test.com");
        String physicianToken = token(physician);
        String patientToken = token(patient);

        // 1. El médico invita por email.
        var invite = httpClient.send(
                jsonRequest("POST", baseUrl + "/health/physician/patients/invite", physicianToken,
                        "{\"patientEmail\":\"" + patient.getEmail() + "\",\"message\":\"Bienvenido\"}"),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, invite.statusCode());
        assertEquals("PENDING", json(invite).get("status").asText());

        // 2. La relación queda PENDING (no permite acceso clínico todavía).
        assertFalse(patientRepository.isAssigned(physician.getId(), patient.getId()));

        // 2b. Acceso a datos del paciente denegado mientras la relación es PENDING (403).
        var denied = httpClient.send(
                getRequest(baseUrl + "/health/physician/patients/" + patient.getId() + "/summary", physicianToken),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, denied.statusCode());

        // 3. El paciente ve la invitación pendiente con el nombre del médico.
        var pending = httpClient.send(
                getRequest(baseUrl + "/health/patient/relationships/pending", patientToken),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pending.statusCode());
        assertEquals(physician.getId().toString(), json(pending).get(0).get("physicianId").asText());

        // 4. El paciente acepta.
        var accept = httpClient.send(
                jsonRequest("POST", baseUrl + "/health/patient/relationships/accept", patientToken,
                        "{\"physicianId\":\"" + physician.getId() + "\"}"),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, accept.statusCode());
        assertEquals("ACTIVE", json(accept).get("status").asText());

        // 5. Ahora la relación está ACTIVA.
        assertTrue(patientRepository.isAssigned(physician.getId(), patient.getId()));
        assertEquals(
                RelationshipStatus.ACTIVE,
                patientRepository.findByPhysicianIdAndPatientId(physician.getId(), patient.getId())
                        .orElseThrow()
                        .status());

        // 6. La lista de pacientes del médico la incluye como ACTIVA.
        var list = httpClient.send(
                getRequest(baseUrl + "/health/physician/patients?status=ACTIVE", physicianToken),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, list.statusCode());
        assertTrue(json(list).get("content").toString().contains(patient.getId().toString()));
    }

    @Test
    void rechazo_deberiaFinalizarRelacion() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.PATIENT, "paciente-" + uuid() + "@test.com");
        String physicianToken = token(physician);
        String patientToken = token(patient);

        httpClient.send(
                jsonRequest("POST", baseUrl + "/health/physician/patients/invite", physicianToken,
                        "{\"patientEmail\":\"" + patient.getEmail() + "\"}"),
                HttpResponse.BodyHandlers.ofString());

        var reject = httpClient.send(
                jsonRequest("POST", baseUrl + "/health/patient/relationships/reject", patientToken,
                        "{\"physicianId\":\"" + physician.getId() + "\"}"),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, reject.statusCode());
        assertEquals("ENDED", json(reject).get("status").asText());
        assertEquals("REJECTED_BY_PATIENT", json(reject).get("endedReason").asText());

        assertFalse(patientRepository.isAssigned(physician.getId(), patient.getId()));
        assertEquals(
                RelationshipStatus.ENDED,
                patientRepository.findByPhysicianIdAndPatientId(physician.getId(), patient.getId())
                        .orElseThrow()
                        .status());
    }

    @Test
    void aislamiento_pacienteNoVeInvitacionesDeOtroPaciente() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patientA = saveUser(UserRole.PATIENT, "paciente-a-" + uuid() + "@test.com");
        var patientB = saveUser(UserRole.PATIENT, "paciente-b-" + uuid() + "@test.com");
        String physicianToken = token(physician);
        String patientAToken = token(patientA);
        String patientBToken = token(patientB);

        httpClient.send(
                jsonRequest("POST", baseUrl + "/health/physician/patients/invite", physicianToken,
                        "{\"patientEmail\":\"" + patientA.getEmail() + "\"}"),
                HttpResponse.BodyHandlers.ofString());

        var pendingA = httpClient.send(
                getRequest(baseUrl + "/health/patient/relationships/pending", patientAToken),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(1, json(pendingA).size());

        var pendingB = httpClient.send(
                getRequest(baseUrl + "/health/patient/relationships/pending", patientBToken),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, pendingB.statusCode());
        assertEquals(0, json(pendingB).size());
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
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
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

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private static String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
