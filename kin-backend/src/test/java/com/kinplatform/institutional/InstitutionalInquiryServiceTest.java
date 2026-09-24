package com.kinplatform.institutional;

import com.kinplatform.auth.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InstitutionalInquiryServiceTest {

    private InstitutionalInquiryRepository repository;
    private EmailSender emailSender;
    private InstitutionalInquiryService service;

    @BeforeEach
    void setUp() {
        repository = mock(InstitutionalInquiryRepository.class);
        emailSender = mock(EmailSender.class);
        service = new InstitutionalInquiryService(repository, emailSender);
        ReflectionTestUtils.setField(service, "notificationEmail", "contacto@kin-platform.com");
    }

    @Test
    void create_savesInquiryAndSendsBothEmails() {
        when(repository.save(any(InstitutionalInquiry.class))).thenAnswer(inv -> {
            InstitutionalInquiry i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            i.setStatus(InstitutionalInquiry.InquiryStatus.PENDING);
            return i;
        });

        InstitutionalInquiry saved = service.create(new InstitutionalInquiryRequest(
                "Clinica Test", "900123456", "Bogota", "Ana", "ana@clinica.com", "3001234567", 50, "Interesados"));

        assertNotNull(saved.getId());
        assertEquals(InstitutionalInquiry.InquiryStatus.PENDING, saved.getStatus());
        verify(repository).save(any(InstitutionalInquiry.class));
        verify(emailSender).sendInstitutionalInquiryAutoReply(eq("ana@clinica.com"), eq("Ana"));
        verify(emailSender).sendInstitutionalInquiryNotification(eq("contacto@kin-platform.com"), anyString());
    }

    @Test
    void create_normalizesEmailToLowerCase() {
        when(repository.save(any(InstitutionalInquiry.class))).thenAnswer(inv -> inv.getArgument(0));

        InstitutionalInquiry saved = service.create(new InstitutionalInquiryRequest(
                "Clinica", "900", null, "Ana", "ANA@CLINICA.COM", null, null, null));

        assertEquals("ana@clinica.com", saved.getEmail());
    }

    @Test
    void create_emailFailureDoesNotBreakPersistence() {
        when(repository.save(any(InstitutionalInquiry.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("SMTP down"))
                .when(emailSender).sendInstitutionalInquiryAutoReply(anyString(), anyString());

        InstitutionalInquiry saved = service.create(new InstitutionalInquiryRequest(
                "Clinica", "900", "Cali", "Ana", "ana@clinica.com", null, null, null));

        assertNotNull(saved);
        verify(repository).save(any(InstitutionalInquiry.class));
    }

    @Test
    void findAll_delegatesToRepository() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        assertTrue(service.findAll().isEmpty());
    }
}
