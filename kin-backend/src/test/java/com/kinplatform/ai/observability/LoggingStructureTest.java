package com.kinplatform.ai.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingStructureTest {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private Logger attach() {
        Logger logger = (Logger) LoggerFactory.getLogger("kin.observability.knowledge");
        appender.start();
        logger.addAppender(appender);
        return logger;
    }

    private void detach(Logger logger) {
        logger.detachAppender(appender);
        appender.stop();
    }

    @AfterEach
    void cleanup() {
        CorrelationContext.clear();
    }

    @Test
    void cycle_deberiaEmitirLogEstructuradoConCorrelacion() {
        Logger logger = attach();
        CorrelationContext.set(new CorrelationContext.Correlation("corr-1", "req-1", "trace-1"));
        KnowledgeStructuredLog.cycle(123, "OK");
        detach(logger);

        assertTrue(appender.list.stream().anyMatch(event -> {
            String message = event.getFormattedMessage();
            return message.contains("\"correlationId\": \"corr-1\"")
                && message.contains("\"requestId\": \"req-1\"")
                && message.contains("\"traceId\": \"trace-1\"")
                && message.contains("\"durationMs\": 123")
                && message.contains("\"result\": \"OK\"");
        }));
    }

    @Test
    void eventYError_deberianEmitirseConMdc() {
        Logger logger = attach();
        CorrelationContext.set(new CorrelationContext.Correlation("corr-2", "req-2", "trace-2"));
        KnowledgeStructuredLog.event("cache_hit", "HIT");
        KnowledgeStructuredLog.error("provider caído");
        detach(logger);

        assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("knowledge.event")));
        assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("knowledge.error")));
        assertTrue(appender.list.stream().anyMatch(event -> {
            Map<String, String> mdc = event.getMDCPropertyMap();
            return "corr-2".equals(mdc.get("correlationId"))
                && "req-2".equals(mdc.get("requestId"))
                && "trace-2".equals(mdc.get("traceId"));
        }));
    }

    @Test
    void nunca_deberiaRegistrarDatosSensibles() {
        Logger logger = attach();
        CorrelationContext.set(new CorrelationContext.Correlation("corr-3", "req-3", "trace-3"));
        KnowledgeStructuredLog.cycle(5, "OK");
        KnowledgeStructuredLog.error("error interno");
        detach(logger);

        assertFalse(appender.list.stream().anyMatch(event -> {
            String message = event.getFormattedMessage().toLowerCase();
            return message.contains("password") || message.contains("secret")
                || message.contains("token") || message.contains("api-key")
                || message.contains("prompt");
        }));
    }

    @Test
    void escape_deberiaProtegerComillasEnValores() {
        Logger logger = attach();
        CorrelationContext.set(new CorrelationContext.Correlation("corr\"4", "req-4", "trace-4"));
        KnowledgeStructuredLog.cycle(1, "OK");
        detach(logger);

        assertTrue(appender.list.stream().anyMatch(event ->
            event.getFormattedMessage().contains("corr\\\"4")));
    }
}
