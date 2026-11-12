package com.jaldishop.backend.identity.infrastructure.persistence.entity;

import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_invitations")
public class AdminInvitationEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "invited_by_user_id", nullable = false)
    private UUID invitedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminInvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AdminInvitationEntity() {}

    public AdminInvitationEntity(
            UUID id,
            String email,
            String tokenHash,
            String firstName,
            String lastName,
            String phone,
            UUID invitedByUserId,
            AdminInvitationStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant acceptedAt,
            Instant revokedAt
    ) {
        this.id = id;
        this.email = email;
        this.tokenHash = tokenHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.invitedByUserId = invitedByUserId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.acceptedAt = acceptedAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getTokenHash() { return tokenHash; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public UUID getInvitedByUserId() { return invitedByUserId; }
    public AdminInvitationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
