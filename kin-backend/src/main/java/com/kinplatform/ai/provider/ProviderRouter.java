package com.kinplatform.ai.provider;

import com.kinplatform.kin.context.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class ProviderRouter {

    private static final Logger log = LoggerFactory.getLogger(ProviderRouter.class);

    private final List<AIProvider> providers;

    public ProviderRouter(List<AIProvider> providers) {
        this.providers = List.copyOf(providers);
        log.info("ProviderRouter initialized with {} providers: {}",
                providers.size(),
                providers.stream().map(AIProvider::providerName).toList());
    }

    public String routeBlocking(List<Message> history, String userMessage, String systemPrompt) {
        for (var provider : providers) {
            log.info("Routing to provider: {}", provider.providerName());
            String response = provider.generateBlocking(history, userMessage, systemPrompt);
            if (response != null && !response.isBlank()) {
                log.info("Provider {} responded successfully ({} chars)", provider.providerName(), response.length());
                return response;
            }
            log.warn("Provider {} returned empty or null, trying next", provider.providerName());
        }
        log.warn("All {} providers failed to generate response", providers.size());
        return null;
    }

    public Flux<String> routeStream(List<Message> history, String userMessage, String systemPrompt) {
        if (providers.isEmpty()) {
            return Flux.empty();
        }
        Flux<String> stream = providers.get(0).generateStream(history, userMessage, systemPrompt);
        for (int i = 1; i < providers.size(); i++) {
            var prevProvider = providers.get(i - 1);
            var fallbackProvider = providers.get(i);
            var prevName = prevProvider.providerName();
            stream = stream.onErrorResume(e -> {
                log.warn("Provider {} stream failed ({}), falling back to {}",
                        prevName, e.getMessage(), fallbackProvider.providerName());
                return fallbackProvider.generateStream(history, userMessage, systemPrompt);
            });
        }
        return stream;
    }
}
