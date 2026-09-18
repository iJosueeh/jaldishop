# 📡 API de Notificaciones — Endpoints

### JaldiShop — Gestión de Notificaciones Persistentes (BE-10)

[![Estado](https://img.shields.io/badge/Estado-Completado-success?style=for-the-badge)](./api-notificaciones.md)
[![Sprint](https://img.shields.io/badge/Sprint-02-orange?style=for-the-badge)](./api-notificaciones.md)
[![Autor](https://img.shields.io/badge/Autor-Mia-lightgrey?style=for-the-badge)](./api-notificaciones.md)

---

`📍 Docs` > `04-Diseño` > **API de Notificaciones**
[⬅ Contrato API Errores](./contrato-api-errores.md) | [🏠 Índice General](../../README.md) | [Modelo ER ➡](./modelo-er.md)

---

## 1. Visión General

Endpoints REST para la consulta y gestión de notificaciones persistentes de los usuarios. Las notificaciones se crean internamente por eventos del sistema y se consumen vía estos endpoints.

> **Nota:** Este módulo NO expone endpoint de creación. Las notificaciones se crean internamente mediante `CreateNotificationService` (futuro: eventos AFTER_COMMIT de Pedido, Inventario).

### Autenticación

Todos los endpoints requieren header:

```
Authorization: Bearer <jwt_token>
```

---

## 2. Endpoints

### 2.1 Listar Notificaciones del Usuario

```
GET /api/v1/notifications?page=0&size=20
```

**Parámetros de consulta:**

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `page` | `int` | `0` | Número de página (0-indexed) |
| `size` | `int` | `20` | Cantidad por página |

**Respuesta 200 OK:**

```json
[
  {
    "id": "uuid",
    "userId": "uuid",
    "type": "NEW_ORDER",
    "title": "Nuevo pedido recibido",
    "message": "El pedido #1234 está listo para preparar",
    "status": "UNREAD",
    "createdAt": "2026-09-17T21:16:10.255Z",
    "readAt": null
  }
]
```

**Ordering:** `created_at DESC` (más recientes primero)

---

### 2.2 Contar Notificaciones No Leídas

```
GET /api/v1/notifications/unread/count
```

**Respuesta 200 OK:**

```json
{
  "unreadCount": 5
}
```

---

### 2.3 Marcar Notificación como Leída

```
PATCH /api/v1/notifications/{id}/read
```

**Path Variables:**

| Variable | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | ID de la notificación |

**Respuesta 200 OK:**

```json
{
  "id": "uuid",
  "userId": "uuid",
  "type": "NEW_ORDER",
  "title": "Nuevo pedido recibido",
  "message": "El pedido #1234 está listo para preparar",
  "status": "READ",
  "createdAt": "2026-09-17T21:16:10.255Z",
  "readAt": "2026-09-17T22:00:00.000Z"
}
```

**Respuesta 404 Not Found:**

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Notification con identificador 'uuid' no fue encontrado"
}
```

---

## 3. Errores

| HTTP Status | `code` | Causa |
|---|---|---|
| `401` | `UNAUTHORIZED` | Token no proporcionado o inválido |
| `404` | `RESOURCE_NOT_FOUND` | Notificación no existe o no pertenece al usuario |
| `500` | `INTERNAL_SERVER_ERROR` | Error no controlado |

---

## 4. Enums

### NotificationType

| Valor | Descripción |
|---|---|
| `NEW_ORDER` | Nuevo pedido (para merchant) |
| `ORDER_STATUS_CHANGED` | Cambio de estado del pedido (para customer) |
| `LOW_STOCK` | Stock bajo (para merchant) |
| `SYSTEM` | Mensaje de la plataforma |

### NotificationStatus

| Valor | Descripción |
|---|---|
| `UNREAD` | No leída (`readAt = null`) |
| `READ` | Leída (`readAt ≠ null`) |

---

## 5. Endpoints Futuros (fuera de alcance BE-10)

| Método | Ruta | Descripción |
|---|---|---|
| `WS` | `/user/queue/notifications` | Entrega en tiempo real vía WebSocket/STOMP |

> Las notificaciones se persisten primero y se entregan por WebSocket después (pattern AFTER_COMMIT). Si el usuario no está conectado, la notificación queda como UNREAD y se consulta vía REST.

---

## 6. Paquete

```
com.jaldishop.backend.notification
├── domain/
├── application/
├── infrastructure/persistence/
└── web/
    ├── controller/NotificationController.java
    └── dto/NotificationResponse.java
```
