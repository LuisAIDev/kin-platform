package com.kinplatform.common.ai.provider;

import com.kinplatform.common.context.Message;
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


