package com.kinplatform.institutional;

import com.kinplatform.auth.email.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstitutionalInquiryService {

    private final InstitutionalInquiryRepository repository;
    private final EmailSender emailSender;

    @Value("${app.institutional.notification-email:contacto@kin-platform.com}")
    private String notificationEmail;

    @Transactional
    public InstitutionalInquiry create(InstitutionalInquiryRequest request) {
        InstitutionalInquiry inquiry = InstitutionalInquiry.builder()
                .ipsName(trim(request.ipsName()))
                .nit(trim(request.nit()))
                .city(trimToNull(request.city()))
                .contactName(trim(request.contactName()))
                .email(trim(request.email()).toLowerCase())
                .phone(trimToNull(request.phone()))
                .beds(request.beds())
                .comments(trimToNull(request.comments()))
                .status(InstitutionalInquiry.InquiryStatus.PENDING)
                .build();

        InstitutionalInquiry saved = repository.save(inquiry);
        sendAutoReply(saved);
        sendInternalNotification(saved);
        return saved;
    }

    public List<InstitutionalInquiry> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<InstitutionalInquiry> findByStatus(InstitutionalInquiry.InquiryStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    private void sendAutoReply(InstitutionalInquiry inquiry) {
        try {
            emailSender.sendInstitutionalInquiryAutoReply(inquiry.getEmail(), inquiry.getContactName());
        } catch (Exception e) {
            log.error("No se pudo enviar la respuesta automatica al solicitante {}: {}",
                    inquiry.getEmail(), e.getMessage());
        }
    }

    private void sendInternalNotification(InstitutionalInquiry inquiry) {
        try {
            emailSender.sendInstitutionalInquiryNotification(notificationEmail, summary(inquiry));
        } catch (Exception e) {
            log.error("No se pudo enviar la notificacion interna de solicitud {}: {}",
                    inquiry.getId(), e.getMessage());
        }
    }

    private String summary(InstitutionalInquiry i) {
        return "Nueva solicitud de acceso Beta (IPS)\n\n"
                + "IPS: " + i.getIpsName() + "\n"
                + "NIT: " + i.getNit() + "\n"
                + "Ciudad: " + nullToDash(i.getCity()) + "\n"
                + "Contacto: " + i.getContactName() + "\n"
                + "Email: " + i.getEmail() + "\n"
                + "Telefono: " + nullToDash(i.getPhone()) + "\n"
                + "Camas: " + (i.getBeds() == null ? "-" : i.getBeds()) + "\n"
                + "Comentarios: " + nullToDash(i.getComments()) + "\n"
                + "ID: " + i.getId();
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
