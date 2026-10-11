package com.kinplatform.kin.medical.admission.api;

import com.kinplatform.kin.medical.admission.domain.Patient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        Patient.DocumentType documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        String fullName,
        LocalDate birthDate,
        Patient.Gender gender,
        String phone,
        String email,
        String address,
        String city,
        String department,
        String eps,
        Patient.Regime regime,
        String rhFactor,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelationship,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        Instant deletedAt,
        String deletionReason,
        boolean deleted
) {
    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getDocumentType(),
                patient.getDocumentNumber(),
                patient.getFirstName(),
                patient.getMiddleName(),
                patient.getLastName(),
                patient.getSecondLastName(),
                patient.getFullName(),
                patient.getBirthDate(),
                patient.getGender(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getCity(),
                patient.getDepartment(),
                patient.getEps(),
                patient.getRegime(),
                patient.getRhFactor(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                patient.getEmergencyContactRelationship(),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.getCreatedBy(),
                patient.getDeletedAt(),
                patient.getDeletionReason(),
                patient.isDeleted()
        );
    }
}