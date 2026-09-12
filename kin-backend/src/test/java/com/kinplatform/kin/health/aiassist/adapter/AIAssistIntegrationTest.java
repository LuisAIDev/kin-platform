package com.kinplatform.kin.health.aiassist.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "jwt.secret=a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm",
            "springdotenv.enabled=false",
            "kin.health.aiassist.enabled=true"
        })
@ActiveProfiles("test")
class AIAssistIntegrationTest extends PostgresTestSupport {

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
    void generarResumen_deberiaFuncionar() throws Exception {
        var physician = saveUser(UserRole.PHYSICIAN, "medico-ai-" + uuid() + "@test.com");
        var patient = saveUser(UserRole.PATIENT, "paciente-ai-" + uuid() + "@test.com");
        physicianPatientRepository.assign(
                PhysicianPatientAssignment.of(physician.getId(), patient.getId(), OffsetDateTime.now()));
        String physicianToken = token(physician);

        var body = """
            {"differentialResult":{"items":[],"unrecognizedSymptoms":[],"confidence":0.8,"explanation":"test","generatedBy":"test","engineVersion":"1.0"}}
            """;

        var request = HttpRequest.newBuilder(URI.create(baseUrl + "/health/aiassist/differential-explain"))
                .header("Authorization", "Bearer " + physicianToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertNotNull(json.get("id"));
        assertEquals("EXPLAIN", json.get("type").asText());
    }

    private User saveUser(UserRole role, String email) {
        return userRepository.save(User.builder()
                .email(email).passwordHash("test-hash")
                .fullName(role == UserRole.PHYSICIAN ? "Dr. AI" : "Paciente AI")
                .role(role).build());
    }

    private String token(User user) {
        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name(), "EMPRESAS");
    }

    private static String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
