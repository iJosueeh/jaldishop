package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.store.application.command.ChangeStoreStatusCommand;

import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeStoreStatusService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AdminAuditService auditService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public ChangeStoreStatusService(StoreRepository storeRepository) {
        this(storeRepository, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ChangeStoreStatusService(
            StoreRepository storeRepository,
            UserRepository userRepository,
            AdminAuditService auditService,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public Store execute(ChangeStoreStatusCommand command) {
        Store store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada con ID: " + command.storeId()));

        if (command.targetStatus() == StoreStatus.SUSPENDED) {
            store.suspend();
        } else if (command.targetStatus() == StoreStatus.ACTIVE) {
            store.activate();
        } else {
            throw new IllegalArgumentException("Estado no soportado para cambio de estado de tienda: " + command.targetStatus());
        }

        Store saved = storeRepository.save(store);

        if (auditService != null) {
            String adminEmail = (command.adminId() != null && userRepository != null)
                    ? userRepository.findById(command.adminId()).map(User::getEmail).orElse(null)
                    : null;
            auditService.logSuccess(
                    command.adminId(),
                    adminEmail,
                    command.targetStatus() == StoreStatus.SUSPENDED ? "SUSPEND_STORE" : "ACTIVATE_STORE",
                    "STORE",
                    command.storeId(),
                    null,
                    "Estado actualizado a: " + command.targetStatus()
            );
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new com.jaldishop.backend.store.domain.event.StoreStatusChangedDomainEvent(
                    saved.getId(),
                    command.targetStatus().name(),
                    command.adminId()
            ));
        }

        return saved;
    }
}
