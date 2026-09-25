package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "patient_identification",
       indexes = {
           @Index(name = "idx_patient_identification_user_id", columnList = "user_id"),
           @Index(name = "idx_patient_identification_document", columnList = "document_type, document_number")
       })
public class PatientIdentification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 10)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(name = "document_expedition_date")
    private LocalDate documentExpeditionDate;

    @Column(name = "document_expedition_place", length = 100)
    private String documentExpeditionPlace;

    @Enumerated(EnumType.STRING)
    @Column(name = "rh_factor", length = 5)
    private RhFactor rhFactor;

    @Column(name = "eps_code", length = 20)
    private String epsCode;

    @Column(name = "eps_name", length = 200)
    private String epsName;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen", length = 20)
    private Regimen regimen;

    @Column(name = "guardian_name", length = 200)
    private String guardianName;

    @Enumerated(EnumType.STRING)
    @Column(name = "guardian_document_type", length = 10)
    private DocumentType guardianDocumentType;

    @Column(name = "guardian_document_number", length = 20)
    private String guardianDocumentNumber;

    @Column(name = "guardian_phone", length = 30)
    private String guardianPhone;

    @Column(name = "guardian_relationship", length = 50)
    private String guardianRelationship;

    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city_code", length = 10)
    private String cityCode;

    @Column(name = "department_code", length = 10)
    private String departmentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", length = 20)
    private Zone zone;

    @Column(name = "stratum")
    private Integer stratum;

    @Column(name = "email_institutional", length = 255)
    private String emailInstitutional;

    @Column(name = "phone_secondary", length = 30)
    private String phoneSecondary;

    @Column(name = "ethnicity", length = 50)
    private String ethnicity;

    @Column(name = "displacement_victim")
    @Builder.Default
    private Boolean displacementVictim = false;

    @Column(name = "disability_certificate", length = 50)
    private String disabilityCertificate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum DocumentType {
        CC, TI, CE, PP, RC
    }

    public enum RhFactor {
        A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG
    }

    public enum Regimen {
        CONTRIBUTIVO, SUBSIDIADO, ESPECIAL, EXCEPCION
    }

    public enum Zone {
        URBANO, RURAL
    }
}