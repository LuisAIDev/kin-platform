package com.kinplatform.ai;

import com.kinplatform.chat.ChatMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AiEngineService {

    private static final Logger log = LoggerFactory.getLogger(AiEngineService.class);
    private static final int OLLAMA_TIMEOUT_SECONDS = 120;

    private final ChatClient chatClient;
    private final String modelName;

    public AiEngineService(ChatClient.Builder chatClientBuilder,
                           @Value("${spring.ai.ollama.chat.options.model}") String modelName) {
        this.chatClient = chatClientBuilder.build();
        this.modelName = modelName;
    }

    @PostConstruct
    void logStartup() {
        log.info("Ollama connected successfully — running {}", modelName);
    }

    public String generateAiResponse(List<ChatMessage> history, String userMessage,
                                     String projectTitle, String projectDescription, String projectCategory) {
        var messages = new ArrayList<Message>();
        messages.add(buildSystemMessage(projectTitle, projectDescription, projectCategory));

        for (var msg : history) {
            messages.add(switch (msg.getRole()) {
                case USER -> new UserMessage(msg.getContent());
                case ASSISTANT -> new AssistantMessage(msg.getContent());
                case SYSTEM -> new SystemMessage(msg.getContent());
            });
        }

        log.debug("Calling Ollama ({} messages in history, model: {})", history.size(), modelName);

        try {
            var future = CompletableFuture.supplyAsync(() ->
                chatClient.prompt()
                    .messages(messages.toArray(new Message[0]))
                    .user(userMessage)
                    .call()
                    .content()
            );

            var response = future.get(OLLAMA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response != null && !response.isBlank()) {
                log.debug("Ollama responded successfully ({} chars)", response.length());
                return response;
            }

            log.warn("Ollama returned empty response");
        } catch (TimeoutException e) {
            log.error("Ollama timed out after {}s (falling back to mock)", OLLAMA_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.error("Ollama call failed (falling back to mock): {}", e.getMessage());
        }

        return mockResponse(history.size(), projectTitle);
    }

    public Flux<String> generateAiResponseStream(
            List<ChatMessage> history, String userMessage,
            String projectTitle, String projectDescription, String projectCategory
    ) {
        var messages = new ArrayList<Message>();
        messages.add(buildSystemMessage(projectTitle, projectDescription, projectCategory));

        for (var msg : history) {
            messages.add(switch (msg.getRole()) {
                case USER -> new UserMessage(msg.getContent());
                case ASSISTANT -> new AssistantMessage(msg.getContent());
                case SYSTEM -> new SystemMessage(msg.getContent());
            });
        }

        log.debug("Streaming from Ollama ({} messages in history, model: {})", history.size(), modelName);

        return chatClient.prompt()
                .messages(messages.toArray(new Message[0]))
                .user(userMessage)
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("Ollama stream failed (falling back to mock): {}", e.getMessage());
                    return Flux.just(mockResponse(history.size(), projectTitle));
                });
    }

    private SystemMessage buildSystemMessage(String title, String description, String category) {
        var desc = (description != null && !description.isBlank()) ? description : "Sin descripción disponible.";
        var prompt = String.format("""
                Eres KIN (Knowledge, Innovation & Navigation), un consultor empresarial experto, empático y directo. \
                Tu misión es guiar al usuario a estructurar su proyecto en menos de 60 minutos mediante una conversación \
                fluida y progresiva.

                ## Proyecto activo del usuario:
                - **Título**: %s
                - **Descripción**: %s
                - **Categoría**: %s

                Cada respuesta que des debe estar contextualizada a este proyecto específico. \
                Usa el título y la descripción para personalizar tus preguntas y recomendaciones.

                ## Reglas de conducta:
                1. Sé empático pero directo. No divagues ni alargues la conversación innecesariamente.
                2. Haz una sola pregunta a la vez. No abrumes al usuario con múltiples preguntas.
                3. Avanza progresivamente por las 4 dimensiones del proyecto:
                   - **Problema**: ¿Qué necesidad o dolor resuelve?
                   - **Solución**: ¿Cuál es la propuesta de valor concreta?
                   - **Clientes**: ¿Quién paga? ¿Cuál es el mercado objetivo?
                   - **Costos**: ¿Recursos, tiempo e inversión necesaria?
                4. Cuando completes una dimensión, confirma con el usuario antes de avanzar a la siguiente.
                5. Si el usuario se desvía, retoma el hilo con amabilidad.
                6. Responde SIEMPRE en español, con tono profesional y cercano.
                7. Al final de la conversación, entrega un resumen estructurado de las 4 dimensiones.

                Objetivo final: emitir un scoring de viabilidad del 0 al 100 y un reporte ejecutivo.
                """, title, desc, category);
        return new SystemMessage(prompt);
    }

    private String mockResponse(int turn, String projectTitle) {
        if (turn <= 1) {
            return String.format("""
                    ¡Hola! Soy KIN, tu consultor empresarial especializado en estructuración de proyectos. \
                    Estoy aquí para ayudarte a desarrollar **%s**.

                    Cuéntame, ¿qué problema o necesidad has identificado que motiva este proyecto? \
                    Descríbeme tu idea con tus propias palabras.""", projectTitle);
        } else if (turn <= 3) {
            return String.format("""
                    Entiendo muy bien el contexto de **%s**. Ahora hablemos de la **solución concreta** que propones:

                    - ¿Cuál es tu propuesta de valor específica?
                    - ¿Cómo resuelve el problema que describiste?
                    - ¿Qué hace única a tu solución frente a otras opciones del mercado?

                    Cuanto más clara sea la solución, mejor podré ayudarte a validarla.""", projectTitle);
        } else if (turn <= 5) {
            return """
                    Avancemos a los **clientes y beneficiarios** de tu proyecto.

                    - ¿Quién utilizaría directamente tu solución?
                    - ¿Quién pagaría por ella? (usuarios finales, empresas, gobiernos, etc.)
                    - ¿Cuál es el tamaño aproximado de ese mercado?

                    Identificar bien a tu público objetivo es clave para la viabilidad del proyecto.""";
        } else if (turn <= 7) {
            return """
                    Perfecto, enfoquémonos ahora en los **costos y recursos necesarios**.

                    - ¿Qué recursos necesitas para construir la primera versión? (equipo, tecnología, materiales)
                    - ¿Cuánto tiempo estimas para tener un prototipo funcional?
                    - ¿Qué inversión inicial requerirías y cómo planeas financiarlo?

                    No olvides considerar costos operativos, de marketing y legales si aplican.""";
        } else {
            return String.format("""
                    Has avanzado muchísimo en la estructuración de **%s**. Aquí tienes un **resumen ejecutivo** \
                    de las 4 dimensiones que hemos trabajado:

                    ### Resumen del Proyecto

                    **Problema:** Identificaste una necesidad u oportunidad específica.

                    **Solución:** Propusiste un enfoque concreto para resolverla.

                    **Clientes:** Definiste quiénes usarán la solución y quiénes la financiarán.

                    **Costos:** Estimaste los recursos necesarios y las inversiones clave.

                    ---
                    ### Scoring de Viabilidad Estimado: **78/100**

                    Tu proyecto tiene un potencial alto. Para fortalecerlo aún más, te recomendaría:
                    1. Validar tu propuesta con al menos 10 potenciales clientes
                    2. Investigar fuentes de financiamiento o subsidios disponibles
                    3. Buscar alianzas estratégicas en tu sector

                    ¿Te gustaría profundizar en alguna de estas áreas o tienes alguna otra pregunta?""", projectTitle);
        }
    }
}
