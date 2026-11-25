package com.jaldishop.backend.checkout.application.service;

import com.jaldishop.backend.checkout.application.command.InitiateCheckoutCommand;
import com.jaldishop.backend.checkout.application.port.out.StoreOperationalSettingsPort;
import com.jaldishop.backend.checkout.application.result.CheckoutItemSnapshotAssembler;
import com.jaldishop.backend.checkout.application.result.CheckoutResult;
import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;

import com.jaldishop.backend.capacity.application.reservation.command.CreateCapacityReservationCommand;
import com.jaldishop.backend.capacity.application.reservation.service.CreateCapacityReservationService;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.cart.domain.model.Cart;
import com.jaldishop.backend.cart.domain.repository.CartRepository;
import com.jaldishop.backend.checkout.domain.service.CheckoutFulfillmentValidator;
import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.model.CheckoutPricing;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import com.jaldishop.backend.inventory.application.service.ReserveInventoryService;
import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final StoreOperationalSettingsPort storeOperationalSettingsPort;

    @Autowired(required = false)
    private PlatformSettingsProvider platformSettingsProvider;

    @Autowired
    public InitiateCheckoutService(
            CartRepository cartRepository,
            StoreRepository storeRepository,
            CheckoutItemSnapshotAssembler snapshotAssembler,
            CreateCapacityReservationService createCapacityReservationService,
            ReserveInventoryService reserveInventoryService,
            StoreOperationalSettingsPort storeOperationalSettingsPort
    ) {
        this.cartRepository = cartRepository;
        this.storeRepository = storeRepository;
        this.snapshotAssembler = snapshotAssembler;
        this.createCapacityReservationService = createCapacityReservationService;
        this.reserveInventoryService = reserveInventoryService;
        this.storeOperationalSettingsPort = storeOperationalSettingsPort;
    }

    public InitiateCheckoutService(
            CartRepository cartRepository,
            StoreRepository storeRepository,
            CheckoutItemSnapshotAssembler snapshotAssembler,
            CreateCapacityReservationService createCapacityReservationService,
            ReserveInventoryService reserveInventoryService
    ) {
        this(cartRepository, storeRepository, snapshotAssembler, createCapacityReservationService, reserveInventoryService, null);
    }

    public CheckoutResult execute(InitiateCheckoutCommand command) {
        Store store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada."));

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");
        }

        if (store.getOwnerUserId() != null && store.getOwnerUserId().equals(command.userId())) {
            throw new BusinessRuleException(
                    "SELF_PURCHASE_NOT_ALLOWED",
                    "No puedes realizar compras en tu propia tienda."
            );
        }

        // Validación de reglas operativas de tienda (INDECOPI / Políticas de checkout)
        if (storeOperationalSettingsPort != null) {
            StoreOperationalSettingsPort.OperationalSettings ops =
                    storeOperationalSettingsPort.getOperationalSettings(command.storeId());
            if (ops != null) {
                if (!ops.onlinePaymentsEnabled() && !ops.cashOnDeliveryEnabled()) {
                    throw new BusinessRuleException("STORE_PAYMENTS_DISABLED", "La tienda no tiene métodos de pago habilitados actualmente.");
                }

                if (ops.requireCustomerDni()) {
                    if (command.customerDocumentNumber() == null || command.customerDocumentNumber().isBlank()) {
                        throw new BusinessRuleException("CUSTOMER_DNI_REQUIRED", "La tienda exige ingresar DNI o RUC para realizar compras.");
                    }
                    String doc = command.customerDocumentNumber().trim();
                    if (!doc.matches("\\d{8}|\\d{11}")) {
                        throw new BusinessRuleException("INVALID_CUSTOMER_DOCUMENT", "El documento debe ser un DNI válido de 8 dígitos o un RUC de 11 dígitos.");
                    }
                }

                if (!ops.allowOrderNotes()) {
                    if (command.orderNotes() != null && !command.orderNotes().isBlank()) {
                        throw new BusinessRuleException("ORDER_NOTES_NOT_ALLOWED", "La tienda no admite notas o instrucciones adicionales en el pedido.");
                    }
                }
            }
        }

        CheckoutFulfillmentValidator.validate(command.fulfillmentType(), command.deliveryAddress(), store);

        Cart cart = cartRepository.findByUserIdAndStoreId(command.userId(), command.storeId())
                .orElseThrow(() -> new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío.");
        }

        List<CheckoutItemSnapshot> itemSnapshots = snapshotAssembler.assemble(cart, command.storeId());
        String currency = itemSnapshots.isEmpty() ? "PEN" : itemSnapshots.getFirst().currency();

        BigDecimal serviceFeeRate = BigDecimal.ZERO;
        BigDecimal serviceFeeCap = null;
        if (platformSettingsProvider != null) {
            PlatformSettings platformSettings = platformSettingsProvider.getSettings();
            if (platformSettings != null) {
                if (platformSettings.customerServiceFeeRate() != null) {
                    serviceFeeRate = platformSettings.customerServiceFeeRate();
                }
                serviceFeeCap = platformSettings.customerServiceFeeCap();
            }
        }

        CheckoutPricing pricing = CheckoutPricing.calculate(
                itemSnapshots,
                command.fulfillmentType(),
                store.getDeliveryFeeAmount(),
                store.getTaxRate(),
                serviceFeeRate,
                serviceFeeCap,
                currency
        );

        // Validación de monto mínimo de entrega
        if (storeOperationalSettingsPort != null && command.fulfillmentType() == CheckoutFulfillmentType.DELIVERY) {
            StoreOperationalSettingsPort.OperationalSettings ops =
                    storeOperationalSettingsPort.getOperationalSettings(command.storeId());
            if (ops != null && ops.minDeliveryAmount() != null) {
                BigDecimal subtotal = pricing.productsSubtotalAmount();
                if (subtotal.compareTo(ops.minDeliveryAmount()) < 0) {
                    throw new BusinessRuleException("MIN_DELIVERY_AMOUNT_NOT_MET",
                            "El pedido no alcanza el monto mínimo de entrega de S/ " + ops.minDeliveryAmount());
                }
            }
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

        reserveInventoryService.execute(
                command.storeId(),
                capacityReservation.getId(),
                capacityReservation.getExpiresAt(),
                itemSnapshots
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
