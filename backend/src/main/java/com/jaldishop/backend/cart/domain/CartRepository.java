package com.jaldishop.backend.cart.domain;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findById(UUID id);

    Optional<Cart> findByUserIdAndStoreId(UUID userId, UUID storeId);

    void delete(Cart cart);

    void deleteById(UUID id);
}
