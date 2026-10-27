package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.address.infrastructure.ubigeo.UbigeoCatalogService;
import com.jaldishop.backend.address.web.dto.CustomerAddressResponse;
import com.jaldishop.backend.address.web.dto.UpdateCustomerAddressRequest;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class UpdateCustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final UbigeoCatalogService ubigeoCatalogService;

    public UpdateCustomerAddressService(CustomerAddressRepository addressRepository,
                                        UbigeoCatalogService ubigeoCatalogService) {
        this.addressRepository = addressRepository;
        this.ubigeoCatalogService = ubigeoCatalogService;
    }

    @Transactional
    public CustomerAddressResponse execute(UUID userId, UUID addressId, UpdateCustomerAddressRequest request) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        Objects.requireNonNull(addressId, "El identificador de la dirección es obligatorio.");
        Objects.requireNonNull(request, "Los datos de actualización son obligatorios.");

        addressRepository.lockUserForUpdate(userId);

        CustomerAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección", addressId));

        boolean validUbigeo = ubigeoCatalogService.isValidHierarchy(
                request.ubigeoCode(),
                request.department(),
                request.province(),
                request.district()
        );
        if (!validUbigeo) {
            throw new BusinessRuleException(
                    "INVALID_UBIGEO",
                    "El código UBIGEO '" + request.ubigeoCode() + "' no corresponde al departamento '"
                            + request.department() + "', provincia '" + request.province()
                            + "' y distrito '" + request.district() + "' en el catálogo oficial de INEI."
            );
        }

        long count = addressRepository.countByUserId(userId);

        // Si es la única dirección, debe permanecer como predeterminada
        boolean targetDefault = request.isDefault();
        if (count == 1) {
            targetDefault = true;
        } else if (targetDefault && !address.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        } else if (!targetDefault && address.isDefault()) {
            // No se puede desmarcar la predeterminada sin designar otra; se promueve la más reciente
            addressRepository.findMostRecentlyUpdated(userId)
                    .filter(other -> !other.getId().equals(address.getId()))
                    .ifPresent(other -> {
                        other.markAsDefault();
                        addressRepository.save(other);
                    });
        }

        address.update(
                request.alias(),
                request.recipientName(),
                request.recipientPhone(),
                request.streetAddress(),
                request.department(),
                request.province(),
                request.district(),
                request.ubigeoCode(),
                request.reference(),
                request.latitude(),
                request.longitude(),
                targetDefault
        );

        CustomerAddress saved = addressRepository.save(address);

        return new CustomerAddressResponse(
                saved.getId(),
                saved.getAlias(),
                saved.getRecipientName(),
                saved.getRecipientPhone(),
                saved.getStreetAddress(),
                saved.getDepartment(),
                saved.getProvince(),
                saved.getDistrict(),
                saved.getUbigeoCode(),
                saved.getReference(),
                saved.getLatitude(),
                saved.getLongitude(),
                saved.isDefault(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
