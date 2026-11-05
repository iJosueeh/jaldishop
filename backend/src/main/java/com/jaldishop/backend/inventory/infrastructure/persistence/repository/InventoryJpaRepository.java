package com.jaldishop.backend.inventory.infrastructure.persistence.repository;

import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM InventoryEntity i
            WHERE i.variantId IN :variantIds
            ORDER BY i.variantId ASC
            """)
    List<InventoryEntity> findByVariantIdInForUpdate(@Param("variantIds") List<UUID> variantIds);
    @Query("""
            SELECT COALESCE(SUM(ir.quantity), 0)
            FROM InventoryReservationEntity ir, CapacityReservationEntity cr
            WHERE ir.capacityReservationId = cr.id
              AND ir.storeId = cr.storeId
              AND ir.storeId = :storeId
              AND ir.variantId = :variantId
              AND ir.status = 'ACTIVE'
              AND (
                   (cr.status = 'ACTIVE' AND cr.expiresAt > :now)
                OR (cr.status = 'PAYMENT_PROTECTED' AND cr.paymentProtectionExpiresAt IS NOT NULL AND cr.paymentProtectionExpiresAt > :now)
              )
            """)
    int sumActiveReservedQuantity(
            @Param("storeId") UUID storeId,
            @Param("variantId") UUID variantId,
            @Param("now") Instant now);
    @Query("""
            SELECT COALESCE(SUM(ir.quantity), 0)
            FROM InventoryReservationEntity ir, CapacityReservationEntity cr
            WHERE ir.capacityReservationId = cr.id
              AND ir.storeId = cr.storeId
              AND ir.storeId = :storeId
              AND ir.variantId = :variantId
              AND ir.status = 'ACTIVE'
              AND ir.capacityReservationId <> :capacityReservationId
              AND (
                   (cr.status = 'ACTIVE' AND cr.expiresAt > :now)
                OR (cr.status = 'PAYMENT_PROTECTED' AND cr.paymentProtectionExpiresAt IS NOT NULL AND cr.paymentProtectionExpiresAt > :now)
              )
            """)
    int sumActiveReservedQuantityExcluding(
            @Param("storeId") UUID storeId,
            @Param("variantId") UUID variantId,
            @Param("capacityReservationId") UUID capacityReservationId,
            @Param("now") Instant now);
    @Modifying
    @Query("""
            UPDATE InventoryEntity i
            SET i.quantity = i.quantity - :quantity, i.updatedAt = :now
            WHERE i.variantId = :variantId
              AND i.quantity - :quantity - :otherActiveReservedQuantity >= 0
            """)
    int decrementStockConditionally(
            @Param("variantId") UUID variantId,
            @Param("quantity") int quantity,
            @Param("otherActiveReservedQuantity") int otherActiveReservedQuantity,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at)
            VALUES (:variantId, :quantity, NULL, :now)
            ON CONFLICT (variant_id) DO UPDATE
            SET quantity = EXCLUDED.quantity,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    int upsertStock(
            @Param("variantId") UUID variantId,
            @Param("quantity") int quantity,
            @Param("now") Instant now);

    @Query(value = """
            SELECT
                pv.id AS variantId,
                pv.product_id AS productId,
                p.name AS productName,
                pv.presentation_name AS presentationName,
                pv.sku AS sku,
                pv.tracks_inventory AS tracksInventory,
                pv.status AS status,
                i.quantity AS physicalQuantity,
                i.low_stock_threshold AS lowStockThreshold,
                i.updated_at AS inventoryUpdatedAt
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN inventories i ON pv.id = i.variant_id
            WHERE pv.store_id = :storeId
            ORDER BY p.name ASC, pv.presentation_name ASC
            """, nativeQuery = true)
    List<StoreVariantInventoryProjection> findAllVariantInventoriesByStoreId(@Param("storeId") UUID storeId);

    @Query(value = """
            SELECT
                pv.id AS variantId,
                pv.product_id AS productId,
                p.name AS productName,
                pv.presentation_name AS presentationName,
                pv.sku AS sku,
                pv.tracks_inventory AS tracksInventory,
                pv.status AS status,
                i.quantity AS physicalQuantity,
                i.low_stock_threshold AS lowStockThreshold,
                i.updated_at AS inventoryUpdatedAt
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            LEFT JOIN inventories i ON pv.id = i.variant_id
            WHERE pv.store_id = :storeId
              AND pv.id = :variantId
            """, nativeQuery = true)
    java.util.Optional<StoreVariantInventoryProjection> findVariantInventoryByStoreIdAndVariantId(
            @Param("storeId") UUID storeId,
            @Param("variantId") UUID variantId);

    @Query(value = """
            SELECT
                pv.id AS variantId,
                pv.product_id AS productId,
                p.name AS productName,
                pv.presentation_name AS presentationName,
                pv.sku AS sku,
                pv.tracks_inventory AS tracksInventory,
                pv.status AS status,
                i.quantity AS physicalQuantity,
                i.low_stock_threshold AS lowStockThreshold,
                i.updated_at AS inventoryUpdatedAt
            FROM product_variants pv
            JOIN products p ON pv.product_id = p.id
            JOIN inventories i ON pv.id = i.variant_id
            WHERE pv.store_id = :storeId
              AND pv.tracks_inventory = true
              AND i.low_stock_threshold IS NOT NULL
              AND i.quantity <= i.low_stock_threshold
            ORDER BY i.quantity ASC, p.name ASC
            """, nativeQuery = true)
    List<StoreVariantInventoryProjection> findLowStockVariantsByStoreId(@Param("storeId") UUID storeId);
}
