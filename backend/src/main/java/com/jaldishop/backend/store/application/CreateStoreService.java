package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class CreateStoreService {

    private final StoreRepository storeRepository;

    public CreateStoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store execute(CreateStoreCommand command) {
        if (storeRepository.existsByMerchantUserId(command.merchantUserId())) {
            throw new ConflictException("MERCHANT_ALREADY_HAS_STORE", "El comerciante ya tiene una tienda registrada.");
        }

        String resolveSlug = resolveSlug(command.name(), command.slug());
        if (storeRepository.existsBySlug(resolveSlug)) {
            throw new ConflictException("STORE_SLUG_ALREADY_EXISTS", "El slug de la tienda ya está en uso");
        }

        Store store = Store.create(
                command.merchantUserId(),
                command.name(),
                resolveSlug,
                command.description(),
                command.contactPhone(),
                command.address(),
                command.addressReference(),
                command.latitude(),
                command.longitude(),
                command.pickupEnabled(),
                command.deliveryEnabled(),
                command.deliveryFeeAmount(),
                command.deliveryFeeCurrency(),
                command.taxApplies(),
                command.taxRate()
        );

        return storeRepository.save(store);
    }

    private String resolveSlug(String name, String slug) {
        String source = (slug != null && !slug.isBlank()) ? slug : name;
        return source.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

}
