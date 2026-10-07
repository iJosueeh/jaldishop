package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.capacity.application.CreateCapacityReservationCommand;
import com.jaldishop.backend.capacity.application.CreateCapacityReservationService;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentValidator;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.CheckoutPricing;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.inventory.application.ReserveInventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class InitiateCheckoutService {

    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final CheckoutItemSnapshotAssembler snapshotAssembler;
    private final CreateCapacityReservationService createCapacityReservationService;
    private final ReserveInventoryService reserveInventoryService;

    public InitiateCheckoutService(
            CartRepository cartRepository,
            StoreRepository storeRepository,
            CheckoutItemSnapshotAssembler snapshotAssembler,
            CreateCapacityReservationService createCapacityReservationService,
            ReserveInventoryService reserveInventoryService
    ) {
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.snapshotAssembler = snapshotAssembler;
        this.createCapacityReservationService = createCapacityReservationService;
        this.reserveInventoryService = reserveInventoryService;
    }

    public CheckoutResult execute(InitiateCheckoutCommand command) {
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

        List<CheckoutItemSnapshot> itemSnapshots = snapshotAssembler.assemble(cart, command.storeId());
        String currency = itemSnapshots.isEmpty() ? "PEN" : itemSnapshots.getFirst().currency();

        CapacityReservation capacityReservation = createCapacityReservationService.execute(
                new CreateCapacityReservationCommand(
                        command.storeId(),
                        command.userId(),
                        command.serviceDate(),
                        command.startTime(),
                        command.endTime()
                )
        );

        reserveInventoryService.execute(
                command.storeId(),
                capacityReservation.getId(),
                capacityReservation.getExpiresAt(),
                itemSnapshots
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
}
