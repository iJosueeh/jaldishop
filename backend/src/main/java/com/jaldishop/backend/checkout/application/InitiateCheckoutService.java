package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.capacity.application.CreateCapacityReservationCommand;
import com.jaldishop.backend.capacity.application.CreateCapacityReservationService;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartItem;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentValidator;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.CheckoutPricing;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InitiateCheckoutService {

    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CreateCapacityReservationService createCapacityReservationService;

    public InitiateCheckoutService(
            CartRepository cartRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            CreateCapacityReservationService createCapacityReservationService
    ) {
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.createCapacityReservationService = createCapacityReservationService;
    }

    public CheckoutResult execute(InitiateCheckoutCommand command) {
        validateCommandPrerequisites(command);

        Store store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada."));

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");
        }

        CheckoutFulfillmentValidator.validate(command.fulfillmentType(), command.deliveryAddress(), store);

        Cart cart = cartRepository.findByUserIdAndStoreId(command.userId(), command.storeId())
                .orElseThrow(() -> new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío.");
        }

        List<CheckoutItemSnapshot> itemSnapshots = snapshotAndValidateItems(cart, command.storeId());
        String currency = itemSnapshots.isEmpty() ? "PEN" : itemSnapshots.get(0).currency();

        CapacityReservation capacityReservation = createCapacityReservationService.execute(
                new CreateCapacityReservationCommand(
                        command.storeId(),
                        command.userId(),
                        command.serviceDate(),
                        command.startTime(),
                        command.endTime()
                )
        );

        CheckoutPricing pricing = CheckoutPricing.calculate(
                itemSnapshots,
                command.fulfillmentType(),
                store.getDeliveryFeeAmount(),
                store.getTaxRate(),
                currency
        );

        return new CheckoutResult(
                capacityReservation.getId(),
                capacityReservation.getExpiresAt(),
                store.getId(),
                store.getName(),
                command.userId(),
                command.fulfillmentType(),
                command.serviceDate(),
                command.startTime(),
                command.endTime(),
                command.customerName(),
                command.customerPhone(),
                command.customerEmail(),
                command.deliveryAddress(),
                command.deliveryReference(),
                itemSnapshots,
                pricing,
                Instant.now()
        );
    }

    private void validateCommandPrerequisites(InitiateCheckoutCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando de inicio de checkout no puede ser nulo.");
        }
        if (command.userId() == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio para iniciar el checkout.");
        }
        if (command.storeId() == null) {
            throw new IllegalArgumentException("El ID de la tienda es obligatorio para iniciar el checkout.");
        }
        if (command.serviceDate() == null) {
            throw new BusinessRuleException("SERVICE_DATE_REQUIRED", "La fecha de servicio es obligatoria.");
        }
    }

    private List<CheckoutItemSnapshot> snapshotAndValidateItems(Cart cart, UUID storeId) {
        List<CheckoutItemSnapshot> snapshots = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante de producto no encontrada: " + item.getVariantId()));

            if (variant.getStatus() != VariantStatus.ACTIVE) {
                throw new ConflictException("VARIANT_INACTIVE",
                        "La presentación '" + variant.getPresentationName() + "' no se encuentra disponible.");
            }

            Product product = productRepository.findById(variant.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto asociado a la variante no encontrado."));

            if (!product.getStoreId().equals(storeId)) {
                throw new ConflictException("VARIANT_STORE_MISMATCH",
                        "El producto '" + product.getName() + "' no pertenece a esta tienda.");
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new ConflictException("PRODUCT_INACTIVE",
                        "El producto '" + product.getName() + "' no se encuentra activo.");
            }

            String itemCurrency = variant.getPriceCurrency() != null ? variant.getPriceCurrency() : "PEN";
            BigDecimal subtotal = variant.getPriceAmount().multiply(BigDecimal.valueOf(item.getQuantity()));

            snapshots.add(new CheckoutItemSnapshot(
                    variant.getId(),
                    product.getId(),
                    product.getName(),
                    variant.getPresentationName(),
                    variant.getSku(),
                    product.getImageUrl(),
                    item.getQuantity(),
                    variant.getPriceAmount(),
                    itemCurrency,
                    subtotal,
                    variant.isTracksInventory()
            ));
        }

        return snapshots;
    }
}
