package com.jaldishop.backend.cart.application.service;
import com.jaldishop.backend.cart.domain.model.*;
import com.jaldishop.backend.cart.domain.repository.*;
import com.jaldishop.backend.cart.application.command.*;
import com.jaldishop.backend.cart.application.query.*;
import com.jaldishop.backend.cart.application.result.*;


import com.jaldishop.backend.cart.domain.model.Cart;
import com.jaldishop.backend.cart.domain.repository.CartRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetCartService {

    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final CartViewAssembler cartViewAssembler;

    public GetCartService(CartRepository cartRepository,
                          StoreRepository storeRepository,
                          CartViewAssembler cartViewAssembler) {
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.cartViewAssembler = cartViewAssembler;
    }

    public CartView execute(GetCartQuery query) {
        validateStoreExists(query.storeId());
        Optional<Cart> cartOptional = cartRepository.findByUserIdAndStoreId(query.userId(), query.storeId());
        return cartOptional.map(cartViewAssembler::assemble)
                .orElseGet(() -> cartViewAssembler.empty(query.userId(), query.storeId()));
    }

    private void validateStoreExists(UUID storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("El ID de la tienda es obligatorio");
        }
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada"));
    }
}
