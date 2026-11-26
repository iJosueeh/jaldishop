package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.identity.application.port.StoreProvisioning;
import com.jaldishop.backend.identity.application.port.StoreProvisioningData;
import com.jaldishop.backend.store.application.command.CreateStoreCommand;
import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Implementación del puerto {@link StoreProvisioning} de {@code identity/application}.
 * Traduce los tipos simples de {@link StoreProvisioningData} hacia {@link CreateStoreCommand}
 * y delega en {@link CreateStoreService} dentro de la misma transacción.
 */
@Component
public class StoreProvisioningAdapter implements StoreProvisioning {

    private final CreateStoreService createStoreService;

    public StoreProvisioningAdapter(CreateStoreService createStoreService) {
        this.createStoreService = createStoreService;
    }

    @Override
    public UUID provisionStore(StoreProvisioningData data) {
        TaxIdType taxIdType = parseEnum(TaxIdType.class, data.taxIdType());
        TaxRegime taxRegime = parseEnum(TaxRegime.class, data.taxRegime());

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
                data.categoryIds(),
                data.legalBusinessName(),
                taxIdType,
                data.taxIdNumber(),
                taxRegime,
                data.canIssueInvoices(),
                data.sunatTermsAcceptedAt()
        );
        return createStoreService.execute(command).getId();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Valor inválido para " + enumClass.getSimpleName() + ": " + raw);
        }
    }
}
