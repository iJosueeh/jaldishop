package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.InviteAdminCommand;
import com.jaldishop.backend.identity.application.port.AdminInvitationMail;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.AdminInvitationRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.AdminInvitationTokens;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@Transactional
public class InviteAdminService {

    private final UserRepository userRepository;
    private final AdminInvitationRepository invitationRepository;
    private final AdminInvitationTokens tokenService;
    private final AdminInvitationMail mailService;
    private final AdminAuditService auditService;

    public InviteAdminService(
            UserRepository userRepository,
            AdminInvitationRepository invitationRepository,
            AdminInvitationTokens tokenService,
            AdminInvitationMail mailService,
            AdminAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.auditService = auditService;
    }

    public AdminInvitation execute(InviteAdminCommand command) {
        if (command.currentAdminId() == null) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }

        User actor = userRepository.findById(command.currentAdminId())
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado."));

        if (!actor.isPrimaryAdmin()) {
            throw new AccessDeniedException("Solo el administrador principal puede invitar nuevos administradores.");
        }

        String normalizedEmail = command.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException(
                    "USER_ALREADY_EXISTS",
                    "Ya existe un usuario registrado con este correo. Si desea que sea administrador, utilice la opción de promoción."
            );
        }

        Instant now = Instant.now();

        // Si ya existe una invitación PENDING, se revoca para permitir la re-invitación de forma limpia
        invitationRepository.findPendingByEmail(normalizedEmail).ifPresent(pending -> {
            pending.revoke(now);
            invitationRepository.save(pending);
        });

        String rawToken = tokenService.generate();
        String tokenHash = tokenService.hash(rawToken);
        Instant expiresAt = now.plus(Duration.ofHours(48));

        AdminInvitation invitation = AdminInvitation.create(
                normalizedEmail,
                tokenHash,
                command.firstName(),
                command.lastName(),
                command.phone(),
                actor.getId(),
                expiresAt
        );

        AdminInvitation saved = invitationRepository.save(invitation);

        mailService.sendInvitation(
                saved.getEmail(),
                saved.getFirstName() + " " + saved.getLastName(),
                rawToken,
                saved.getExpiresAt()
        );

        auditService.logSuccess(
                actor.getId(),
                actor.getEmail(),
                "INVITE_ADMIN",
                "ADMIN_INVITATION",
                saved.getId(),
                null,
                "Invitación enviada a: " + saved.getEmail()
        );

        return saved;
    }
}
