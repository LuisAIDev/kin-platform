package com.kinplatform.kin.health.aiassist.adapter;

import com.kinplatform.kin.health.aiassist.port.AIProviderPort;
import com.kinplatform.kin.ai.AIRequest;
import com.kinplatform.kin.ai.AIResponder;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekAIProvider implements AIProviderPort {

    private final AIResponder aiResponder;

    public DeepSeekAIProvider(AIResponder aiResponder) {
        this.aiResponder = aiResponder;
    }

    @Override
    public String generate(String prompt) {
        AIRequest request = new AIRequest(List.of(), prompt,
                "Eres un asistente medico de KIN. Responde exclusivamente en español.");
        return aiResponder.respond(request);
    }

    @Override
    public String generate(String prompt, int maxTokens, double temperature) {
        return generate(prompt);
    }
}
