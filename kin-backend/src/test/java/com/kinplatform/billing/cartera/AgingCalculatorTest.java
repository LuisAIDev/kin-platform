package com.kinplatform.billing.cartera;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AgingCalculatorTest {

    private final AgingCalculator calculator = new AgingCalculator(null);

    @Test
    void daysOverdue_returnsZeroWhenNotDue() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        assertEquals(0, calculator.daysOverdue(today.plusDays(10), today));
        assertEquals(0, calculator.daysOverdue(today, today));
    }

    @Test
    void daysOverdue_returnsElapsedDays() {
        LocalDate today = LocalDate.of(2026, 1, 15);
        assertEquals(40, calculator.daysOverdue(today.minusDays(40), today));
    }

    @Test
    void bucket_mapsRanges() {
        assertEquals(AgingCalculator.CURRENT, calculator.bucket(0));
        assertEquals(AgingCalculator.CURRENT, calculator.bucket(-5));
        assertEquals(AgingCalculator.BUCKET_1_30, calculator.bucket(1));
        assertEquals(AgingCalculator.BUCKET_1_30, calculator.bucket(30));
        assertEquals(AgingCalculator.BUCKET_31_60, calculator.bucket(31));
        assertEquals(AgingCalculator.BUCKET_61_90, calculator.bucket(90));
        assertEquals(AgingCalculator.BUCKET_91_180, calculator.bucket(120));
        assertEquals(AgingCalculator.BUCKET_180_PLUS, calculator.bucket(181));
    }

    @Test
    void recalculate_setsOverdueWhenPastDue() {
        AccountsReceivable ar = AccountsReceivable.builder()
                .dueDate(LocalDate.now().minusDays(40))
                .totalValueCop(new BigDecimal("100000"))
                .paidValueCop(BigDecimal.ZERO)
                .build();

        calculator.recalculate(ar, LocalDate.now());

        assertEquals(40, ar.getDaysOverdue());
        assertEquals(AgingCalculator.BUCKET_31_60, ar.getAgingBucket());
        assertEquals(AccountsReceivable.ArStatus.OVERDUE, ar.getStatus());
    }

    @Test
    void recalculate_marksPaidWhenNoPending() {
        AccountsReceivable ar = AccountsReceivable.builder()
                .dueDate(LocalDate.now().minusDays(40))
                .totalValueCop(new BigDecimal("100000"))
                .paidValueCop(new BigDecimal("100000"))
                .build();

        calculator.recalculate(ar, LocalDate.now());

        assertEquals(AccountsReceivable.ArStatus.PAID, ar.getStatus());
    }

    @Test
    void recalculate_marksPartialWhenPartiallyPaidAndNotOverdue() {
        AccountsReceivable ar = AccountsReceivable.builder()
                .dueDate(LocalDate.now().plusDays(5))
                .totalValueCop(new BigDecimal("100000"))
                .paidValueCop(new BigDecimal("40000"))
                .build();

        calculator.recalculate(ar, LocalDate.now());

        assertEquals(AccountsReceivable.ArStatus.PARTIAL, ar.getStatus());
        assertEquals(AgingCalculator.CURRENT, ar.getAgingBucket());
    }
}
