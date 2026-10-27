package com.jaldishop.backend.address.infrastructure.persistence.adapter;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.address.infrastructure.persistence.entity.CustomerAddressEntity;
import com.jaldishop.backend.address.infrastructure.persistence.mapper.CustomerAddressPersistenceMapper;
import com.jaldishop.backend.address.infrastructure.persistence.repository.CustomerAddressJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCustomerAddressRepositoryAdapter implements CustomerAddressRepository {

    private final CustomerAddressJpaRepository jpaRepository;
    private final CustomerAddressPersistenceMapper mapper;

    public JpaCustomerAddressRepositoryAdapter(CustomerAddressJpaRepository jpaRepository,
                                              CustomerAddressPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CustomerAddress save(CustomerAddress address) {
        CustomerAddressEntity entity = mapper.toEntity(address);
        CustomerAddressEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CustomerAddress> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public List<CustomerAddress> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CustomerAddress> findDefaultByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndIsDefaultTrue(userId).map(mapper::toDomain);
    }

    @Override
    public long countByUserId(UUID userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public void delete(CustomerAddress address) {
        jpaRepository.deleteById(address.getId());
    }

    @Override
    public void clearDefaultForUser(UUID userId) {
        jpaRepository.clearDefaultForUser(userId);
    }

    @Override
    public Optional<CustomerAddress> findMostRecentlyUpdated(UUID userId) {
        return jpaRepository.findMostRecentlyUpdated(userId).map(mapper::toDomain);
    }

    @Override
    public void lockUserForUpdate(UUID userId) {
        jpaRepository.lockUserForUpdate(userId);
    }
}
