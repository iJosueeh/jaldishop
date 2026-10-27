package com.jaldishop.backend.address.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository {

    CustomerAddress save(CustomerAddress address);

    Optional<CustomerAddress> findByIdAndUserId(UUID id, UUID userId);

    List<CustomerAddress> findByUserId(UUID userId);

    Optional<CustomerAddress> findDefaultByUserId(UUID userId);

    long countByUserId(UUID userId);

    void delete(CustomerAddress address);

    void clearDefaultForUser(UUID userId);

    Optional<CustomerAddress> findMostRecentlyUpdated(UUID userId);

    void lockUserForUpdate(UUID userId);
}
