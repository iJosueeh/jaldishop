package com.jaldishop.backend.checkout.domain;

import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutFulfillmentValidatorTest {

    @Test
    @DisplayName("Debe validar exitosamente cuando DELIVERY está habilitado y se provee dirección")
    void shouldValidateDeliverySuccessfully() {
        Store store = Store.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), "Tienda Test", "tienda-test",
                null, null, null, null, null, null,
                true, true, new BigDecimal("5.00"), "PEN", null,
                StoreStatus.ACTIVE, null, null
        );

        assertThatCode(() -> CheckoutFulfillmentValidator.validate(
                CheckoutFulfillmentType.DELIVERY, "Av Principal 123", store
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe lanzar ConflictException cuando DELIVERY está deshabilitado")
    void shouldThrowConflictWhenDeliveryDisabled() {
        Store store = Store.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), "Tienda Test", "tienda-test",
                null, null, null, null, null, null,
                true, false, null, "PEN", null,
                StoreStatus.ACTIVE, null, null
        );

        assertThatThrownBy(() -> CheckoutFulfillmentValidator.validate(
                CheckoutFulfillmentType.DELIVERY, "Av Principal 123", store
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("envíos a domicilio");
    }

    @Test
    @DisplayName("Debe lanzar BusinessRuleException cuando falta dirección de entrega en DELIVERY")
    void shouldThrowBusinessRuleWhenDeliveryAddressMissing() {
        Store store = Store.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), "Tienda Test", "tienda-test",
                null, null, null, null, null, null,
                true, true, new BigDecimal("5.00"), "PEN", null,
                StoreStatus.ACTIVE, null, null
        );

        assertThatThrownBy(() -> CheckoutFulfillmentValidator.validate(
                CheckoutFulfillmentType.DELIVERY, "   ", store
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dirección de entrega es obligatoria");
    }

    @Test
    @DisplayName("Debe validar exitosamente cuando PICKUP está habilitado")
    void shouldValidatePickupSuccessfully() {
        Store store = Store.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), "Tienda Test", "tienda-test",
                null, null, null, null, null, null,
                true, false, null, "PEN", null,
                StoreStatus.ACTIVE, null, null
        );

        assertThatCode(() -> CheckoutFulfillmentValidator.validate(
                CheckoutFulfillmentType.PICKUP, null, store
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe lanzar ConflictException cuando PICKUP está deshabilitado")
    void shouldThrowConflictWhenPickupDisabled() {
        Store store = Store.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), "Tienda Test", "tienda-test",
                null, null, null, null, null, null,
                false, true, new BigDecimal("5.00"), "PEN", null,
                StoreStatus.ACTIVE, null, null
        );

        assertThatThrownBy(() -> CheckoutFulfillmentValidator.validate(
                CheckoutFulfillmentType.PICKUP, null, store
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("retiro en tienda");
    }
}
