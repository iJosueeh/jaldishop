package com.jaldishop.backend.payment.application.status;

import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.model.OrderItem;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.web.dto.CustomerPaymentHistoryResponse;
import com.jaldishop.backend.shared.web.dto.CustomerPageResponse;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListCustomerPaymentHistoryService {

    private static final Set<String> ALLOWED_FILTERS = Set.of(
            "ALL", "WITH_ORDER", "APPROVED", "REFUND", "NO_ORDER"
    );

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    @Autowired
    public ListCustomerPaymentHistoryService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            StoreRepository storeRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
    }

    public ListCustomerPaymentHistoryService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository
    ) {
        this(paymentRepository, orderRepository, null);
    }

    @Transactional(readOnly = true)
    public List<CustomerPaymentHistoryResponse> execute(UUID customerUserId) {
        Objects.requireNonNull(customerUserId, "customerUserId no puede ser nulo");
        List<Payment> payments = paymentRepository.findByCustomerUserId(customerUserId);
        return mapPaymentsWithOrders(payments);
    }

    @Transactional(readOnly = true)
    public CustomerPageResponse<CustomerPaymentHistoryResponse> executePaged(
            UUID customerUserId,
            String filter,
            String q,
            int page,
            int size
    ) {
        Objects.requireNonNull(customerUserId, "customerUserId no puede ser nulo");

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        String normalizedFilter = normalizeFilter(filter);
        String searchPattern = buildSearchPattern(q);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Payment> resultPage = paymentRepository.findPagedByCustomerUserId(
                customerUserId,
                normalizedFilter,
                searchPattern,
                pageable
        );

        List<CustomerPaymentHistoryResponse> items = mapPaymentsWithOrders(resultPage.getContent());

        return new CustomerPageResponse<>(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    private List<CustomerPaymentHistoryResponse> mapPaymentsWithOrders(List<Payment> payments) {
        if (payments.isEmpty()) {
            return List.of();
        }

        List<UUID> paymentIds = payments.stream().map(Payment::getId).toList();
        Map<UUID, Order> ordersByPaymentId = orderRepository.findByPaymentIdIn(paymentIds).stream()
                .collect(Collectors.toMap(Order::getPaymentId, Function.identity(), (a, b) -> a));

        Map<UUID, Store> storeCache = new ConcurrentHashMap<>();

        return payments.stream()
                .map(payment -> {
                    Order order = ordersByPaymentId.get(payment.getId());
                    String storeName = null;
                    Integer totalItems = null;

                    if (order != null) {
                        totalItems = order.getItems().stream()
                                .mapToInt(OrderItem::quantity)
                                .sum();
                        if (storeRepository != null && order.getStoreId() != null) {
                            Store store = storeCache.computeIfAbsent(
                                    order.getStoreId(),
                                    id -> storeRepository.findById(id).orElse(null)
                            );
                            if (store != null) {
                                storeName = store.getName();
                            }
                        }
                    }

                    return new CustomerPaymentHistoryResponse(
                            payment.getId(),
                            payment.getStatus().name(),
                            payment.getRefundStatus().name(),
                            payment.getAmount(),
                            payment.getCurrency(),
                            order != null ? order.getOrderNumber() : null,
                            order != null ? order.getStatus().name() : null,
                            payment.getCreatedAt(),
                            payment.getApprovedAt(),
                            payment.getRefundedAt(),
                            storeName,
                            totalItems
                    );
                })
                .toList();
    }

    private String normalizeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return "ALL";
        }
        String upper = filter.trim().toUpperCase();
        return ALLOWED_FILTERS.contains(upper) ? upper : "ALL";
    }

    private String buildSearchPattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String escaped = q.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
