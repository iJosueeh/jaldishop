package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.AdminCreateUserCommand;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class AdminCreateUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AdminCreateUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AdminAuditService auditService
    ) {
        this(userRepository, roleRepository, passwordEncoder, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminCreateUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AdminAuditService auditService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public User execute(AdminCreateUserCommand command) {
        String normalizedEmail = command.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("USER_ALREADY_EXISTS", "El correo ya está registrado en el sistema.");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Rol CUSTOMER no encontrado."));

        String encodedPassword = passwordEncoder.encode(command.password());

        User newUser = User.create(
                normalizedEmail,
                encodedPassword,
                command.firstName(),
                command.lastName(),
                command.phone(),
                Set.of(customerRole)
        );

        User saved = userRepository.save(newUser);

        String actorEmail = command.currentAdminId() != null
                ? userRepository.findById(command.currentAdminId()).map(User::getEmail).orElse(null)
                : null;

        auditService.logSuccess(
                command.currentAdminId(),
                actorEmail,
                "ADMIN_CREATE_CUSTOMER",
                "USER",
                saved.getId(),
                null,
                "Cliente creado desde panel admin: " + saved.getEmail()
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.jaldishop.backend.identity.domain.event.UserRegisteredDomainEvent(
                    saved.getId(),
                    saved.getEmail(),
                    "CUSTOMER"
            ));
        }

        return saved;
    }
}
