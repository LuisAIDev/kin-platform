package com.kinplatform.billing.fev;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DianStubClientTest {

    private final DianStubClient client = new DianStubClient();

    @Test
    void send_returnsAcceptedResponse() {
        FevRipsInvoice invoice = FevRipsInvoice.builder()
                .id(java.util.UUID.randomUUID())
                .dianCufe("CUFE-TEST-123")
                .build();

        DianClient.DianResponse response = client.send(invoice);

        assertTrue(response.accepted());
        assertEquals("CUFE-TEST-123", response.cufe());
        assertEquals("00", response.statusCode());
        assertTrue(response.responseXml().contains("CUFE-TEST-123"));
    }

    @Test
    void send_generatesCufeWhenMissing() {
        FevRipsInvoice invoice = FevRipsInvoice.builder()
                .id(java.util.UUID.randomUUID())
                .build();

        DianClient.DianResponse response = client.send(invoice);

        assertTrue(response.accepted());
        assertEquals("STUB-CUFE", response.cufe());
    }

    @Test
    void queryStatus_returnsAccepted() {
        DianClient.DianStatusResponse response = client.queryStatus("CUFE-TEST-123");

        assertEquals("CUFE-TEST-123", response.cufe());
        assertEquals("ACCEPTED", response.status());
    }
}
