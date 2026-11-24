package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.port.StoreMediaCleaner;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AdminDeleteStoreService {

    private final StoreRepository storeRepository;
    private final StoreMediaCleaner storeMediaCleaner;
    private final AdminAuditService auditService;
    private final UserRepository userRepository;

    public AdminDeleteStoreService(StoreRepository storeRepository, StoreMediaCleaner storeMediaCleaner) {
        this(storeRepository, storeMediaCleaner, null, null);
    }

    @Autowired
    public AdminDeleteStoreService(
            StoreRepository storeRepository,
            StoreMediaCleaner storeMediaCleaner,
            AdminAuditService auditService,
            UserRepository userRepository
    ) {
        this.storeRepository = storeRepository;
        this.storeMediaCleaner = storeMediaCleaner;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    public void execute(UUID storeId, UUID adminId) {
        Store store = storeRepository.lockByIdForDeletion(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda", storeId.toString()));

        if (storeRepository.hasOperationalHistory(store.getId())) {
            throw new ResourceConflictException(
                    "STORE_HAS_OPERATIONAL_HISTORY",
                    "La tienda tiene pedidos, pagos, reservas o reseñas registradas y no puede eliminarse permanentemente. Puedes suspenderla para preservar el historial.",
                    Map.of(
                            "suggestedAction", "SUSPEND",
                            "currentStatus", store.getStatus().name(),
                            "blockingReason", "OPERATIONAL_HISTORY"
                    )
            );
        }

        String snapshotName = store.getName();
        String snapshotSlug = store.getSlug();
        UUID ownerUserId = store.getOwnerUserId();

        storeRepository.deleteStore(store.getId(), ownerUserId);
        if (storeMediaCleaner != null) {
            storeMediaCleaner.deleteStoreMedia(store.getId());
        }

        if (auditService != null) {
            String adminEmail = (adminId != null && userRepository != null)
                    ? userRepository.findById(adminId).map(User::getEmail).orElse(null)
                    : null;
            auditService.logSuccess(
                    adminId,
                    adminEmail,
                    "STORE_DELETED",
                    "STORE",
                    storeId,
                    null,
                    "Tienda eliminada permanentemente: " + snapshotName + " (" + snapshotSlug + ")"
            );
        }
    }
}
