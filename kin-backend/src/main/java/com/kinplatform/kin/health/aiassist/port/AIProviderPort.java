package com.kinplatform.kin.health.aiassist.port;

public interface AIProviderPort {

    String generate(String prompt);

    String generate(String prompt, int maxTokens, double temperature);
}
