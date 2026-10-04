package com.jaldishop.backend.store.application;

import com.jaldishop.backend.media.domain.MediaStorageService;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.CloseStoreAction;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CloseMyStoreService {

    private final StoreRepository storeRepository;
    private final MediaStorageService mediaStorageService;

    public CloseMyStoreService(StoreRepository storeRepository, MediaStorageService mediaStorageService) {
        this.storeRepository = storeRepository;
        this.mediaStorageService = mediaStorageService;
    }

    public CloseStoreResult execute(CloseMyStoreCommand command) {
        Store store = storeRepository.findByMerchantUserId(command.merchantUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ninguna tienda asociada a tu cuenta de comerciante."));

        boolean hasHistory = storeRepository.hasOperationalHistory(store.getId());

        if (hasHistory) {
            store.deactivate();
            storeRepository.save(store);
            return new CloseStoreResult(
                    store.getId(),
                    CloseStoreAction.DEACTIVATED,
                    StoreStatus.INACTIVE,
                    "Tu tienda tiene pedidos o reservas registradas y ha sido desactivada para preservar la integridad transaccional."
            );
        } else {
            storeRepository.deleteStore(store.getId(), command.merchantUserId());
            try {
                mediaStorageService.deleteStoreMedia(store.getId());
            } catch (Exception ignored) {
                // Aislamiento: si Cloudinary tiene latencia o error, la transacción de BD ya está confirmada
            }
            return new CloseStoreResult(
                    store.getId(),
                    CloseStoreAction.DELETED,
                    null,
                    "Tu tienda no registraba operaciones previas y ha sido eliminada permanentemente de la plataforma."
            );
        }
    }
}
