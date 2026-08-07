package com.kinplatform.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    private String captureMdcDuringChain() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        doAnswer(invocation -> {
                    captured.set(MDC.get("correlationId"));
                    return null;
                })
                .when(filterChain)
                .doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        return captured.get();
    }

    @Test
    void sinHeader_deberiaGenerarCorrelationIdYPropagarlo() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        String mdc = captureMdcDuringChain();

        assertNotNull(mdc);
        assertNull(MDC.get("correlationId"));
        verify(response).setHeader("X-Request-Id", mdc);
    }

    @Test
    void conHeader_deberiaReutilizarlo() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn("  req-abc-123  ");

        String mdc = captureMdcDuringChain();

        assertNotNull(mdc);
        assertEquals("req-abc-123", mdc);
        assertNull(MDC.get("correlationId"));
        verify(response).setHeader("X-Request-Id", "req-abc-123");
    }

    @Test
    void dosRequests_deberianTenerCorrelationIdsDistintos() throws Exception {
        when(request.getHeader("X-Request-Id")).thenReturn(null);

        String first = captureMdcDuringChain();
        String second = captureMdcDuringChain();

        assertNotEquals(first, second);
    }
}
