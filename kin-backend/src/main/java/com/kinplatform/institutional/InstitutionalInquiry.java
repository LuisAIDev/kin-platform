package com.kinplatform.institutional;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "institutional_inquiries", indexes = {
    @Index(name = "idx_institutional_inquiries_status", columnList = "status"),
    @Index(name = "idx_institutional_inquiries_created", columnList = "created_at"),
    @Index(name = "idx_institutional_inquiries_email", columnList = "email")
})
public class InstitutionalInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ips_name", nullable = false, length = 200)
    private String ipsName;

    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "beds")
    private Integer beds;

    @Column(name = "comments", columnDefinition = "text")
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = InquiryStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum InquiryStatus {
        PENDING, CONTACTED, QUALIFIED, REJECTED
    }
}
