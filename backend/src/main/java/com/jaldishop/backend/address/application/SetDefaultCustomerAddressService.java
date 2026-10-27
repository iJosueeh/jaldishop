package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.address.web.dto.CustomerAddressResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class SetDefaultCustomerAddressService {

    private final CustomerAddressRepository addressRepository;

    public SetDefaultCustomerAddressService(CustomerAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional
    public CustomerAddressResponse execute(UUID userId, UUID addressId) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        Objects.requireNonNull(addressId, "El identificador de la dirección es obligatorio.");

        addressRepository.lockUserForUpdate(userId);

        CustomerAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección", addressId));

        if (!address.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
            address.markAsDefault();
            address = addressRepository.save(address);
        }

        return new CustomerAddressResponse(
                address.getId(),
                address.getAlias(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getStreetAddress(),
                address.getDepartment(),
                address.getProvince(),
                address.getDistrict(),
                address.getUbigeoCode(),
                address.getReference(),
                address.getLatitude(),
                address.getLongitude(),
                address.isDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
