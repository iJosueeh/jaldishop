package com.jaldishop.backend.ordering.application;

import com.jaldishop.backend.ordering.domain.Order;
import com.jaldishop.backend.ordering.domain.OrderRepository;
import com.jaldishop.backend.ordering.web.dto.*;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetCustomerOrderDetailService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    public GetCustomerOrderDetailService(OrderRepository orderRepository, StoreRepository storeRepository) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public CustomerOrderDetailResponse execute(UUID customerUserId, String orderNumber) {
        if (customerUserId == null) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio.");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("El número de pedido es obligatorio.");
        }

        Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .filter(o -> o.getUserId().equals(customerUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", orderNumber.trim()));

        Store store = storeRepository.findById(order.getStoreId()).orElse(null);

        CustomerOrderStoreInfo storeInfo = new CustomerOrderStoreInfo(
                order.getStoreId(),
                store != null ? store.getName() : "Comercio Local",
                store != null ? store.getSlug() : "",
                store != null ? store.getContactPhone() : null,
                store != null ? store.getWhatsappNumber() : null,
                store != null ? store.getAddress() : null,
                store != null ? store.getAddressReference() : null,
                store != null ? store.getLogoUrl() : null
        );

        List<CustomerOrderItemResponse> items = order.getItems().stream()
                .map(item -> new CustomerOrderItemResponse(
                        item.id(),
                        item.variantId(),
                        item.productName(),
                        item.variantName(),
                        item.quantity(),
                        item.unitPriceAmount(),
                        item.subtotalAmount()
                ))
                .toList();

        List<CustomerOrderStatusHistoryResponse> statusHistory = order.getStatusHistory().stream()
                .map(history -> new CustomerOrderStatusHistoryResponse(
                        history.status(),
                        history.reason(),
                        history.changedAt()
                ))
                .toList();

        return new CustomerOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                storeInfo,
                order.getStatus(),
                order.getDeliveryMode(),
                order.getServiceDate(),
                order.getServiceStartTime(),
                order.getServiceEndTime(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getDeliveryAddress(),
                order.getDeliveryReference(),
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude(),
                order.getCurrency(),
                order.getProductsSubtotalAmount(),
                order.getDiscountAmount(),
                order.getDiscountCode(),
                order.getDeliveryFeeAmount(),
                order.getIncludedTaxAmount(),
                order.getTotalAmount(),
                order.getConfirmedAt(),
                order.getUpdatedAt(),
                items,
                statusHistory
        );
    }

}
