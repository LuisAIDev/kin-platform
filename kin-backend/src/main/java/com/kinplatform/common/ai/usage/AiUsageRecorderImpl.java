package com.kinplatform.common.ai.usage;

import com.kinplatform.common.usage.AiReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Implementación de {@link AiUsageRecorder} con atribución vía {@link ReservationContext}. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiUsageRecorderImpl implements AiUsageRecorder {

    private final ReservationContext reservationContext;
    private final AiBudgetControlService budgetControlService;

    @Override
    public void recordActual(long inputTokens, long outputTokens) {
        recordActual(reservationContext.current(), inputTokens, outputTokens);
    }

    @Override
    public void recordActual(AiReservation reservation, long inputTokens, long outputTokens) {
        if (reservation == null) {
            log.debug("Sin reserva para atribuir uso de IA (input={}, output={})", inputTokens, outputTokens);
            return;
        }
        budgetControlService.recordActual(reservation, inputTokens, outputTokens);
    }
}



