package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.AcceptAdminInvitationCommand;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.AdminInvitationRepository;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.AdminInvitationTokens;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@Transactional
public class AcceptAdminInvitationService {

    private final AdminInvitationRepository invitationRepository;
    private final AdminInvitationTokens tokenService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AcceptAdminInvitationService(
            AdminInvitationRepository invitationRepository,
            AdminInvitationTokens tokenService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AdminAuditService auditService
    ) {
        this(invitationRepository, tokenService, userRepository, roleRepository, passwordEncoder, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AcceptAdminInvitationService(
            AdminInvitationRepository invitationRepository,
            AdminInvitationTokens tokenService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AdminAuditService auditService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.invitationRepository = invitationRepository;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public User execute(AcceptAdminInvitationCommand command) {
        String tokenHash = tokenService.hash(command.token());
        AdminInvitation invitation = invitationRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new ConflictException(
                        "INVALID_OR_EXPIRED_INVITATION",
                        "El token de invitación es inválido o no existe."
                ));

        Instant now = Instant.now();
        invitation.accept(now);
        invitationRepository.save(invitation);

        if (userRepository.findByEmail(invitation.getEmail()).isPresent()) {
            throw new ConflictException("USER_ALREADY_EXISTS", "Ya existe una cuenta activa con este correo.");
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Rol ADMIN no encontrado."));

        String encodedPassword = passwordEncoder.encode(command.newPassword());
        User newUser = User.create(
                invitation.getEmail(),
                encodedPassword,
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getPhone(),
                Set.of(adminRole)
        );

        User savedUser = userRepository.save(newUser);

        auditService.logSuccess(
                savedUser.getId(),
                savedUser.getEmail(),
                "ACCEPT_ADMIN_INVITATION",
                "USER",
                savedUser.getId(),
                null,
                "Invitación aceptada exitosamente. Usuario ADMIN creado."
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.jaldishop.backend.identity.domain.event.AdminInvitationAcceptedDomainEvent(
                    savedUser.getId(),
                    savedUser.getEmail(),
                    invitation.getInvitedByUserId()
            ));
        }

        return savedUser;
    }
}
