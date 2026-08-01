package com.kinplatform.kin.conversation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CommunicationModeTest {

    @Test
    void modes_deberiaContenerLosCuatroModos() {
        var values = CommunicationMode.values();
        assertEquals(4, values.length);
        assertSame(CommunicationMode.QUESTION, CommunicationMode.valueOf("QUESTION"));
        assertSame(CommunicationMode.EXPLAIN_REPORT, CommunicationMode.valueOf("EXPLAIN_REPORT"));
        assertSame(CommunicationMode.SUMMARY, CommunicationMode.valueOf("SUMMARY"));
        assertSame(CommunicationMode.FAREWELL, CommunicationMode.valueOf("FAREWELL"));
    }
}
