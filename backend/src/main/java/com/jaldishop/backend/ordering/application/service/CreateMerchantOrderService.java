package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductJpaRepository;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductVariantJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.ordering.application.result.MerchantOrderDetailResponse;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.web.dto.CreateMerchantOrderItemRequest;
import com.jaldishop.backend.ordering.web.dto.CreateMerchantOrderRequest;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreateMerchantOrderService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final ProductVariantJpaRepository productVariantJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final InventoryJpaRepository inventoryJpaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateMerchantOrderService(
            OrderRepository orderRepository,
            StoreRepository storeRepository,
            ProductVariantJpaRepository productVariantJpaRepository,
            ProductJpaRepository productJpaRepository,
            InventoryJpaRepository inventoryJpaRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.productVariantJpaRepository = productVariantJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.inventoryJpaRepository = inventoryJpaRepository;
        this.eventPublisher = eventPublisher;
    }

    public MerchantOrderDetailResponse execute(UUID storeId, UUID merchantUserId, CreateMerchantOrderRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "Tienda no encontrada: " + storeId));
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");
        }

        String orderNumber = formatOrderNumber(orderRepository.nextOrderNumber());
        List<Order.ItemData> itemsData = new ArrayList<>();
        BigDecimal productsSubtotal = BigDecimal.ZERO;
        String currency = "PEN";

        for (CreateMerchantOrderItemRequest itemReq : request.items()) {
            ProductVariantEntity variant = productVariantJpaRepository.findByIdAndStoreIdForUpdate(itemReq.variantId(), storeId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "VARIANT_NOT_FOUND",
                            "La variante con ID " + itemReq.variantId() + " no existe o no pertenece a la tienda."
                    ));

            if (!"ACTIVE".equals(variant.getStatus())) {
                throw new ConflictException("VARIANT_INACTIVE", "La variante no se encuentra activa: " + variant.getPresentationName());
            }

            if (variant.isTracksInventory()) {
                Instant now = Instant.now();
                int otherReserved = inventoryJpaRepository.sumActiveReservedQuantity(storeId, variant.getId(), now);
                int updated = inventoryJpaRepository.decrementStockConditionally(variant.getId(), itemReq.quantity(), otherReserved, now);
                if (updated == 0) {
                    throw new ConflictException("INSUFFICIENT_STOCK", "Stock insuficiente para la variante: " + variant.getPresentationName());
                }
            }

            String productName = productJpaRepository.findById(variant.getProductId())
                    .map(ProductEntity::getName)
                    .orElse("Producto");

            BigDecimal unitPrice = itemReq.unitPriceAmount() != null && itemReq.unitPriceAmount().compareTo(BigDecimal.ZERO) >= 0
                    ? itemReq.unitPriceAmount()
                    : variant.getPriceAmount();

            if (variant.getPriceCurrency() != null && !variant.getPriceCurrency().isBlank()) {
                currency = variant.getPriceCurrency();
            }

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
            productsSubtotal = productsSubtotal.add(itemSubtotal);

            itemsData.add(new Order.ItemData(
                    variant.getId(),
                    productName,
                    variant.getPresentationName(),
                    itemReq.quantity(),
                    unitPrice
            ));
        }

        BigDecimal deliveryFee = request.deliveryMode() == DeliveryMode.DELIVERY && request.deliveryFeeAmount() != null
                ? request.deliveryFeeAmount()
                : BigDecimal.ZERO;

        BigDecimal totalAmount = productsSubtotal.add(deliveryFee);

        Order.MerchantDirectData directData = new Order.MerchantDirectData(
                merchantUserId,
                storeId,
                request.deliveryMode(),
                request.serviceDate() != null ? request.serviceDate() : LocalDate.now(),
                request.serviceStartTime(),
                request.serviceEndTime(),
                request.customerName().trim(),
                request.customerPhone().trim(),
                request.customerEmail() != null ? request.customerEmail().trim() : "",
                request.deliveryAddress(),
                request.deliveryReference(),
                request.deliveryLatitude(),
                request.deliveryLongitude(),
                currency,
                productsSubtotal,
                BigDecimal.ZERO,
                null,
                deliveryFee,
                BigDecimal.ZERO,
                totalAmount,
                request.paymentMethod(),
                request.notes(),
                itemsData
        );

        Order order = Order.createMerchantDirect(directData, orderNumber);
        Order savedOrder = orderRepository.save(order);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new OrderConfirmedDomainEvent(
                    savedOrder.getId(),
                    savedOrder.getOrderNumber(),
                    savedOrder.getStoreId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getCurrency()
            ));
        }

        return MerchantOrderDetailResponse.from(savedOrder);
    }

    private String formatOrderNumber(long sequence) {
        return String.format("ORD-%08d", sequence);
    }
}
