package com.kinplatform.kin.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@ConfigurationProperties(prefix = "kin.outbox.relay")
@Validated
public class OutboxRelayProperties {

    private boolean enabled = true;

    @Positive
    private long pollIntervalMs = 2000;

    @Min(1)
    @Max(1000)
    private int batchSize = 100;

    @Min(0)
    @Max(100)
    private int maxRetries = 5;

    @Positive
    private long backoffBaseMs = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getBackoffBaseMs() {
        return backoffBaseMs;
    }

    public void setBackoffBaseMs(long backoffBaseMs) {
        this.backoffBaseMs = backoffBaseMs;
    }
}