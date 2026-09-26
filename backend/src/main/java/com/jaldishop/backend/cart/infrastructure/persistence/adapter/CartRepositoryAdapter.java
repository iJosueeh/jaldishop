package com.jaldishop.backend.cart.infrastructure.persistence.adapter;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.jaldishop.backend.cart.infrastructure.persistence.mapper.CartPersistenceMapper;
import com.jaldishop.backend.cart.infrastructure.persistence.repository.CartJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CartRepositoryAdapter implements CartRepository {

    private final CartJpaRepository cartJpaRepository;
    private final CartPersistenceMapper mapper;

    public CartRepositoryAdapter(CartJpaRepository cartJpaRepository, CartPersistenceMapper mapper) {
        this.cartJpaRepository = cartJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Cart save(Cart cart) {
        CartEntity entity = mapper.toEntity(cart);
        CartEntity saved = cartJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Cart> findById(UUID id) {
        return cartJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Cart> findByUserIdAndStoreId(UUID userId, UUID storeId) {
        return cartJpaRepository.findByUserIdAndStoreId(userId, storeId).map(mapper::toDomain);
    }

    @Override
    public void delete(Cart cart) {
        if (cart != null && cart.getId() != null) {
            cartJpaRepository.deleteById(cart.getId());
        }
    }

    @Override
    public void deleteById(UUID id) {
        cartJpaRepository.deleteById(id);
    }
}
