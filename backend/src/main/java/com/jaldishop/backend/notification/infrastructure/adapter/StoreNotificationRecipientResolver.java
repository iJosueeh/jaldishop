package com.jaldishop.backend.notification.infrastructure.adapter;

import com.jaldishop.backend.notification.domain.port.NotificationRecipientResolver;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de infraestructura para resolver el usuario comerciante dueño de una tienda
 * delegando la consulta a {@link StoreRepository}.
 */
@Component
public class StoreNotificationRecipientResolver implements NotificationRecipientResolver {

    private final StoreRepository storeRepository;

    public StoreNotificationRecipientResolver(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Override
    public Optional<UUID> resolveMerchantUserId(UUID storeId) {
        if (storeId == null) {
            return Optional.empty();
        }
        return storeRepository.findById(storeId).map(Store::getOwnerUserId);
    }

}
