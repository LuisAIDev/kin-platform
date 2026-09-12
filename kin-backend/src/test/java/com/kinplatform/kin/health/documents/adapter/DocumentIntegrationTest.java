package com.kinplatform.kin.health.documents.adapter;

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
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integración de documentos clínicos (ADR-036) con PostgreSQL real
 * (Testcontainers): el médico sube un documento, el paciente lo lista y lo
 * descarga, sobre HTTP real con JWT y almacenamiento local temporal.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "jwt.secret=a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm",
            "springdotenv.enabled=false",
            "kin.health.documents.storage-path=./target/test-storage-documents"
        })
@ActiveProfiles("test")
class DocumentIntegrationTest extends PostgresTestSupport {

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
    void flujoCompleto_subirListarYDescargar() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.PATIENT, "paciente-" + uuid() + "@test.com");
        physicianPatientRepository.assign(
                PhysicianPatientAssignment.of(physician.getId(), patient.getId(), OffsetDateTime.now()));
        String physicianToken = token(physician);
        String patientToken = token(patient);

        // 1. El médico sube un documento multipart.
        String boundary = "----kin" + uuid();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"patientId\"\r\n\r\n" + patient.getId() + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"description\"\r\n\r\nResultado de laboratorio\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"resultado.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n"
                + "CONTENIDO_PDF\r\n"
                + "--" + boundary + "--\r\n";
        var upload = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/health/documents/upload"))
                        .header("Authorization", "Bearer " + physicianToken)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, upload.statusCode());
        String documentId = objectMapper.readTree(upload.body()).get("id").asText();

        // 2. El paciente lista sus documentos.
        var my = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/health/documents/my"))
                        .header("Authorization", "Bearer " + patientToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, my.statusCode());
        JsonNode list = objectMapper.readTree(my.body());
        assertTrue(list.size() >= 1);
        assertEquals("resultado.pdf", list.get(0).get("fileName").asText());

        // 3. El paciente descarga el documento.
        var download = httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/health/documents/" + documentId + "/download"))
                        .header("Authorization", "Bearer " + patientToken)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, download.statusCode());
        assertTrue(download.body().contains("CONTENIDO_PDF"));
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
