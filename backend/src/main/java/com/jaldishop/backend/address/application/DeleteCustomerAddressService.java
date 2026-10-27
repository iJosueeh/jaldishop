package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class DeleteCustomerAddressService {

    private final CustomerAddressRepository addressRepository;

    public DeleteCustomerAddressService(CustomerAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional
    public void execute(UUID userId, UUID addressId) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        Objects.requireNonNull(addressId, "El identificador de la dirección es obligatorio.");

        addressRepository.lockUserForUpdate(userId);

        CustomerAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección", addressId));

        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        // Si la dirección eliminada era la predeterminada y restan otras, promovemos la más reciente
        if (wasDefault) {
            addressRepository.findMostRecentlyUpdated(userId).ifPresent(remaining -> {
                remaining.markAsDefault();
                addressRepository.save(remaining);
            });
        }
    }
}
