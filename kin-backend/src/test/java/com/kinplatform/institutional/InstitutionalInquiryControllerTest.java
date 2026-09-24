package com.kinplatform.institutional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InstitutionalInquiryControllerTest {

    @Mock
    private InstitutionalInquiryService service;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new InstitutionalInquiryController(service)).build();
    }

    @Test
    void create_returns201WithMessage() throws Exception {
        when(service.create(any())).thenReturn(InstitutionalInquiry.builder()
                .id(UUID.randomUUID())
                .status(InstitutionalInquiry.InquiryStatus.PENDING)
                .build());

        mockMvc().perform(post("/institutional/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ipsName\":\"Clinica Test\",\"nit\":\"900123456\","
                                + "\"contactName\":\"Ana\",\"email\":\"ana@clinica.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value(
                        "Hemos recibido tu solicitud. Te contactaremos en 48h para agendar una demo."));
    }

    @Test
    void create_withMissingRequiredFields_returns400() throws Exception {
        mockMvc().perform(post("/institutional/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Bogota\"}"))
                .andExpect(status().isBadRequest());
    }
}
