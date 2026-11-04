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

import java.util.UUID;

@Service
@Transactional
public class UpdateCartItemQuantityService {

    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final CartViewAssembler cartViewAssembler;

    public UpdateCartItemQuantityService(CartRepository cartRepository,
                                         StoreRepository storeRepository,
                                         CartViewAssembler cartViewAssembler) {
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.cartViewAssembler = cartViewAssembler;
    }

    public CartView execute(UpdateCartItemQuantityCommand command) {
        validateStoreExists(command.storeId());

        Cart cart = cartRepository.findByUserIdAndStoreId(command.userId(), command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        if (cart.findItemByVariantId(command.variantId()).isEmpty()) {
            throw new ResourceNotFoundException("El producto no se encuentra en el carrito");
        }

        cart.updateItemQuantity(command.variantId(), command.quantity());
        Cart savedCart = cartRepository.save(cart);
        return cartViewAssembler.assemble(savedCart);
    }

    private void validateStoreExists(UUID storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("El ID de la tienda es obligatorio");
        }
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada"));
    }
}
