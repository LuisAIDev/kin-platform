package com.kinplatform.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.chat.dto.ChatMessageResponse;
import com.kinplatform.chat.dto.ChatRequest;
import com.kinplatform.chat.dto.ChatResponse;
import com.kinplatform.chat.dto.SaveMessageRequest;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String EMAIL = "u@kin.com";

    @Mock
    private ChatService chatService;

    @Mock
    private ChatOrchestratorService chatOrchestratorService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatService, chatOrchestratorService, userRepository);
    }

    private void stubUser() {
        when(authentication.getName()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.builder().id(USER_ID).email(EMAIL).build()));
    }

    private ChatRequest request() {
        var request = new ChatRequest();
        request.setContent("hola");
        return request;
    }

    @Test
    void chat_deberiaDelegarYResponder() {
        stubUser();
        var response = ChatResponse.builder().content("respuesta").build();
        when(chatOrchestratorService.processMessage(USER_ID, PROJECT_ID, request()))
                .thenReturn(response);

        ResponseEntity<ChatResponse> result = controller.chat(authentication, PROJECT_ID, request());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("respuesta", result.getBody().getContent());
    }

    @Test
    void chat_sinAutenticacion_deberiaFallar() {
        assertThrows(IllegalArgumentException.class, () -> controller.chat(null, PROJECT_ID, request()));
    }

    @Test
    void chatStream_deberiaDevolverElEmitter() {
        stubUser();
        var emitter = mock(SseEmitter.class);
        when(chatOrchestratorService.processMessageStream(USER_ID, PROJECT_ID, request()))
                .thenReturn(emitter);

        SseEmitter result = controller.chatStream(authentication, PROJECT_ID, request());

        assertSame(emitter, result);
    }

    @Test
    void saveMessage_deberiaResponder201() {
        stubUser();
        var request = new SaveMessageRequest();
        request.setContent("msg");
        var response = ChatMessageResponse.builder().id(UUID.randomUUID()).build();
        when(chatService.saveMessage(USER_ID, PROJECT_ID, request)).thenReturn(response);

        ResponseEntity<ChatMessageResponse> result = controller.saveMessage(authentication, PROJECT_ID, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void getHistory_deberiaDevolverMensajes() {
        stubUser();
        var messages =
                List.of(ChatMessageResponse.builder().id(UUID.randomUUID()).build());
        when(chatService.getConversationHistory(USER_ID, PROJECT_ID)).thenReturn(messages);

        ResponseEntity<List<ChatMessageResponse>> result = controller.getHistory(authentication, PROJECT_ID);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void clearConversation_deberiaResponder204() {
        stubUser();

        ResponseEntity<Void> result = controller.clearConversation(authentication, PROJECT_ID);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(chatService).clearConversation(USER_ID, PROJECT_ID);
    }

    @Test
    void usuarioNoEncontrado_deberiaFallar() {
        when(authentication.getName()).thenReturn("ghost@kin.com");
        when(userRepository.findByEmail("ghost@kin.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> controller.chat(authentication, PROJECT_ID, request()));
    }
}
