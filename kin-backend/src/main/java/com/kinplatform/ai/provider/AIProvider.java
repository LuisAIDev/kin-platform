package com.kinplatform.ai.provider;

import com.kinplatform.kin.context.Message;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AIProvider {

    String generateBlocking(List<Message> history,
                            String userMessage,
                            String systemPrompt);

    Flux<String> generateStream(List<Message> history,
                                String userMessage,
                                String systemPrompt);

    String providerName();
}
