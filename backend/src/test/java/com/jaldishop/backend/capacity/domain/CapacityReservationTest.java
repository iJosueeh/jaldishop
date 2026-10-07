package com.jaldishop.backend.capacity.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CapacityReservationTest {

    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("Crear reserva activa con vigencia de 10 minutos")
    void createActiveReservation() {
        CapacityReservation reservation = CapacityReservation.create(
                storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertNotNull(reservation.getId());
        assertEquals(storeId, reservation.getStoreId());
        assertEquals(userId, reservation.getUserId());
        assertEquals(LocalDate.of(2026, 9, 22), reservation.getServiceDate());
        assertEquals(LocalTime.of(10, 0), reservation.getStartTime());
        assertEquals(LocalTime.of(12, 0), reservation.getEndTime());
        assertEquals(CapacityReservationStatus.ACTIVE, reservation.getStatus());
        assertNotNull(reservation.getExpiresAt());
        assertTrue(reservation.getExpiresAt().isAfter(reservation.getCreatedAt()));
        assertNull(reservation.getPaymentProtectionExpiresAt());
        assertNotNull(reservation.getCreatedAt());
        assertNotNull(reservation.getUpdatedAt());
    }

    @Test
    @DisplayName("Lanzar excepción cuando storeId es nulo")
    void throwExceptionWhenStoreIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityReservation.create(null, userId, LocalDate.of(2026, 9, 22),
                        LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("Lanzar excepción cuando userId es nulo")
    void throwExceptionWhenUserIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityReservation.create(storeId, null, LocalDate.of(2026, 9, 22),
                        LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("Lanzar excepción cuando serviceDate es nulo")
    void throwExceptionWhenServiceDateIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityReservation.create(storeId, userId, null,
                        LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("Lanzar excepción cuando falta startTime")
    void throwExceptionWhenStartTimeMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityReservation.create(storeId, userId, LocalDate.of(2026, 9, 22),
                        null, LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("Lanzar excepción cuando falta endTime")
    void throwExceptionWhenEndTimeMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityReservation.create(storeId, userId, LocalDate.of(2026, 9, 22),
                        LocalTime.of(10, 0), null));
    }

    @Test
    @DisplayName("Lanzar excepción cuando startTime no es anterior a endTime")
    void throwExceptionWhenStartTimeNotBeforeEndTime() {
        assertThrows(IllegalArgumentException.class,
                () -> CapacityReservation.create(storeId, userId, LocalDate.of(2026, 9, 22),
                        LocalTime.of(12, 0), LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("isExpired devuelve true cuando la reserva venció")
    void isExpiredAfterDeadline() {
        Instant createdAt = Instant.now().minusSeconds(600);
        CapacityReservation reservation = activeFixture(createdAt);

        assertTrue(reservation.isExpired(createdAt.plusSeconds(601)));
    }

    @Test
    @DisplayName("isExpired devuelve false mientras la reserva está vigente")
    void isExpiredBeforeDeadline() {
        Instant createdAt = Instant.now().minusSeconds(600);
        CapacityReservation reservation = activeFixture(createdAt);

        assertFalse(reservation.isExpired(createdAt.plusSeconds(599)));
        assertFalse(reservation.isExpired(createdAt));
    }

    @Test
    @DisplayName("expireIfDue marca la reserva como EXPIRADA al vencer")
    void expireIfDueExpiresActiveReservation() {
        CapacityReservation reservation = CapacityReservation.create(
                storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        reservation.expireIfDue(reservation.getExpiresAt().plusSeconds(1));

        assertEquals(CapacityReservationStatus.EXPIRED, reservation.getStatus());
    }

    @Test
    @DisplayName("expireIfDue no cambia una reserva vigente")
    void expireIfDueKeepsActiveReservation() {
        CapacityReservation reservation = CapacityReservation.create(
                storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        reservation.expireIfDue(reservation.getExpiresAt().minusSeconds(1));

        assertEquals(CapacityReservationStatus.ACTIVE, reservation.getStatus());
    }

    @Test
    @DisplayName("Liberar reserva activa pasa a RELEASED")
    void releaseActiveReservation() {
        CapacityReservation reservation = CapacityReservation.create(
                storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        reservation.release();

        assertEquals(CapacityReservationStatus.RELEASED, reservation.getStatus());
    }

    @Test
    @DisplayName("Liberar reserva protegida por pago pasa a RELEASED")
    void releasePaymentProtectedReservation() {
        CapacityReservation reservation = activeFixture(Instant.now());
        reservation.protectPayment();

        reservation.release();

        assertEquals(CapacityReservationStatus.RELEASED, reservation.getStatus());
    }

    @Test
    @DisplayName("Liberar una reserva comprometida lanza excepción")
    void releaseCommittedReservationThrows() {
        CapacityReservation reservation = activeFixture(Instant.now());
        reservation.protectPayment();
        reservation.commit();

        assertThrows(IllegalStateException.class, reservation::release);
    }

    @Test
    @DisplayName("Liberar dos veces lanza excepción (estado terminal)")
    void releaseTwiceThrows() {
        CapacityReservation reservation = CapacityReservation.create(
                storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));
        reservation.release();

        assertThrows(IllegalStateException.class, reservation::release);
    }

    @Test
    @DisplayName("protectPayment transiciona ACTIVA a PAYMENT_PROTECTED")
    void protectPaymentTransitionsToProtected() {
        CapacityReservation reservation = CapacityReservation.create(
                storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        reservation.protectPayment();

        assertEquals(CapacityReservationStatus.PAYMENT_PROTECTED, reservation.getStatus());
        assertNotNull(reservation.getPaymentProtectionExpiresAt());
    }

    @Test
    @DisplayName("protectPayment sobre reserva expirada lanza excepción")
    void protectPaymentExpiredThrows() {
        CapacityReservation reservation = activeFixture(Instant.now().minusSeconds(700));

        assertThrows(IllegalStateException.class, reservation::protectPayment);
    }

    @Test
    @DisplayName("commit transiciona PROTEGIDA_PAGO a COMPROMETIDA")
    void commitTransitionsToCommitted() {
        CapacityReservation reservation = CapacityReservation.create(
                storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));
        reservation.protectPayment();

        reservation.commit();

        assertEquals(CapacityReservationStatus.COMMITTED, reservation.getStatus());
    }

    @Test
    @DisplayName("commit sobre reserva activa lanza excepción")
    void commitActiveThrows() {
        CapacityReservation reservation = CapacityReservation.create(
                storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThrows(IllegalStateException.class, reservation::commit);
    }

    @Test
    @DisplayName("Reconstituir reserva desde persistencia")
    void reconstituteReservation() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-22T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-22T10:00:05Z");
        Instant expiresAt = Instant.parse("2026-09-22T10:10:00Z");
        Instant protectionExpiresAt = Instant.parse("2026-09-22T10:20:00Z");

        CapacityReservation reservation = CapacityReservation.reconstitute(
                id, storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.PAYMENT_PROTECTED, expiresAt,
                protectionExpiresAt, createdAt, updatedAt);

        assertEquals(id, reservation.getId());
        assertEquals(storeId, reservation.getStoreId());
        assertEquals(userId, reservation.getUserId());
        assertEquals(LocalDate.of(2026, 9, 22), reservation.getServiceDate());
        assertEquals(LocalTime.of(10, 0), reservation.getStartTime());
        assertEquals(LocalTime.of(12, 0), reservation.getEndTime());
        assertEquals(CapacityReservationStatus.PAYMENT_PROTECTED, reservation.getStatus());
        assertEquals(expiresAt, reservation.getExpiresAt());
        assertEquals(protectionExpiresAt, reservation.getPaymentProtectionExpiresAt());
        assertEquals(createdAt, reservation.getCreatedAt());
        assertEquals(updatedAt, reservation.getUpdatedAt());
    }

    @Test
    @DisplayName("isExpired en PAYMENT_PROTECTED devuelve false mientras esté vigente")
    void isExpiredPaymentProtectedBeforeDeadline() {
        Instant now = Instant.now();
        CapacityReservation reservation = CapacityReservation.reconstitute(
                UUID.randomUUID(), storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.PAYMENT_PROTECTED, now.minusSeconds(100), now.plusSeconds(300),
                now.minusSeconds(700), now.minusSeconds(100));

        assertFalse(reservation.isExpired(now));
    }

    @Test
    @DisplayName("isExpired en PAYMENT_PROTECTED devuelve true al vencer la protección")
    void isExpiredPaymentProtectedAfterDeadline() {
        Instant now = Instant.now();
        CapacityReservation reservation = CapacityReservation.reconstitute(
                UUID.randomUUID(), storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.PAYMENT_PROTECTED, now.minusSeconds(700), now.minusSeconds(10),
                now.minusSeconds(700), now.minusSeconds(100));

        assertTrue(reservation.isExpired(now));
    }

    @Test
    @DisplayName("isExpired en PAYMENT_PROTECTED con paymentProtectionExpiresAt nulo se considera vencido/anómalo")
    void isExpiredPaymentProtectedWithNullDeadline() {
        Instant now = Instant.now();
        CapacityReservation reservation = CapacityReservation.reconstitute(
                UUID.randomUUID(), storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.PAYMENT_PROTECTED, now.minusSeconds(100), null,
                now.minusSeconds(700), now.minusSeconds(100));

        assertTrue(reservation.isExpired(now));
    }

    private CapacityReservation activeFixture(Instant createdAt) {
        return CapacityReservation.reconstitute(
                UUID.randomUUID(), storeId, userId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.ACTIVE, createdAt.plusSeconds(600), null,
                createdAt, createdAt);
    }
}