package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.application.command.CloseMyStoreCommand;
import com.jaldishop.backend.store.application.result.CloseStoreResult;
import com.jaldishop.backend.store.domain.enums.CloseStoreAction;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.port.StoreMediaCleaner;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CloseMyStoreService {

    private final StoreRepository storeRepository;
    private final StoreMediaCleaner storeMediaCleaner;

    public CloseMyStoreService(StoreRepository storeRepository, StoreMediaCleaner storeMediaCleaner) {
        this.storeRepository = storeRepository;
        this.storeMediaCleaner = storeMediaCleaner;
    }

    public CloseStoreResult execute(CloseMyStoreCommand command) {
        Store store = storeRepository.findByMerchantUserId(command.merchantUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna tienda asociada a tu cuenta de comerciante."));

        Store lockedStore = storeRepository.lockByIdForDeletion(store.getId())
                .orElse(store);

        boolean hasHistory = storeRepository.hasOperationalHistory(lockedStore.getId());

        if (hasHistory) {
            throw new ResourceConflictException(
                    "STORE_HAS_OPERATIONAL_HISTORY",
                    "Tu tienda tiene pedidos, pagos, reservas o reseñas registradas y no puede eliminarse permanentemente. Puedes desactivarla para ocultarla al público preservando el historial.",
                    Map.of(
                            "suggestedAction", "DEACTIVATE",
                            "currentStatus", lockedStore.getStatus().name(),
                            "blockingReason", "OPERATIONAL_HISTORY"
                    )
            );
        }

        storeRepository.deleteStore(lockedStore.getId(), command.merchantUserId());
        try {
            storeMediaCleaner.deleteStoreMedia(lockedStore.getId());
        } catch (Exception ignored) {
            // Aislamiento: si Cloudinary tiene latencia o error, la transacción de BD ya está confirmada
        }
        return new CloseStoreResult(
                lockedStore.getId(),
                CloseStoreAction.DELETED,
                null,
                "Tu tienda y los datos de tu cuenta comercial han sido eliminados permanentemente de la plataforma."
        );
    }

    public CloseStoreResult deactivate(UUID merchantUserId) {
        Store store = storeRepository.findByMerchantUserId(merchantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna tienda asociada a tu cuenta de comerciante."));

        if (store.getStatus() == StoreStatus.INACTIVE) {
            throw new ConflictException("STORE_ALREADY_INACTIVE", "La tienda ya se encuentra inactiva.");
        }
        if (store.getStatus() == StoreStatus.SUSPENDED || store.getStatus() == StoreStatus.CLOSED) {
            throw new ConflictException("STORE_STATUS_LOCKED", "No es posible desactivar una tienda en estado " + store.getStatus().name() + ".");
        }

        store.deactivate();
        Store saved = storeRepository.save(store);
        return new CloseStoreResult(
                saved.getId(),
                CloseStoreAction.DEACTIVATED,
                saved.getStatus(),
                "Tu tienda ha sido desactivada. Permanecerá oculta al público y tu historial operativo sigue intacto."
        );
    }

    public Store reactivate(UUID merchantUserId) {
        Store store = storeRepository.findByMerchantUserId(merchantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna tienda asociada a tu cuenta de comerciante."));

        if (store.getStatus() == StoreStatus.SUSPENDED) {
            throw new AccessDeniedException("Tu tienda fue suspendida por administración y no puede reactivarse desde tu cuenta.");
        }
        if (store.getStatus() == StoreStatus.CLOSED) {
            throw new ConflictException("STORE_CLOSED", "Una tienda clausurada no puede reactivarse.");
        }

        store.activate();
        return storeRepository.save(store);
    }
}
