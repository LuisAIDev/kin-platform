package com.kinplatform.billing.fev;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FevRipsControllerTest {

    @Mock
    private FevRipsService fevRipsService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new FevRipsController(fevRipsService)).build();
    }

    @Test
    void generate_returnsCreatedInvoice() throws Exception {
        UUID batchId = UUID.randomUUID();
        FevRipsInvoice invoice = invoice(FevRipsInvoice.InvoiceStatus.DRAFT);
        when(fevRipsService.generateInvoiceFromBatch(batchId)).thenReturn(invoice);

        mockMvc().perform(post("/billing/fev-rips/generate/{batchId}", batchId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("FEV0000000001"));
    }

    @Test
    void sign_returnsSignedInvoice() throws Exception {
        UUID id = UUID.randomUUID();
        FevRipsInvoice invoice = invoice(FevRipsInvoice.InvoiceStatus.SIGNED);
        when(fevRipsService.sign(id)).thenReturn(invoice);

        mockMvc().perform(post("/billing/fev-rips/{id}/sign", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"));
    }

    @Test
    void send_returnsAcceptedInvoice() throws Exception {
        UUID id = UUID.randomUUID();
        FevRipsInvoice invoice = invoice(FevRipsInvoice.InvoiceStatus.ACCEPTED);
        when(fevRipsService.sendToDian(id)).thenReturn(invoice);

        mockMvc().perform(post("/billing/fev-rips/{id}/send", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void status_returnsInvoice() throws Exception {
        UUID id = UUID.randomUUID();
        FevRipsInvoice invoice = invoice(FevRipsInvoice.InvoiceStatus.CONTINGENCY);
        when(fevRipsService.getStatus(id)).thenReturn(invoice);

        mockMvc().perform(get("/billing/fev-rips/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTINGENCY"));
    }

    @Test
    void contingency_returnsInvoice() throws Exception {
        UUID id = UUID.randomUUID();
        FevRipsInvoice invoice = invoice(FevRipsInvoice.InvoiceStatus.CONTINGENCY);
        when(fevRipsService.enterContingency(eq(id), anyString())).thenReturn(invoice);

        mockMvc().perform(post("/billing/fev-rips/{id}/contingency", id)
                        .param("reason", "DIAN caida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTINGENCY"));
    }

    private FevRipsInvoice invoice(FevRipsInvoice.InvoiceStatus status) {
        return FevRipsInvoice.builder()
                .id(UUID.randomUUID())
                .invoicePrefix("FEV")
                .invoiceSequence(1L)
                .invoiceNumber("FEV0000000001")
                .status(status)
                .build();
    }
}
