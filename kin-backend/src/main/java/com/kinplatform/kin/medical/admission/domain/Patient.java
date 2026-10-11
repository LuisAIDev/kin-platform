package com.kinplatform.kin.medical.admission.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "patients",
       indexes = {
           @Index(name = "idx_patients_document", columnList = "document_type, document_number", unique = true),
           @Index(name = "idx_patients_names", columnList = "last_name, second_last_name, first_name"),
           @Index(name = "idx_patients_birth_date", columnList = "birth_date"),
           @Index(name = "idx_patients_deleted_at", columnList = "deleted_at")
       })
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 10)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "second_last_name", length = 100)
    private String secondLastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, columnDefinition = "varchar(1)")
    private Gender gender;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "eps", length = 200)
    private String eps;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime", length = 20)
    private Regime regime;

    @Column(name = "rh_factor", length = 5)
    private String rhFactor;

    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    public enum DocumentType {
        CC, CE, TI, RC, PA, MS, AS, PE, PT, SC, CD, DE, SI, NV
    }

    public enum Gender {
        M, F, O, I
    }

    public enum Regime {
        CONTRIBUTIVO, SUBSIDIADO, VINCULADO, ESPECIAL
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (middleName != null && !middleName.isBlank()) sb.append(" ").append(middleName);
        if (lastName != null) sb.append(" ").append(lastName);
        if (secondLastName != null && !secondLastName.isBlank()) sb.append(" ").append(secondLastName);
        return sb.toString().trim();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}