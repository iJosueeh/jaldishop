package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.RegisterMerchantCommand;
import com.jaldishop.backend.identity.application.result.AuthResult;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AdminCreateMerchantService {

    private final RegisterMerchantService registerMerchantService;
    private final UserRepository userRepository;
    private final AdminAuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AdminCreateMerchantService(
            RegisterMerchantService registerMerchantService,
            UserRepository userRepository,
            AdminAuditService auditService
    ) {
        this(registerMerchantService, userRepository, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminCreateMerchantService(
            RegisterMerchantService registerMerchantService,
            UserRepository userRepository,
            AdminAuditService auditService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.registerMerchantService = registerMerchantService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public AuthResult execute(RegisterMerchantCommand command, UUID adminId) {
        AuthResult result = registerMerchantService.execute(command);

        String adminEmail = (adminId != null)
                ? userRepository.findById(adminId).map(User::getEmail).orElse(null)
                : null;

        auditService.logSuccess(
                adminId,
                adminEmail,
                "ADMIN_CREATE_MERCHANT",
                "USER",
                result.userId(),
                null,
                "Comerciante y tienda inicial creados administrativamente para: " + command.email()
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.jaldishop.backend.identity.domain.event.UserRegisteredDomainEvent(
                    result.userId(),
                    command.email(),
                    "MERCHANT"
            ));
        }

        return result;
    }
}
