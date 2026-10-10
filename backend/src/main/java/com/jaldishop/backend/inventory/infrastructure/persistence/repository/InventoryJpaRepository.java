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

    /**
     * Suma de cantidades reservadas activas y vigentes de OTROS holds
     * (excluye la reserva de capacidad own): la base para la guardantía del
     * decremento atómico de ConfirmPurchase.
     */
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

    /**
     * Decremento atómico condicional de stock (estrategia §16.1).
     * 1 fila actualizada = stock descontado correctamente.
     * 0 filas = stock insuficiente o insuficiente para sostener holds ajenos vigentes.
     * Requiere haber adquirido el lock pesimista previo con findByVariantIdInForUpdate.
     */
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
}
