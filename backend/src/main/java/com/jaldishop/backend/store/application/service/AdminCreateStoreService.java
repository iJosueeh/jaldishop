package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.store.application.command.CreateStoreCommand;
import com.jaldishop.backend.store.domain.model.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AdminCreateStoreService {

    private final CreateStoreService createStoreService;
    private final UserRepository userRepository;
    private final AdminAuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AdminCreateStoreService(
            CreateStoreService createStoreService,
            UserRepository userRepository,
            AdminAuditService auditService
    ) {
        this(createStoreService, userRepository, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminCreateStoreService(
            CreateStoreService createStoreService,
            UserRepository userRepository,
            AdminAuditService auditService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.createStoreService = createStoreService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public Store execute(CreateStoreCommand command, UUID adminId) {
        User merchant = userRepository.findById(command.ownerUserId())
                .orElseThrow(() -> new com.jaldishop.backend.shared.exception.ResourceNotFoundException("Comerciante no encontrado con ID: " + command.ownerUserId()));

        if (merchant.getStatus() != com.jaldishop.backend.identity.domain.enums.UserStatus.ACTIVE) {
            throw new com.jaldishop.backend.shared.exception.ConflictException("MERCHANT_NOT_ACTIVE", "El comerciante debe estar en estado ACTIVE.");
        }

        Store store = createStoreService.execute(command);

        String adminEmail = (adminId != null)
                ? userRepository.findById(adminId).map(User::getEmail).orElse(null)
                : null;

        auditService.logSuccess(
                adminId,
                adminEmail,
                "ADMIN_CREATE_STORE",
                "STORE",
                store.getId(),
                null,
                "Tienda creada administrativamente: " + store.getName() + " para comerciante: " + command.ownerUserId()
        );

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.jaldishop.backend.store.domain.event.StoreCreatedDomainEvent(
                    store.getId(),
                    store.getOwnerUserId(),
                    store.getName(),
                    store.getSlug()
            ));
        }

        return store;
    }
}
