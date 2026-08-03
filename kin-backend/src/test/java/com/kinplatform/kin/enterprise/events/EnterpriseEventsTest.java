package com.kinplatform.kin.enterprise.events;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnterpriseEventsTest {

    @Test
    void requested_exponeContrato() {
        var projectId = UUID.randomUUID();
        var event = new EnterpriseProjectRequested(projectId, 2);

        assertEquals("ENTERPRISE_PROJECT_REQUESTED", event.type());
        assertEquals(projectId, event.aggregateId());
        assertEquals(projectId, event.projectId());
        assertEquals(2, event.version());
        assertEquals(event, new EnterpriseProjectRequested(projectId, 2));
        assertEquals(event.hashCode(), new EnterpriseProjectRequested(projectId, 2).hashCode());
        assertTrue(event.toString().contains("ENTERPRISE_PROJECT_REQUESTED")
            || event.toString().contains("EnterpriseProjectRequested"));
    }

    @Test
    void generated_exponeContrato() {
        var projectId = UUID.randomUUID();
        var event = new EnterpriseProjectGenerated(projectId, 3);

        assertEquals("ENTERPRISE_PROJECT_GENERATED", event.type());
        assertEquals(projectId, event.aggregateId());
        assertEquals(projectId, event.projectId());
        assertEquals(3, event.version());
        assertEquals(event, new EnterpriseProjectGenerated(projectId, 3));
        assertEquals(event.hashCode(), new EnterpriseProjectGenerated(projectId, 3).hashCode());
        assertNotEquals(event, new EnterpriseProjectGenerated(projectId, 4));
    }

    @Test
    void failed_exponeContrato() {
        var projectId = UUID.randomUUID();
        var event = new EnterpriseProjectFailed(projectId, 4, "motivo");

        assertEquals("ENTERPRISE_PROJECT_FAILED", event.type());
        assertEquals(projectId, event.aggregateId());
        assertEquals(projectId, event.projectId());
        assertEquals(4, event.version());
        assertEquals("motivo", event.reason());
        assertEquals(event, new EnterpriseProjectFailed(projectId, 4, "motivo"));
        assertEquals(event.hashCode(), new EnterpriseProjectFailed(projectId, 4, "motivo").hashCode());
        assertNotEquals(event, new EnterpriseProjectFailed(projectId, 4, "otro"));
    }
}
