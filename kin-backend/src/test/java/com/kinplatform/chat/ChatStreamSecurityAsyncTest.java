package com.kinplatform.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kinplatform.common.security.JwtService;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.StreamingTurnOutcome;
import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.SupportLevel;
import com.kinplatform.pricing.ViabilityScoringDetail;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;

/**
 * Reproducción y regresión del fallo de SSE + Spring Security en el async
 * dispatch (POST /api/v1/projects/{id}/chat/stream).
 *
 * <p>Arranca Tomcat real ({@code RANDOM_PORT}) y la cadena de seguridad
 * completa. El {@code ConversationOrchestrator} se mockea para emitir un
 * stream inmediato (sin DeepSeek real). Verifica que el SSE completa con HTTP
 * 200 sin {@code AccessDeniedException} durante {@code CoyoteAdapter.asyncDispatch}
 * y que la protección del endpoint se mantiene (sin JWT / JWT inválido
 * rechazados, otros endpoints siguen protegidos).</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "jwt.secret=a2luLXBsYXRmb3JtLXNlY3VyZS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLWNlcnRpZmljYXRpb24tMjAyNi0wMTIzNDU2Nzg5YWJjZGVm",
            "springdotenv.enabled=false"
        })
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class ChatStreamSecurityAsyncTest extends PostgresTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ConversationOrchestrator conversationOrchestrator;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void seedPlan() {
        if (pricingPlanRepository.findByName("FREE_TEST").isEmpty()) {
            pricingPlanRepository.save(PricingPlan.builder()
                    .name("FREE_TEST")
                    .price(BigDecimal.ZERO)
                    .features("[]")
                    .maxProjects(1000)
                    .messagesPerMonth(1000)
                    .supportLevel(SupportLevel.BASIC)
                    .viabilityScoringDetail(ViabilityScoringDetail.BASIC)
                    .isActive(true)
                    .build());
        }
    }

    @Test
    void streamingAutenticadoRoLeFree_deberiaCompletar200SinAccessDenied(CapturedOutput output) throws Exception {
        var user = userRepository.save(User.builder()
                .email("async-" + UUID.randomUUID() + "@test.com")
                .passwordHash("test-hash")
                .fullName("Async Test")
                .role(UserRole.FREE)
                .build());
        var project = projectRepository.save(
                Project.builder().user(user).title("Proyecto Async").build());
        var token = jwtService.generateToken(
                user.getId(), user.getEmail(), user.getRole().name(), "EMPRESAS");

        when(conversationOrchestrator.orchestrateStreamWithOutcome(any()))
                .thenReturn(new StreamingTurnOutcome(Flux.just("hola", "mundo"), null, null));

        var response = postStream(project.getId(), token);

        Thread.sleep(2500);

        assertThat(response.statusCode()).as("status").isEqualTo(200);
        assertThat(response.body()).contains("\"done\"");
        assertThat(response.body()).contains("hola");
        assertThat(output.getOut())
                .as("no debe aparecer AccessDeniedException en el async dispatch")
                .doesNotContain("AccessDeniedException")
                .doesNotContain("Access Denied");
    }

    @Test
    void streamingSinJWT_deberiaSerRechazado() throws Exception {
        var projectId = UUID.randomUUID();
        var response = postStream(projectId, null);
        assertThat(response.statusCode()).isIn(401, 403);
    }

    @Test
    void streamingConJWTInvalido_deberiaSerRechazado() throws Exception {
        var projectId = UUID.randomUUID();
        var response = postStream(projectId, "token.invalido");
        assertThat(response.statusCode()).isIn(401, 403);
    }

    @Test
    void historialSinJWT_deberiaSeguirProtegido() throws Exception {
        var request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/projects/" + UUID.randomUUID() + "/messages"))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isIn(401, 403);
    }

    private StreamResult postStream(UUID projectId, String token) throws Exception {
        var builder = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/projects/" + projectId + "/chat/stream"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString("{\"content\":\"hola\"}"));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        String body;
        try (var is = response.body()) {
            body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return new StreamResult(response.statusCode(), body);
    }

    private record StreamResult(int statusCode, String body) {}
}
