package com.jaldishop.backend.cart.application;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ClearCartService {

    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final CartViewAssembler cartViewAssembler;

    public ClearCartService(CartRepository cartRepository,
                            StoreRepository storeRepository,
                            CartViewAssembler cartViewAssembler) {
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.cartViewAssembler = cartViewAssembler;
    }

    public CartView execute(ClearCartCommand command) {
        validateStoreExists(command.storeId());

        Optional<Cart> cartOptional = cartRepository.findByUserIdAndStoreId(command.userId(), command.storeId());
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            cart.clear();
            Cart savedCart = cartRepository.save(cart);
            return cartViewAssembler.assemble(savedCart);
        }

        return cartViewAssembler.empty(command.userId(), command.storeId());
    }

    private void validateStoreExists(UUID storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("El ID de la tienda es obligatorio");
        }
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada"));
    }
}
