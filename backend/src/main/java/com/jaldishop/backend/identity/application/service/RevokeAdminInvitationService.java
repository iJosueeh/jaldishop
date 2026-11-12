package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.AdminInvitationRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class RevokeAdminInvitationService {

    private final UserRepository userRepository;
    private final AdminInvitationRepository invitationRepository;
    private final AdminAuditService auditService;

    public RevokeAdminInvitationService(
            UserRepository userRepository,
            AdminInvitationRepository invitationRepository,
            AdminAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.auditService = auditService;
    }

    public void execute(UUID invitationId, UUID currentAdminId) {
        if (currentAdminId == null) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }

        User actor = userRepository.findById(currentAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado."));

        if (!actor.isPrimaryAdmin()) {
            throw new AccessDeniedException("Solo el administrador principal puede revocar invitaciones.");
        }

        AdminInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitación no encontrada con ID: " + invitationId));

        invitation.revoke(Instant.now());
        invitationRepository.save(invitation);

        auditService.logSuccess(
                actor.getId(),
                actor.getEmail(),
                "REVOKE_ADMIN_INVITATION",
                "ADMIN_INVITATION",
                invitation.getId(),
                null,
                "Invitación revocada para: " + invitation.getEmail()
        );
    }
}
