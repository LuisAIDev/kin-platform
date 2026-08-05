package com.kinplatform.ai.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TracingTest {

    @Test
    void start_deberiaGenerarIdsYHacerlosActuales() {
        var correlation = CorrelationContext.start();

        assertNotNull(correlation.correlationId());
        assertEquals(correlation.correlationId(), correlation.requestId());
        assertTrue(correlation.traceId().startsWith("trace-"));
        assertEquals(correlation, CorrelationContext.current());
        CorrelationContext.clear();
    }

    @Test
    void set_deberiaInstalarContextoDeterminista() {
        var correlation = new CorrelationContext.Correlation("c1", "r1", "t1");
        CorrelationContext.set(correlation);

        assertEquals(correlation, CorrelationContext.current());
        CorrelationContext.clear();
    }

    @Test
    void unknown_deberiaNormalizarse() {
        var correlation = new CorrelationContext.Correlation(null, "  ", "");

        assertEquals("unknown", correlation.correlationId());
        assertEquals("unknown", correlation.requestId());
        assertEquals("unknown", correlation.traceId());
    }

    @Test
    void clear_deberiaReiniciarElContexto() {
        CorrelationContext.start();
        CorrelationContext.clear();

        var regenerated = CorrelationContext.current();
        assertNotNull(regenerated.correlationId());
        assertNotEquals("unknown", regenerated.correlationId());
        CorrelationContext.clear();
    }

    @Test
    void mdc_deberiaPropagarseEnElLogEstructurado() {
        Logger logger = (Logger) LoggerFactory.getLogger("kin.observability.knowledge");
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var correlation = new CorrelationContext.Correlation("c-x", "r-x", "t-x");
            CorrelationContext.set(correlation);

            KnowledgeStructuredLog.cycle(42, "OK");

            var hasMdc = appender.list.stream().anyMatch(event -> {
                Map<String, String> mdc = event.getMDCPropertyMap();
                return "c-x".equals(mdc.get("correlationId"))
                    && "r-x".equals(mdc.get("requestId"))
                    && "t-x".equals(mdc.get("traceId"));
            });
            assertTrue(hasMdc);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            CorrelationContext.clear();
            MDC.clear();
        }
    }
}
