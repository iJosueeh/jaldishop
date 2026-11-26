package com.jaldishop.backend.store.infrastructure.persistence.mapper;

import com.jaldishop.backend.store.domain.model.DeliveryConfiguration;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreLocation;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCategoryEntity;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StorePersistenceMapper {

    public Store toDomain(StoreEntity entity) {
        if (entity == null) {
            return null;
        }
        Set<UUID> categoryIds;
        try {
            categoryIds = (entity.getCategories() != null)
                    ? entity.getCategories().stream().map(StoreCategoryEntity::getId).collect(Collectors.toSet())
                    : Collections.emptySet();
        } catch (Exception ignored) {
            categoryIds = Collections.emptySet();
        }

        StoreLocation location = StoreLocation.of(
                entity.getAddress(),
                entity.getAddressReference(),
                entity.getLatitude(),
                entity.getLongitude()
        );
        DeliveryConfiguration deliveryConfig = DeliveryConfiguration.of(
                entity.isPickupEnabled(),
                entity.isDeliveryEnabled(),
                entity.getDeliveryFeeAmount(),
                entity.getDeliveryFeeCurrency(),
                entity.getTaxRate()
        );

        com.jaldishop.backend.store.domain.enums.TaxIdType taxIdType = com.jaldishop.backend.store.domain.enums.TaxIdType.DNI;
        try {
            if (entity.getTaxIdType() != null) {
                taxIdType = com.jaldishop.backend.store.domain.enums.TaxIdType.valueOf(entity.getTaxIdType());
            }
        } catch (Exception ignored) {}

        com.jaldishop.backend.store.domain.enums.TaxRegime taxRegime = com.jaldishop.backend.store.domain.enums.TaxRegime.RUS;
        try {
            if (entity.getTaxRegime() != null) {
                taxRegime = com.jaldishop.backend.store.domain.enums.TaxRegime.valueOf(entity.getTaxRegime());
            }
        } catch (Exception ignored) {}

        com.jaldishop.backend.store.domain.model.StoreTaxProfile taxProfile = new com.jaldishop.backend.store.domain.model.StoreTaxProfile(
                entity.getLegalBusinessName(),
                taxIdType,
                entity.getTaxIdNumber(),
                taxRegime,
                entity.isCanIssueInvoices()
        );

        return Store.reconstitute(
                entity.getId(),
                entity.getOwnerUserId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getContactPhone(),
                location,
                deliveryConfig,
                entity.getLogoUrl(),
                entity.getBannerUrl(),
                entity.getInstagramUrl(),
                entity.getFacebookUrl(),
                entity.getWhatsappNumber(),
                categoryIds,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                taxProfile,
                entity.getSunatTermsAcceptedAt()
        );
    }

    public StoreEntity toEntity(Store domain) {
        if (domain == null) {
            return null;
        }
        StoreEntity entity = new StoreEntity(
                domain.getId(),
                domain.getOwnerUserId(),
                domain.getName(),
                domain.getSlug(),
                domain.getDescription(),
                domain.getContactPhone(),
                domain.getAddress(),
                domain.getAddressReference(),
                domain.getLatitude(),
                domain.getLongitude(),
                domain.isPickupEnabled(),
                domain.isDeliveryEnabled(),
                domain.getDeliveryFeeAmount(),
                domain.getDeliveryFeeCurrency(),
                domain.getTaxRate(),
                domain.getLogoUrl(),
                domain.getBannerUrl(),
                domain.getInstagramUrl(),
                domain.getFacebookUrl(),
                domain.getWhatsappNumber(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
        entity.setLegalBusinessName(domain.getLegalBusinessName());
        entity.setTaxIdType(domain.getTaxIdType() != null ? domain.getTaxIdType().name() : "DNI");
        entity.setTaxIdNumber(domain.getTaxIdNumber());
        entity.setTaxRegime(domain.getTaxRegime() != null ? domain.getTaxRegime().name() : "RUS");
        entity.setCanIssueInvoices(domain.isCanIssueInvoices());
        entity.setSunatTermsAcceptedAt(domain.getSunatTermsAcceptedAt());
        return entity;
    }

}
