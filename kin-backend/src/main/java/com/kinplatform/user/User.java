package com.kinplatform.user;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.UserSubscription;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "platform", nullable = false, length = 20)
    @Builder.Default
    private String platform = "EMPRESAS";

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer credits = 10;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    /** Fecha de nacimiento del paciente (auto-registro vertical Salud). */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Sexo del paciente (auto-registro vertical Salud). */
    @Column(name = "sex", length = 20)
    private String sex;

    /** Teléfono de contacto. */
    @Column(name = "phone", length = 30)
    private String phone;

    /** Opt-in para recibir avisos por WhatsApp de sus pacientes. */
    @Column(name = "whatsapp_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean whatsappNotificationsEnabled = false;

    /** Número de cédula profesional del médico (validado por ADMIN). */
    @Column(name = "license_number", length = 60)
    private String licenseNumber;

    /** Especialidad médica declarada en el auto-registro. */
    @Column(name = "specialty", length = 100)
    private String specialty;

    /** País del médico (auto-registro). */
    @Column(name = "country", length = 60)
    private String country;

    /**
     * Estado de verificación del médico. {@code null} = no sujeto a revisión
     * (equivalente a APROBADO para médicos del piloto/administrador).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "physician_verification_status", length = 20)
    private PhysicianVerificationStatus physicianVerificationStatus;

    /**
     * Consentimiento explícito para el tratamiento de datos de salud. Requerido
     * en el auto-registro de la vertical Salud.
     */
    @Column(name = "health_data_consent", nullable = false)
    @Builder.Default
    private Boolean healthDataConsent = false;

    /** Acceso ilimitado (fundador/VIP): sin limite de triajes ni paywall de exportacion. */
    @Column(name = "unlimited_access", nullable = false)
    @Builder.Default
    private Boolean unlimitedAccess = false;

    /** Proyectos COMPLETADOS en el período vigente (persistente; no decrece al eliminar). */
    @Column(name = "completed_projects", nullable = false)
    @Builder.Default
    private Integer completedProjects = 0;

    /** Inicio del período al que corresponde {@code completedProjects}. */
    @Column(name = "completed_projects_period_start")
    private OffsetDateTime completedProjectsPeriodStart;

    @ToString.Exclude
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserSubscription subscription;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_plan_id")
    private PricingPlan currentPlan;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "preferred_payment_gateway", length = 20)
    @Builder.Default
    private String preferredPaymentGateway = "STRIPE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
