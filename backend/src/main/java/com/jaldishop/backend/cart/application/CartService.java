package com.jaldishop.backend.cart.application;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final CartViewAssembler cartViewAssembler;

    public CartService(CartRepository cartRepository,
                       StoreRepository storeRepository,
                       ProductVariantRepository productVariantRepository,
                       ProductRepository productRepository,
                       CartViewAssembler cartViewAssembler) {
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
        this.cartViewAssembler = cartViewAssembler;
    }

    @Transactional(readOnly = true)
    public CartView getCart(UUID userId, UUID storeId) {
        validateStoreExists(storeId);
        Optional<Cart> cartOptional = cartRepository.findByUserIdAndStoreId(userId, storeId);
        return cartOptional.map(cartViewAssembler::assemble)
                .orElseGet(() -> cartViewAssembler.empty(userId, storeId));
    }

    @Transactional
    public CartView addItemToCart(UUID userId, AddItemToCartCommand command) {
        if (command.quantity() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        validateStoreExists(command.storeId());

        ProductVariant variant = productVariantRepository.findById(command.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));

        if (variant.getStatus() != VariantStatus.ACTIVE) {
            throw new ConflictException("VARIANT_INACTIVE", "La variante de producto no se encuentra disponible");
        }

        Product product = productRepository.findById(variant.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto asociado a la variante no encontrado"));

        if (!product.getStoreId().equals(command.storeId())) {
            throw new ConflictException("VARIANT_STORE_MISMATCH", "El producto no pertenece a la tienda especificada");
        }

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ConflictException("PRODUCT_INACTIVE", "El producto no se encuentra activo");
        }

        Cart cart = cartRepository.findByUserIdAndStoreId(userId, command.storeId())
                .orElseGet(() -> Cart.create(userId, command.storeId()));

        cart.addItem(
                variant.getId(),
                command.quantity(),
                variant.getPriceAmount(),
                variant.getPriceCurrency()
        );

        Cart savedCart = cartRepository.save(cart);
        return cartViewAssembler.assemble(savedCart);
    }

    @Transactional
    public CartView updateCartItemQuantity(UUID userId, UpdateCartItemQuantityCommand command) {
        validateStoreExists(command.storeId());

        Cart cart = cartRepository.findByUserIdAndStoreId(userId, command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        if (cart.findItemByVariantId(command.variantId()).isEmpty()) {
            throw new ResourceNotFoundException("El producto no se encuentra en el carrito");
        }

        cart.updateItemQuantity(command.variantId(), command.quantity());
        Cart savedCart = cartRepository.save(cart);
        return cartViewAssembler.assemble(savedCart);
    }

    @Transactional
    public CartView removeCartItem(UUID userId, UUID storeId, UUID variantId) {
        validateStoreExists(storeId);

        Cart cart = cartRepository.findByUserIdAndStoreId(userId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        cart.removeItem(variantId);
        Cart savedCart = cartRepository.save(cart);
        return cartViewAssembler.assemble(savedCart);
    }

    @Transactional
    public CartView clearCart(UUID userId, UUID storeId) {
        validateStoreExists(storeId);

        Optional<Cart> cartOptional = cartRepository.findByUserIdAndStoreId(userId, storeId);
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            cart.clear();
            Cart savedCart = cartRepository.save(cart);
            return cartViewAssembler.assemble(savedCart);
        }

        return cartViewAssembler.empty(userId, storeId);
    }

    private void validateStoreExists(UUID storeId) {
        if (storeId == null) {
            throw new IllegalArgumentException("El ID de la tienda es obligatorio");
        }
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada"));
    }
}
