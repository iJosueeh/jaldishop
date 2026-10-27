package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.address.infrastructure.ubigeo.UbigeoCatalogService;
import com.jaldishop.backend.address.web.dto.CreateCustomerAddressRequest;
import com.jaldishop.backend.address.web.dto.CustomerAddressResponse;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class CreateCustomerAddressService {

    private static final int MAX_ADDRESSES_PER_CUSTOMER = 5;

    private final CustomerAddressRepository addressRepository;
    private final UbigeoCatalogService ubigeoCatalogService;

    public CreateCustomerAddressService(CustomerAddressRepository addressRepository,
                                        UbigeoCatalogService ubigeoCatalogService) {
        this.addressRepository = addressRepository;
        this.ubigeoCatalogService = ubigeoCatalogService;
    }

    @Transactional
    public CustomerAddressResponse execute(UUID userId, CreateCustomerAddressRequest request) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        Objects.requireNonNull(request, "Los datos de la dirección son obligatorios.");

        // Bloqueo pesimista del usuario para serializar altas concurrentes y garantizar el límite estricto
        addressRepository.lockUserForUpdate(userId);

        long currentCount = addressRepository.countByUserId(userId);
        if (currentCount >= MAX_ADDRESSES_PER_CUSTOMER) {
            throw new BusinessRuleException(
                    "ADDRESS_LIMIT_EXCEEDED",
                    "Has alcanzado el límite máximo de " + MAX_ADDRESSES_PER_CUSTOMER + " direcciones guardadas."
            );
        }

        // Validación estricta de procedencia e integridad del código UBIGEO contra catálogo oficial INEI
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

        // Si es la primera dirección del usuario, debe ser predeterminada
        boolean makeDefault = currentCount == 0 || request.isDefault();

        if (makeDefault && currentCount > 0) {
            addressRepository.clearDefaultForUser(userId);
        }

        CustomerAddress address = CustomerAddress.create(
                userId,
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
                makeDefault
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
