package com.jaldishop.backend.inventory.infrastructure.persistence.repository;

import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, UUID> {

    /**
     * PROTOCOLO DE CONCURRENCIA: ORDEN GLOBAL DE LOCKS
     * 1. Capacity Anchor (adquirido en CreateCapacityReservationService)
     * 2. Inventories ordenados canónicamente por variant_id ASC (este método)
     *
     * ESTE ORDEN ES OBLIGATORIO Y NUNCA DEBE INVERTIRSE PARA PREVENIR DEADLOCKS.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM InventoryEntity i
            WHERE i.variantId IN :variantIds
            ORDER BY i.variantId ASC
            """)
    List<InventoryEntity> findByVariantIdInForUpdate(@Param("variantIds") List<UUID> variantIds);

    /**
     * Calcula la suma de cantidades reservadas activas y vigentes para una variante y tienda dada.
     * Expresa el aislamiento multitenant reforzado por V13 (ir.storeId = cr.storeId).
     * La autoridad temporal del hold reside en capacity_reservations.
     */
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
}
