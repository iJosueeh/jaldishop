package com.jaldishop.backend.notification.infrastructure.persistence.mapper;

import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.domain.NotificationStatus;
import com.jaldishop.backend.notification.domain.NotificationType;
import com.jaldishop.backend.notification.infrastructure.persistence.entity.NotificationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPersistenceMapperTest {

    private NotificationPersistenceMapper mapper;

    private UUID testId;
    private UUID testUserId;
    private Instant testCreatedAt;
    private Instant testReadAt;

    @BeforeEach
    void setUp() {
        mapper = new NotificationPersistenceMapper();
        testId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCreatedAt = Instant.parse("2026-01-10T08:00:00Z");
        testReadAt = Instant.parse("2026-01-10T12:00:00Z");
    }

    @Test
    @DisplayName("toEntity() debe mapear correctamente todos los campos desde el dominio")
    void toEntityShouldMapAllFields() {
        Notification notification = Notification.reconstitute(
                testId, testUserId, NotificationType.NEW_ORDER, "Nuevo pedido",
                "Tienes un nuevo pedido", NotificationStatus.READ, testCreatedAt, testReadAt
        );

        NotificationEntity entity = mapper.toEntity(notification);

        assertNotNull(entity);
        assertEquals(testId, entity.getId());
        assertEquals(testUserId, entity.getUserId());
        assertEquals(NotificationType.NEW_ORDER, entity.getType());
        assertEquals("Nuevo pedido", entity.getTitle());
        assertEquals("Tienes un nuevo pedido", entity.getMessage());
        assertEquals(NotificationStatus.READ, entity.getStatus());
        assertEquals(testCreatedAt, entity.getCreatedAt());
        assertEquals(testReadAt, entity.getReadAt());
    }

    @Test
    @DisplayName("toDomain() debe mapear correctamente todos los campos desde la entidad JPA")
    void toDomainShouldMapAllFields() {
        NotificationEntity entity = new NotificationEntity(
                testId, testUserId, NotificationType.LOW_STOCK, "Stock bajo",
                "Producto por agotarse", NotificationStatus.UNREAD, testCreatedAt, null
        );

        Notification domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(testId, domain.getId());
        assertEquals(testUserId, domain.getUserId());
        assertEquals(NotificationType.LOW_STOCK, domain.getType());
        assertEquals("Stock bajo", domain.getTitle());
        assertEquals("Producto por agotarse", domain.getMessage());
        assertEquals(NotificationStatus.UNREAD, domain.getStatus());
        assertEquals(testCreatedAt, domain.getCreatedAt());
        assertNull(domain.getReadAt());
    }

    @Test
    @DisplayName("toEntity() y toDomain() deben soportar readAt nulo")
    void shouldHandleNullReadAt() {
        Notification notification = Notification.reconstitute(
                testId, testUserId, NotificationType.SYSTEM, "Aviso",
                "Mensaje de sistema", NotificationStatus.UNREAD, testCreatedAt, null
        );

        NotificationEntity entity = mapper.toEntity(notification);
        assertNull(entity.getReadAt());

        Notification domainFromEntity = mapper.toDomain(entity);
        assertNull(domainFromEntity.getReadAt());
    }

    @Test
    @DisplayName("Roundtrip: toDomain(toEntity(domain)) debe preservar todos los campos")
    void roundtripShouldPreserveAllFields() {
        Notification original = Notification.reconstitute(
                testId, testUserId, NotificationType.ORDER_STATUS_CHANGED, "Estado cambiado",
                "Tu pedido fue entregado", NotificationStatus.READ, testCreatedAt, testReadAt
        );

        NotificationEntity entity = mapper.toEntity(original);
        Notification roundtripped = mapper.toDomain(entity);

        assertEquals(original.getId(), roundtripped.getId());
        assertEquals(original.getUserId(), roundtripped.getUserId());
        assertEquals(original.getType(), roundtripped.getType());
        assertEquals(original.getTitle(), roundtripped.getTitle());
        assertEquals(original.getMessage(), roundtripped.getMessage());
        assertEquals(original.getStatus(), roundtripped.getStatus());
        assertEquals(original.getCreatedAt(), roundtripped.getCreatedAt());
        assertEquals(original.getReadAt(), roundtripped.getReadAt());
    }
}
