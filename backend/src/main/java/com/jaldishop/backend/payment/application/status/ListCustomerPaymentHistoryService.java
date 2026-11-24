package com.jaldishop.backend.payment.application.status;

import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.web.dto.CustomerPaymentHistoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ListCustomerPaymentHistoryService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public ListCustomerPaymentHistoryService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerPaymentHistoryResponse> execute(UUID customerUserId) {
        Objects.requireNonNull(customerUserId, "customerUserId no puede ser nulo");

        return paymentRepository.findByCustomerUserId(customerUserId).stream()
                .map(payment -> {
                    var order = orderRepository.findByPaymentId(payment.getId()).orElse(null);
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
                            payment.getRefundedAt()
                    );
                })
                .toList();
    }
}
