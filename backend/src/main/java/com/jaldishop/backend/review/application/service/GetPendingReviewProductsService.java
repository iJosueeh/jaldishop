package com.jaldishop.backend.review.application.service;

import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.review.web.dto.PendingReviewProductResponse;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class GetPendingReviewProductsService {

    private final JdbcTemplate jdbcTemplate;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public GetPendingReviewProductsService(
            JdbcTemplate jdbcTemplate,
            ProductRepository productRepository,
            StoreRepository storeRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<PendingReviewProductResponse> execute(UUID userId) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT DISTINCT ON (pv.product_id)
                       pv.product_id,
                       o.order_number,
                       o.confirmed_at
                FROM orders o
                JOIN order_items oi ON oi.order_id = o.id
                JOIN product_variants pv ON pv.id = oi.variant_id
                WHERE o.user_id = ?
                  AND o.status = 'COMPLETED'
                  AND NOT EXISTS (
                      SELECT 1 FROM reviews r
                      WHERE r.user_id = o.user_id
                        AND r.product_id = pv.product_id
                  )
                ORDER BY pv.product_id, o.confirmed_at DESC
                """,
                userId
        );

        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<PendingReviewProductResponse> result = new ArrayList<>();
        Map<UUID, Optional<Store>> storeCache = new HashMap<>();

        for (Map<String, Object> row : rows) {
            UUID productId = UUID.fromString(row.get("product_id").toString());
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                continue;
            }
            Product product = productOpt.get();
            Optional<Store> storeOpt = storeCache.computeIfAbsent(product.getStoreId(), storeRepository::findById);

            Object confirmedObj = row.get("confirmed_at");
            Instant lastPurchasedAt = confirmedObj instanceof Timestamp ts
                    ? ts.toInstant()
                    : confirmedObj instanceof Instant inst ? inst : null;

            result.add(new PendingReviewProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getSlug(),
                    storeOpt.map(Store::getName).orElse("Comercio Local"),
                    storeOpt.map(Store::getSlug).orElse(""),
                    product.getImageUrl(),
                    row.get("order_number") != null ? row.get("order_number").toString() : "",
                    lastPurchasedAt
            ));
        }

        result.sort(Comparator.comparing(
                PendingReviewProductResponse::lastPurchasedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }
}
