package com.jaldishop.backend.identity.domain.model;

import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import com.jaldishop.backend.shared.exception.ConflictException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class AdminInvitation {

    private final UUID id;
    private final String email;
    private final String tokenHash;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final UUID invitedByUserId;
    private AdminInvitationStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant acceptedAt;
    private Instant revokedAt;

    public AdminInvitation(
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
        this.id = Objects.requireNonNull(id, "id no puede ser nulo");
        this.email = Objects.requireNonNull(email, "email no puede ser nulo").toLowerCase(Locale.ROOT).trim();
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash no puede ser nulo");
        this.firstName = Objects.requireNonNull(firstName, "firstName no puede ser nulo").trim();
        this.lastName = Objects.requireNonNull(lastName, "lastName no puede ser nulo").trim();
        this.phone = phone != null ? phone.trim() : null;
        this.invitedByUserId = Objects.requireNonNull(invitedByUserId, "invitedByUserId no puede ser nulo");
        this.status = Objects.requireNonNull(status, "status no puede ser nulo");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt no puede ser nulo");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt no puede ser nulo");
        this.acceptedAt = acceptedAt;
        this.revokedAt = revokedAt;
    }

    public static AdminInvitation create(
            String email,
            String tokenHash,
            String firstName,
            String lastName,
            String phone,
            UUID invitedByUserId,
            Instant expiresAt
    ) {
        Instant now = Instant.now();
        return new AdminInvitation(
                UUID.randomUUID(),
                email,
                tokenHash,
                firstName,
                lastName,
                phone,
                invitedByUserId,
                AdminInvitationStatus.PENDING,
                expiresAt,
                now,
                null,
                null
        );
    }

    public static AdminInvitation reconstitute(
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
        return new AdminInvitation(
                id,
                email,
                tokenHash,
                firstName,
                lastName,
                phone,
                invitedByUserId,
                status,
                expiresAt,
                createdAt,
                acceptedAt,
                revokedAt
        );
    }

    public boolean isPendingAndValid(Instant now) {
        return this.status == AdminInvitationStatus.PENDING && this.expiresAt.isAfter(now);
    }

    public void accept(Instant now) {
        if (this.status != AdminInvitationStatus.PENDING) {
            throw new ConflictException("INVITATION_NOT_PENDING", "La invitación ya no está pendiente.");
        }
        if (this.expiresAt.isBefore(now)) {
            this.status = AdminInvitationStatus.EXPIRED;
            throw new ConflictException("INVITATION_EXPIRED", "La invitación ha expirado.");
        }
        this.status = AdminInvitationStatus.ACCEPTED;
        this.acceptedAt = now;
    }

    public void revoke(Instant now) {
        if (this.status != AdminInvitationStatus.PENDING) {
            throw new ConflictException("INVITATION_NOT_PENDING", "Solo se pueden revocar invitaciones pendientes.");
        }
        this.status = AdminInvitationStatus.REVOKED;
        this.revokedAt = now;
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
