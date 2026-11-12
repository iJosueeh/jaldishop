package com.jaldishop.backend.analytics.infrastructure.persistence;

import com.jaldishop.backend.analytics.domain.model.*;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcAnalyticsRepositoryAdapter implements AnalyticsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAnalyticsRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate no puede ser nulo");
    }

    @Override
    public CommercialSummary findCommercialSummary(PeriodFilter period, UUID storeId, UUID userId) {
        String sql = """
            WITH period_orders AS (
                SELECT
                    COUNT(o.id) AS historical_confirmed_orders_count,
                    COALESCE(SUM(o.total_amount), 0) AS historical_confirmed_gmv,
                    COUNT(CASE WHEN o.status != 'CANCELLED' THEN 1 END) AS active_orders_count,
                    COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.total_amount ELSE 0 END), 0) AS active_commercial_volume,
                    COUNT(CASE WHEN o.status = 'COMPLETED' THEN 1 END) AS completed_orders_count,
                    COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.products_subtotal_amount ELSE 0 END), 0) AS products_gross_sales,
                    COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.discount_amount ELSE 0 END), 0) AS total_discounts,
                    COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.delivery_fee_amount ELSE 0 END), 0) AS total_delivery_fees
                FROM orders o
                WHERE o.confirmed_at >= :start_instant
                  AND o.confirmed_at < :end_instant
                  AND (cast(:store_id as uuid) IS NULL OR o.store_id = cast(:store_id as uuid))
                  AND (cast(:user_id as uuid) IS NULL OR o.user_id = cast(:user_id as uuid))
            ),
            cancelled_orders AS (
                SELECT
                    COUNT(DISTINCT o.id) AS cancelled_orders_count,
                    COALESCE(SUM(o.total_amount), 0) AS cancelled_orders_volume
                FROM orders o
                JOIN order_status_history osh ON osh.order_id = o.id AND osh.status = 'CANCELLED'
                WHERE o.status = 'CANCELLED'
                  AND osh.changed_at >= :start_instant
                  AND osh.changed_at < :end_instant
                  AND (cast(:store_id as uuid) IS NULL OR o.store_id = cast(:store_id as uuid))
                  AND (cast(:user_id as uuid) IS NULL OR o.user_id = cast(:user_id as uuid))
            )
            SELECT
                po.historical_confirmed_orders_count,
                po.historical_confirmed_gmv,
                po.active_orders_count,
                po.active_commercial_volume,
                po.completed_orders_count,
                po.products_gross_sales,
                po.total_discounts,
                po.total_delivery_fees,
                ca.cancelled_orders_count,
                ca.cancelled_orders_volume
            FROM period_orders po
            CROSS JOIN cancelled_orders ca;
            """;

        MapSqlParameterSource params = createBaseParams(period, storeId, userId);

        return jdbcTemplate.query(sql, params, (rs) -> {
            if (rs.next()) {
                long histConfirmed = rs.getLong("historical_confirmed_orders_count");
                BigDecimal histGmv = rs.getBigDecimal("historical_confirmed_gmv");
                long activeOrders = rs.getLong("active_orders_count");
                BigDecimal activeVolume = rs.getBigDecimal("active_commercial_volume");
                long completed = rs.getLong("completed_orders_count");
                BigDecimal grossSales = rs.getBigDecimal("products_gross_sales");
                BigDecimal discounts = rs.getBigDecimal("total_discounts");
                BigDecimal deliveryFees = rs.getBigDecimal("total_delivery_fees");
                long cancelled = rs.getLong("cancelled_orders_count");
                BigDecimal cancelledVol = rs.getBigDecimal("cancelled_orders_volume");

                return CommercialSummary.of(
                        histConfirmed, histGmv, activeOrders, activeVolume, completed,
                        grossSales, discounts, deliveryFees, cancelled, cancelledVol
                );
            }
            return CommercialSummary.empty();
        });
    }

    @Override
    public FinancialSummary findFinancialSummary(PeriodFilter period, UUID storeId, UUID userId) {
        String sql = """
            WITH approved_payments AS (
                SELECT
                    p.id,
                    p.amount,
                    o.id AS order_id,
                    COALESCE(o.store_id, cr.store_id) AS effective_store_id,
                    COALESCE(o.user_id, cr.user_id) AS effective_user_id
                FROM payments p
                LEFT JOIN orders o ON o.payment_id = p.id
                JOIN capacity_reservations cr ON cr.id = p.capacity_reservation_id
                WHERE p.status = 'APPROVED'
                  AND p.approved_at >= :start_instant
                  AND p.approved_at < :end_instant
            ),
            inflows AS (
                SELECT
                    COALESCE(SUM(ap.amount), 0) AS gross_inflow_amount,
                    COUNT(ap.id) AS approved_payments_count,
                    COALESCE(SUM(CASE WHEN ap.order_id IS NOT NULL THEN ap.amount ELSE 0 END), 0) AS order_linked_amount,
                    COUNT(CASE WHEN ap.order_id IS NOT NULL THEN 1 END) AS order_linked_count,
                    COALESCE(SUM(CASE WHEN ap.order_id IS NULL THEN ap.amount ELSE 0 END), 0) AS orphan_amount,
                    COUNT(CASE WHEN ap.order_id IS NULL THEN 1 END) AS orphan_count
                FROM approved_payments ap
                WHERE (cast(:store_id as uuid) IS NULL OR ap.effective_store_id = cast(:store_id as uuid))
                  AND (cast(:user_id as uuid) IS NULL OR ap.effective_user_id = cast(:user_id as uuid))
            ),
            refunded_payments AS (
                SELECT
                    p.id,
                    p.refund_amount,
                    COALESCE(o.store_id, cr.store_id) AS effective_store_id,
                    COALESCE(o.user_id, cr.user_id) AS effective_user_id
                FROM payments p
                LEFT JOIN orders o ON o.payment_id = p.id
                JOIN capacity_reservations cr ON cr.id = p.capacity_reservation_id
                WHERE p.refund_status = 'COMPLETED'
                  AND p.refunded_at >= :start_instant
                  AND p.refunded_at < :end_instant
            ),
            outflows AS (
                SELECT
                    COALESCE(SUM(rp.refund_amount), 0) AS settled_refunds_amount,
                    COUNT(rp.id) AS settled_refunds_count
                FROM refunded_payments rp
                WHERE (cast(:store_id as uuid) IS NULL OR rp.effective_store_id = cast(:store_id as uuid))
                  AND (cast(:user_id as uuid) IS NULL OR rp.effective_user_id = cast(:user_id as uuid))
            )
            SELECT
                i.gross_inflow_amount,
                i.approved_payments_count,
                o.settled_refunds_amount,
                o.settled_refunds_count,
                i.order_linked_amount,
                i.order_linked_count,
                i.orphan_amount,
                i.orphan_count
            FROM inflows i
            CROSS JOIN outflows o;
            """;

        MapSqlParameterSource params = createBaseParams(period, storeId, userId);

        return jdbcTemplate.query(sql, params, (rs) -> {
            if (rs.next()) {
                BigDecimal grossInflow = rs.getBigDecimal("gross_inflow_amount");
                long approvedCount = rs.getLong("approved_payments_count");
                BigDecimal refunds = rs.getBigDecimal("settled_refunds_amount");
                long refundsCount = rs.getLong("settled_refunds_count");
                BigDecimal orderLinkedAmount = rs.getBigDecimal("order_linked_amount");
                long orderLinkedCount = rs.getLong("order_linked_count");
                BigDecimal orphanAmount = rs.getBigDecimal("orphan_amount");
                long orphanCount = rs.getLong("orphan_count");

                BigDecimal effectiveInflow = storeId != null ? orderLinkedAmount : grossInflow;

                return FinancialSummary.of(
                        effectiveInflow,
                        storeId != null ? orderLinkedCount : approvedCount,
                        refunds,
                        refundsCount,
                        orderLinkedAmount,
                        orderLinkedCount,
                        storeId != null ? BigDecimal.ZERO : orphanAmount,
                        storeId != null ? 0L : orphanCount
                );
            }
            return FinancialSummary.empty();
        });
    }

    @Override
    public List<ProductSalesRank> findTopSellingProducts(PeriodFilter period, UUID storeId, UUID userId, int limit) {
        String sql = """
            SELECT
                oi.variant_id,
                pv.product_id,
                oi.product_name,
                oi.variant_name,
                SUM(oi.quantity) AS units_sold,
                SUM(oi.subtotal_amount) AS total_revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN product_variants pv ON pv.id = oi.variant_id
            WHERE o.status != 'CANCELLED'
              AND o.confirmed_at >= :start_instant
              AND o.confirmed_at < :end_instant
              AND (cast(:store_id as uuid) IS NULL OR oi.store_id = cast(:store_id as uuid))
              AND (cast(:user_id as uuid) IS NULL OR o.user_id = cast(:user_id as uuid))
            GROUP BY oi.variant_id, pv.product_id, oi.product_name, oi.variant_name
            ORDER BY units_sold DESC, total_revenue DESC
            LIMIT :limit;
            """;

        MapSqlParameterSource params = createBaseParams(period, storeId, userId)
                .addValue("limit", Math.max(1, Math.min(limit, 100)));

        return jdbcTemplate.query(sql, params, this::mapProductSalesRank);
    }

    @Override
    public List<StorePerformanceRank> findStoreRankings(PeriodFilter period, int limit) {
        String sql = """
            WITH store_orders AS (
                SELECT
                    o.store_id,
                    COUNT(o.id) AS confirmed_orders_count,
                    COALESCE(SUM(o.total_amount), 0) AS commercial_gmv
                FROM orders o
                WHERE o.status != 'CANCELLED'
                  AND o.confirmed_at >= :start_instant
                  AND o.confirmed_at < :end_instant
                GROUP BY o.store_id
            ),
            store_inflows AS (
                SELECT
                    o.store_id,
                    COALESCE(SUM(p.amount), 0) AS gross_inflow
                FROM payments p
                JOIN orders o ON o.payment_id = p.id
                WHERE p.status = 'APPROVED'
                  AND p.approved_at >= :start_instant
                  AND p.approved_at < :end_instant
                GROUP BY o.store_id
            ),
            store_refunds AS (
                SELECT
                    o.store_id,
                    COALESCE(SUM(p.refund_amount), 0) AS settled_refunds
                FROM payments p
                JOIN orders o ON o.payment_id = p.id
                WHERE p.refund_status = 'COMPLETED'
                  AND p.refunded_at >= :start_instant
                  AND p.refunded_at < :end_instant
                GROUP BY o.store_id
            )
            SELECT
                s.id AS store_id,
                s.name AS store_name,
                COALESCE(so.confirmed_orders_count, 0) AS confirmed_orders_count,
                COALESCE(so.commercial_gmv, 0) AS commercial_gmv,
                COALESCE(si.gross_inflow, 0) AS gross_inflow,
                COALESCE(sr.settled_refunds, 0) AS settled_refunds,
                (COALESCE(si.gross_inflow, 0) - COALESCE(sr.settled_refunds, 0)) AS net_financial_inflow
            FROM stores s
            LEFT JOIN store_orders so ON so.store_id = s.id
            LEFT JOIN store_inflows si ON si.store_id = s.id
            LEFT JOIN store_refunds sr ON sr.store_id = s.id
            WHERE (so.confirmed_orders_count > 0 OR si.gross_inflow > 0 OR sr.settled_refunds > 0)
            ORDER BY commercial_gmv DESC, confirmed_orders_count DESC
            LIMIT :limit;
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start_instant", Timestamp.from(period.startInstant()))
                .addValue("end_instant", Timestamp.from(period.endInstant()))
                .addValue("limit", Math.max(1, Math.min(limit, 100)));

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new StorePerformanceRank(
                rs.getObject("store_id", UUID.class),
                rs.getString("store_name"),
                rs.getLong("confirmed_orders_count"),
                rs.getBigDecimal("commercial_gmv"),
                rs.getBigDecimal("gross_inflow"),
                rs.getBigDecimal("settled_refunds"),
                rs.getBigDecimal("net_financial_inflow")
        ));
    }

    @Override
    public CapacityDemandSummary findCapacityDemand(PeriodFilter period, UUID storeId) {
        String sql = """
            SELECT
                COUNT(CASE WHEN cr.status = 'COMMITTED' THEN 1 END) AS committed_count,
                COUNT(CASE WHEN cr.status = 'RELEASED' THEN 1 END) AS released_count
            FROM capacity_reservations cr
            WHERE cr.service_date >= :start_date
              AND cr.service_date <= :end_date
              AND (cast(:store_id as uuid) IS NULL OR cr.store_id = cast(:store_id as uuid));
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start_date", period.fromDate())
                .addValue("end_date", period.toDate())
                .addValue("store_id", storeId, Types.OTHER);

        return jdbcTemplate.query(sql, params, (rs) -> {
            if (rs.next()) {
                return CapacityDemandSummary.of(
                        rs.getLong("committed_count"),
                        rs.getLong("released_count")
                );
            }
            return CapacityDemandSummary.of(0L, 0L);
        });
    }

    @Override
    public CustomerSpendingSummary findCustomerSpending(PeriodFilter period, UUID userId) {
        Objects.requireNonNull(userId, "userId es obligatorio para consultar spending de cliente");

        String sql = """
            WITH customer_orders AS (
                SELECT
                    o.user_id,
                    COUNT(o.id) AS total_orders_count,
                    COALESCE(SUM(o.total_amount), 0) AS total_spent_amount,
                    MIN(o.confirmed_at) AS first_order_at,
                    MAX(o.confirmed_at) AS last_order_at
                FROM orders o
                WHERE o.status != 'CANCELLED'
                  AND o.confirmed_at >= :start_instant
                  AND o.confirmed_at < :end_instant
                  AND o.user_id = cast(:user_id as uuid)
                GROUP BY o.user_id
            ),
            customer_refunds AS (
                SELECT
                    o.user_id,
                    COALESCE(SUM(p.refund_amount), 0) AS total_refunded_amount
                FROM payments p
                JOIN orders o ON o.payment_id = p.id
                WHERE p.refund_status = 'COMPLETED'
                  AND p.refunded_at >= :start_instant
                  AND p.refunded_at < :end_instant
                  AND o.user_id = cast(:user_id as uuid)
                GROUP BY o.user_id
            )
            SELECT
                cast(:user_id as uuid) AS user_id,
                COALESCE(co.total_orders_count, 0) AS total_orders_count,
                COALESCE(co.total_spent_amount, 0) AS total_spent_amount,
                COALESCE(cr.total_refunded_amount, 0) AS total_refunded_amount,
                co.first_order_at,
                co.last_order_at
            FROM (SELECT 1) dummy
            LEFT JOIN customer_orders co ON 1=1
            LEFT JOIN customer_refunds cr ON 1=1;
            """;

        MapSqlParameterSource params = createBaseParams(period, null, userId);

        return jdbcTemplate.query(sql, params, (rs) -> {
            if (rs.next()) {
                Timestamp firstOrderTs = rs.getTimestamp("first_order_at");
                Timestamp lastOrderTs = rs.getTimestamp("last_order_at");

                return new CustomerSpendingSummary(
                        userId,
                        rs.getLong("total_orders_count"),
                        rs.getBigDecimal("total_spent_amount"),
                        rs.getBigDecimal("total_refunded_amount"),
                        firstOrderTs != null ? firstOrderTs.toInstant() : null,
                        lastOrderTs != null ? lastOrderTs.toInstant() : null
                );
            }
            return new CustomerSpendingSummary(userId, 0L, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        });
    }

    private ProductSalesRank mapProductSalesRank(ResultSet rs, int rowNum) throws SQLException {
        return new ProductSalesRank(
                rs.getObject("variant_id", UUID.class),
                rs.getObject("product_id", UUID.class),
                rs.getString("product_name"),
                rs.getString("variant_name"),
                rs.getLong("units_sold"),
                rs.getBigDecimal("total_revenue")
        );
    }

    private MapSqlParameterSource createBaseParams(PeriodFilter period, UUID storeId, UUID userId) {
        return new MapSqlParameterSource()
                .addValue("start_instant", Timestamp.from(period.startInstant()))
                .addValue("end_instant", Timestamp.from(period.endInstant()))
                .addValue("store_id", storeId, Types.OTHER)
                .addValue("user_id", userId, Types.OTHER);
    }
}
