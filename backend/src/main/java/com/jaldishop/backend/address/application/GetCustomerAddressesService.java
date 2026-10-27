package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.address.web.dto.CustomerAddressListResponse;
import com.jaldishop.backend.address.web.dto.CustomerAddressResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class GetCustomerAddressesService {

    private static final int MAX_ALLOWED_ADDRESSES = 5;
    private final CustomerAddressRepository addressRepository;

    public GetCustomerAddressesService(CustomerAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public CustomerAddressListResponse execute(UUID userId) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");

        List<CustomerAddress> addresses = addressRepository.findByUserId(userId);
        List<CustomerAddressResponse> responses = addresses.stream()
                .map(this::toResponse)
                .toList();

        return new CustomerAddressListResponse(responses.size(), MAX_ALLOWED_ADDRESSES, responses);
    }

    private CustomerAddressResponse toResponse(CustomerAddress a) {
        return new CustomerAddressResponse(
                a.getId(),
                a.getAlias(),
                a.getRecipientName(),
                a.getRecipientPhone(),
                a.getStreetAddress(),
                a.getDepartment(),
                a.getProvince(),
                a.getDistrict(),
                a.getUbigeoCode(),
                a.getReference(),
                a.getLatitude(),
                a.getLongitude(),
                a.isDefault(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
