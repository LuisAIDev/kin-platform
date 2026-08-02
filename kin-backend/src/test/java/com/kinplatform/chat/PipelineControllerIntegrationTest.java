package com.kinplatform.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.kin.TestIntegrationPipeline;
import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import com.kinplatform.kin.context.ContextRepository;
import com.kinplatform.kin.context.ProjectContext;
import com.kinplatform.kin.conversation.ConversationOrchestrator;
import com.kinplatform.kin.conversation.history.HistoryWindow;
import com.kinplatform.kin.conversation.policy.DefaultTurnPolicy;
import com.kinplatform.kin.conversation.validation.ResponseGuard;
import com.kinplatform.kin.event.InMemoryDomainEventBus;
import com.kinplatform.project.Project;
import com.kinplatform.project.ProjectRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FASE 9 · E6 — integración del endpoint REST: ChatController →
 * ChatOrchestratorServiceImpl → ConversationOrchestrator → KinMethod →
 * Pipeline real (13 etapas). Solo los servicios JPA y los puertos de
 * infraestructura se simulan (necesarios); el pipeline y la orquestación son
 * reales.
 */
@ExtendWith(MockitoExtension.class)
class PipelineControllerIntegrationTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "test@kin.com";

    @Mock
    private ChatService chatService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private AIResponder aiResponder;

    @Mock
    private Authentication authentication;

    @Test
    void endpointChat_deberiaEjecutarElFlujoCompletoYResponder() {
        var user = User.builder().id(USER_ID).email(EMAIL).role(UserRole.FREE).build();
        var project = mock(Project.class);
        when(project.getTitle()).thenReturn("Proyecto Test");
        when(project.getDescription()).thenReturn("Descripción");
        when(project.getCategory()).thenReturn(null);
        when(project.getUser()).thenReturn(user);

        when(authentication.getName()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(chatService.saveMessage(any(UUID.class), any(UUID.class), any(SaveMessageRequest.class)))
            .thenReturn(ChatMessageResponse.builder().id(UUID.randomUUID()).tokensUsed(10).build());
        when(chatService.getConversationHistory(any(UUID.class), any(UUID.class))).thenReturn(List.of());
        when(contextRepository.findOrCreate(PROJECT_ID, "Proyecto Test", "Descripción", null))
            .thenReturn(TestIntegrationPipeline.fullContext());
        when(aiResponder.respond(any(AIRequest.class)))
            .thenReturn("Aquí tenés el informe de viabilidad completo.");

        var orchestrator = new ConversationOrchestrator(new HistoryWindow(), new DefaultTurnPolicy(),
            TestIntegrationPipeline.realKinMethod(aiResponder, contextRepository,
                new InMemoryDomainEventBus(), PROJECT_ID),
            new ResponseGuard(), contextRepository);
        var service = new ChatOrchestratorServiceImpl(chatService, projectRepository,
            new ObjectMapper(), orchestrator);
        var controller = new ChatController(chatService, service, userRepository);

        var request = new ChatRequest();
        request.setContent("generá el informe");
        ResponseEntity<ChatResponse> response = controller.chat(authentication, PROJECT_ID, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getContent().isBlank());
        assertNotNull(response.getBody().getAssistantMessageId());

        verify(contextRepository).save(any(UUID.class), any(ProjectContext.class));
    }
}
