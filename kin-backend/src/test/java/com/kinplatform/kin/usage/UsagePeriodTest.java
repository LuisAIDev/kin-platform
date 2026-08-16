package com.kinplatform.kin.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class UsagePeriodTest {

    @Test
    void current_iniciaElPrimeroDelMes() {
        var period = UsagePeriod.current();
        assertEquals(1, period.start().getDayOfMonth());
        assertEquals(period.start().plusMonths(1), period.end());
    }

    @Test
    void contains_dentroDelPeriodo() {
        var period = UsagePeriod.current();
        assertTrue(period.contains(OffsetDateTime.now()));
    }

    @Test
    void contains_fueraDelPeriodo() {
        var period = UsagePeriod.current();
        assertFalse(period.contains(period.end().plusSeconds(1)));
        assertFalse(period.contains(period.start().minusSeconds(1)));
    }

    @Test
    void isAfter_masReciente() {
        var period = UsagePeriod.current();
        assertTrue(period.isAfter(null));
        assertFalse(period.isAfter(period));
        assertTrue(period.isAfter(new UsagePeriod(period.start().minusMonths(1), period.start())));
    }
}
