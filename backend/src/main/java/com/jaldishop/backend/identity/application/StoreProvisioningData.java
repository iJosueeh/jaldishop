package com.jaldishop.backend.identity.application;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Espejo de los 20 campos de CreateStoreCommand usado en el registro de
 * comerciantes. Los valores por defecto del flujo (slug=null, deliveryFeeAmount=null,
 * "PEN", taxRate=18.00, instagramUrl=null, facebookUrl=null, whatsappNumber=contactPhone)
 * se calculan en RegisterMerchantService, igual que antes de la inversión de
 * dependencia; el adaptador en store/application solo mapea 1:1.
 */
public record StoreProvisioningData(
        UUID ownerUserId,
        String name,
        String slug,
        String description,
        String contactPhone,
        String address,
        String addressReference,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean pickupEnabled,
        boolean deliveryEnabled,
        BigDecimal deliveryFeeAmount,
        String deliveryFeeCurrency,
        BigDecimal taxRate,
        String logoUrl,
        String bannerUrl,
        String instagramUrl,
        String facebookUrl,
        String whatsappNumber,
        Set<UUID> categoryIds
) {
}
