package com.kinplatform.kin.health.hce.entity;

import jakarta.persistence.*;
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
@Table(name = "medical_orders",
       indexes = {
           @Index(name = "idx_medical_orders_treatment_plan_id", columnList = "treatment_plan_id"),
           @Index(name = "idx_medical_orders_encounter_id", columnList = "encounter_id"),
           @Index(name = "idx_medical_orders_patient_id", columnList = "patient_id"),
           @Index(name = "idx_medical_orders_type_status", columnList = "order_type, status"),
           @Index(name = "idx_medical_orders_physician_id", columnList = "physician_id")
       })
public class MedicalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "treatment_plan_id")
    private UUID treatmentPlanId;

    @Column(name = "encounter_id", nullable = false)
    private UUID encounterId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "physician_id", nullable = false)
    private UUID physicianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 30)
    private OrderType orderType;

    @Column(name = "drug_name", length = 200)
    private String drugName;

    @Column(name = "dose", length = 100)
    private String dose;

    @Column(name = "dose_unit", length = 50)
    private String doseUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "route", length = 50)
    private Route route;

    @Column(name = "frequency", length = 100)
    private String frequency;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "cups_code", length = 20)
    private String cupsCode;

    @Column(name = "cups_description", length = 500)
    private String cupsDescription;

    @Column(name = "body_site", length = 100)
    private String bodySite;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.ROUTINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.ORDERED;

    @Column(name = "instructions", length = 2000)
    private String instructions;

    @Column(name = "ordered_at", nullable = false)
    @Builder.Default
    private Instant orderedAt = Instant.now();

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "executed_by")
    private UUID executedBy;

    @Column(name = "execution_notes", length = 2000)
    private String executionNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum OrderType {
        MEDICATION, PROCEDURE, LAB_EXAM, IMAGING, DIET, NURSING_CARE, OTHER
    }

    public enum Route {
        ORAL, IV, IM, SC, TOPICAL, INHALATION, SUBLINGUAL, RECTAL, TRANSDERMAL, OTHER
    }

    public enum Priority {
        STAT, URGENT, ROUTINE, SCHEDULED
    }

    public enum Status {
        ORDERED, IN_PROGRESS, EXECUTED, PARTIALLY_EXECUTED, SUSPENDED, CANCELLED, ON_HOLD
    }
}