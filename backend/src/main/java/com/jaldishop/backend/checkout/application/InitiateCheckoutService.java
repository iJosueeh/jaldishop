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
import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        if (command == null) {
            throw new IllegalArgumentException("El comando de inicio de checkout no puede ser nulo.");
        }
        if (command.userId() == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio para iniciar el checkout.");
        }
        if (command.storeId() == null) {
            throw new IllegalArgumentException("El ID de la tienda es obligatorio para iniciar el checkout.");
        }
        if (command.fulfillmentType() == null) {
            throw new BusinessRuleException("FULFILLMENT_TYPE_REQUIRED", "El tipo de entrega (PICKUP o DELIVERY) es obligatorio.");
        }
        if (command.serviceDate() == null) {
            throw new BusinessRuleException("SERVICE_DATE_REQUIRED", "La fecha de servicio es obligatoria.");
        }

        Store store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada."));

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");
        }

        validateFulfillment(command, store);

        Cart cart = cartRepository.findByUserIdAndStoreId(command.userId(), command.storeId())
                .orElseThrow(() -> new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío.");
        }

        List<CheckoutItemSnapshot> itemSnapshots = new ArrayList<>();
        String currency = "PEN";

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante de producto no encontrada: " + item.getVariantId()));

            if (variant.getStatus() != VariantStatus.ACTIVE) {
                throw new ConflictException("VARIANT_INACTIVE",
                        "La presentación '" + variant.getPresentationName() + "' no se encuentra disponible.");
            }

            Product product = productRepository.findById(variant.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto asociado a la variante no encontrado."));

            if (!product.getStoreId().equals(command.storeId())) {
                throw new ConflictException("VARIANT_STORE_MISMATCH",
                        "El producto '" + product.getName() + "' no pertenece a esta tienda.");
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new ConflictException("PRODUCT_INACTIVE",
                        "El producto '" + product.getName() + "' no se encuentra activo.");
            }

            currency = variant.getPriceCurrency() != null ? variant.getPriceCurrency() : "PEN";
            BigDecimal subtotal = variant.getPriceAmount().multiply(BigDecimal.valueOf(item.getQuantity()));

            itemSnapshots.add(new CheckoutItemSnapshot(
                    variant.getId(),
                    product.getId(),
                    product.getName(),
                    variant.getPresentationName(),
                    variant.getSku(),
                    product.getImageUrl(),
                    item.getQuantity(),
                    variant.getPriceAmount(),
                    currency,
                    subtotal,
                    variant.isTracksInventory()
            ));
        }

        CapacityReservation capacityReservation = createCapacityReservationService.execute(
                new CreateCapacityReservationCommand(
                        command.storeId(),
                        command.userId(),
                        command.serviceDate(),
                        command.startTime(),
                        command.endTime()
                )
        );

        CheckoutPricing pricing = calculatePricing(command.fulfillmentType(), store, itemSnapshots, currency);

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

    private void validateFulfillment(InitiateCheckoutCommand command, Store store) {
        if (command.fulfillmentType() == CheckoutFulfillmentType.DELIVERY) {
            if (!store.isDeliveryEnabled()) {
                throw new ConflictException("DELIVERY_NOT_AVAILABLE", "La tienda no realiza envíos a domicilio.");
            }
            if (command.deliveryAddress() == null || command.deliveryAddress().isBlank()) {
                throw new BusinessRuleException("DELIVERY_ADDRESS_REQUIRED", "La dirección de entrega es obligatoria para envíos a domicilio.");
            }
        } else if (command.fulfillmentType() == CheckoutFulfillmentType.PICKUP) {
            if (!store.isPickupEnabled()) {
                throw new ConflictException("PICKUP_NOT_AVAILABLE", "La tienda no permite retiro en tienda.");
            }
        }
    }

    private CheckoutPricing calculatePricing(
            CheckoutFulfillmentType fulfillmentType,
            Store store,
            List<CheckoutItemSnapshot> items,
            String currency
    ) {
        BigDecimal productsSubtotal = items.stream()
                .map(CheckoutItemSnapshot::subtotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (fulfillmentType == CheckoutFulfillmentType.DELIVERY && store.getDeliveryFeeAmount() != null) {
            deliveryFee = store.getDeliveryFeeAmount();
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountCode = null;

        BigDecimal taxRate = store.getTaxRate() != null ? store.getTaxRate() : new BigDecimal("18.00");
        BigDecimal includedTaxAmount = BigDecimal.ZERO;
        if (taxRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal denominator = BigDecimal.valueOf(100).add(taxRate);
            includedTaxAmount = productsSubtotal.multiply(taxRate).divide(denominator, 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = productsSubtotal.add(deliveryFee).subtract(discountAmount);

        return new CheckoutPricing(
                productsSubtotal,
                discountAmount,
                discountCode,
                deliveryFee,
                taxRate,
                includedTaxAmount,
                totalAmount,
                currency
        );
    }
}
