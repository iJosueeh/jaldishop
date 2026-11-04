package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.store.application.command.CreateStoreCommand;
import com.jaldishop.backend.store.domain.model.StoreCategory;
import com.jaldishop.backend.store.domain.repository.StoreCategoryRepository;

import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
@Transactional
public class CreateStoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public CreateStoreService(StoreRepository storeRepository, UserRepository userRepository) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    public Store execute(CreateStoreCommand command) {
        if (storeRepository.existsByOwnerUserId(command.ownerUserId())) {
            throw new ConflictException("MERCHANT_ALREADY_HAS_STORE", "El comerciante ya tiene una tienda registrada.");
        }

        String resolveSlug = resolveSlug(command.name(), command.slug());
        if (storeRepository.existsBySlug(resolveSlug)) {
            throw new ConflictException("STORE_SLUG_ALREADY_EXISTS", "El slug de la tienda ya está en uso");
        }

        Store store = Store.create(
                command.ownerUserId(),
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
                command.taxRate(),
                command.logoUrl(),
                command.bannerUrl(),
                command.instagramUrl(),
                command.facebookUrl(),
                command.whatsappNumber(),
                command.categoryIds()
        );

        Store savedStore = storeRepository.save(store);
        userRepository.assignStoreToMerchantRole(command.ownerUserId(), savedStore.getId());
        return savedStore;
    }

    private String resolveSlug(String name, String slug) {
        String source = (slug != null && !slug.isBlank()) ? slug : name;
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("El slug no puede ser nulo ni vacio.");
        }
        String decomposed = Normalizer.normalize(source.trim(), Normalizer.Form.NFD);
        String stripped = decomposed.replaceAll("\\p{M}", "");
        String resolved = stripped.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (resolved.isBlank()) {
            throw new IllegalArgumentException("El slug no puede ser nulo ni vacio.");
        }
        return resolved;
    }

}
