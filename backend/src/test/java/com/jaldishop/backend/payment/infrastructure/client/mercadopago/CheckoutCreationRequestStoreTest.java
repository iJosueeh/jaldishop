package com.jaldishop.backend.payment.infrastructure.client.mercadopago;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CheckoutCreationRequestStoreTest {
    @Test void durableOrdersSnapshotMarksUncertaintyBeforeReturningBody() {
        var jdbc = mock(JdbcTemplate.class);
        var id = UUID.randomUUID();
        var body = "{}".getBytes(StandardCharsets.UTF_8);
        when(jdbc.update(contains("INSERT INTO checkout_creation_requests"), eq("{}"), eq(id), eq("ORDERS"), eq("key"))).thenReturn(1);
        when(jdbc.update(contains("UPDATE payment_attempts"), eq(id))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(String.class), eq(id))).thenReturn("{}");
        assertThat(new CheckoutCreationRequestStore(jdbc).saveBeforeSend(id, "ORDERS", "key", body)).isEqualTo(body);
        var order = inOrder(jdbc);
        order.verify(jdbc).update(contains("INSERT INTO checkout_creation_requests"), eq("{}"), eq(id), eq("ORDERS"), eq("key"));
        order.verify(jdbc).update(contains("UPDATE payment_attempts"), eq(id));
        order.verify(jdbc).queryForObject(anyString(), eq(String.class), eq(id));
    }

    @Test void duplicateSnapshotCannotReturnBodyForAnotherPost() {
        var jdbc = mock(JdbcTemplate.class);
        assertThatThrownBy(() -> new CheckoutCreationRequestStore(jdbc).saveBeforeSend(UUID.randomUUID(), "ORDERS", "key", new byte[]{123,125}))
                .isInstanceOf(com.jaldishop.backend.shared.exception.ConflictException.class);
        verify(jdbc, never()).queryForObject(anyString(), eq(String.class), any(UUID.class));
        verify(jdbc, never()).update(contains("UPDATE payment_attempts"), any(UUID.class));
    }
}
