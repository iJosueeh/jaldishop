package com.jaldishop.backend.store.application;

import com.jaldishop.backend.identity.application.StoreProvisioning;
import com.jaldishop.backend.identity.application.StoreProvisioningData;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación del puerto {@link StoreProvisioning} de {@code identity/application}.
 * Adaptador burdo: solo mapea los 20 campos 1:1 hacia {@link CreateStoreCommand} y
 * delega en {@link CreateStoreService} (validaciones, defaults y resolución de slug
 * permanecen allí, intactos). Sin {@link org.springframework.transaction.annotation.Transactional}:
 * no abre transacción propia, por lo que se une a la transacción de
 * RegisterMerchantService y el rollback del registro sigue siendo atómico.
 */
@Component
public class StoreProvisioningAdapter implements StoreProvisioning {

    private final CreateStoreService createStoreService;

    public StoreProvisioningAdapter(CreateStoreService createStoreService) {
        this.createStoreService = createStoreService;
    }

    @Override
    public UUID provisionStore(StoreProvisioningData data) {
        CreateStoreCommand command = new CreateStoreCommand(
                data.ownerUserId(),
                data.name(),
                data.slug(),
                data.description(),
                data.contactPhone(),
                data.address(),
                data.addressReference(),
                data.latitude(),
                data.longitude(),
                data.pickupEnabled(),
                data.deliveryEnabled(),
                data.deliveryFeeAmount(),
                data.deliveryFeeCurrency(),
                data.taxRate(),
                data.logoUrl(),
                data.bannerUrl(),
                data.instagramUrl(),
                data.facebookUrl(),
                data.whatsappNumber(),
                data.categoryIds()
        );
        return createStoreService.execute(command).getId();
    }
}
