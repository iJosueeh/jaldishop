package com.jaldishop.backend.address.infrastructure.persistence.mapper;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.infrastructure.persistence.entity.CustomerAddressEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerAddressPersistenceMapper {

    public CustomerAddress toDomain(CustomerAddressEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CustomerAddress(
                entity.getId(),
                entity.getUserId(),
                entity.getAlias(),
                entity.getRecipientName(),
                entity.getRecipientPhone(),
                entity.getStreetAddress(),
                entity.getDepartment(),
                entity.getProvince(),
                entity.getDistrict(),
                entity.getUbigeoCode(),
                entity.getReference(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.isDefault(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CustomerAddressEntity toEntity(CustomerAddress domain) {
        if (domain == null) {
            return null;
        }
        return new CustomerAddressEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getAlias(),
                domain.getRecipientName(),
                domain.getRecipientPhone(),
                domain.getStreetAddress(),
                domain.getDepartment(),
                domain.getProvince(),
                domain.getDistrict(),
                domain.getUbigeoCode(),
                domain.getReference(),
                domain.getLatitude(),
                domain.getLongitude(),
                domain.isDefault(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
