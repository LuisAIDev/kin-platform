package com.kinplatform.institutional;

import com.kinplatform.user.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "organization_members",
        uniqueConstraints = @UniqueConstraint(name = "uq_org_member", columnNames = {"organization_id", "user_id"}),
        indexes = {
            @Index(name = "idx_org_members_org", columnList = "organization_id"),
            @Index(name = "idx_org_members_branch", columnList = "branch_id"),
            @Index(name = "idx_org_members_user", columnList = "user_id")
        })
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id")
    private UUID userId;

    /** Email del invitado cuando aun no tiene cuenta (user_id null hasta aceptar). */
    @Column(name = "invited_email", length = 255)
    private String invitedEmail;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "invited_at")
    private OffsetDateTime invitedAt;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = MemberStatus.INVITED;
        if (invitedAt == null) invitedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum MemberStatus {
        INVITED, ACTIVE, REMOVED
    }
}
