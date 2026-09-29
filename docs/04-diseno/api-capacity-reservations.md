---
description: API de Reservas Temporales de Capacidad (Hold) - JaldiShop
---

# API de Reservas Temporales de Capacidad (Hold)

[![Endpoint](https://img.shields.io/badge/Endpoints-REST-blue)](#endpoints)
[![Auth](https://img.shields.io/badge/Auth-JWT-green)](#autenticación)
[![Role](https://img.shields.io/badge/Role-CUSTOMER-orange)](#roles)

La **API de Reservas Temporales de Capacidad** permite a un cliente (rol `CUSTOMER`) consultar la **disponibilidad** de una tienda para una fecha y franja horaria, y **reservar temporalmente** un cupo (Hold) al momento de iniciar el Checkout.

> 📌 **Regla de Negocio Central (RN-CAP-07):** `Capacidad Disponible = Capacidad Efectiva - Capacidad Reservada (Hold) - Capacidad Comprometida`.
>
> 📌 **Regla de Negocio Central (RN-RES-03/05):** La reserva temporal tiene una vigencia de **10 minutos**. Al expirar, o al liberarse el checkout, el cupo retornará inmediatamente a la disponibilidad (expiración *lazy*: no se requiere un job; los conteos ignoran automáticamente las reservas `ACTIVE` vencidas).

---

## Roles

| Rol | Acceso |
|---|---|
| `CUSTOMER` | Todas las operaciones de esta API |
| `MERCHANT` / `ADMIN` | `403 Forbidden` |

---

## Endpoints

### 1. Consultar disponibilidad de una franja

```http
GET /api/v1/capacity/availability?storeId=a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d&date=2026-09-22&startTime=10:00&endTime=12:00
```

**Autenticación:** Bearer JWT (Rol `CUSTOMER`)

**Query Parameters:**

| Parámetro | Tipo | Requerido | Descripción |
|---|:---:|:---:|---|
| `storeId` | string (UUID) | **Sí** | Tienda a consultar |
| `date` | string (LocalDate: `YYYY-MM-DD`) | **Sí** | Fecha del servicio |
| `startTime` | string (`HH:mm` o `HH:mm:ss`) | **Sí** | Inicio de la franja horaria |
| `endTime` | string (`HH:mm` o `HH:mm:ss`) | **Sí** | Fin de la franja horaria |

**Response `200 OK`:**
```json
{
  "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "serviceDate": "2026-09-22",
  "startTime": "10:00:00",
  "endTime": "12:00:00",
  "effectiveCapacity": 20,
  "reservedCapacity": 4,
  "committedCapacity": 2,
  "availableCapacity": 14
}
```

| Campo | Tipo | Descripción |
|---|:---:|---|
| `storeId` | string (UUID) | Tienda consultada |
| `serviceDate` | string (LocalDate) | Fecha consultada (echo) |
| `startTime` | string (LocalTime) | Inicio de franja consultado (echo) |
| `endTime` | string (LocalTime) | Fin de franja consultado (echo) |
| `effectiveCapacity` | integer | Capacidad efectiva según excepción/base (`>= 0`; `0` = cerrado) |
| `reservedCapacity` | integer | Cupos reservados (Hold ACTIVE no vencido + `PAYMENT_PROTECTED`) |
| `committedCapacity` | integer | Cupos comprometidos por pedidos confirmados (`COMMITTED`) |
| `availableCapacity` | integer | `max(0, effective - reserved - committed)` |

**Errores:**
* `400 Bad Request` — Formato de parámetros inválido (fecha u hora mal formateada).
* `403 Forbidden` — Usuario sin rol `CUSTOMER`.
* `422 Unprocessable Entity` — Falta `storeId`, falta la fecha, falta la franja (`startTime`/`endTime`) o `startTime >= endTime` (código `INVALID_CAPACITY_QUERY`).

---

### 2. Crear reserva temporal (Hold). *Iniciar Checkout*

```http
POST /api/v1/capacity/reservations
Content-Type: application/json

{
  "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "serviceDate": "2026-09-22",
  "startTime": "10:00:00",
  "endTime": "12:00:00"
}
```

**Autenticación:** Bearer JWT (Rol `CUSTOMER`). El `userId` del cliente se toma del token (`principal.userId()`), no del body.

**Response `201 Created`:**
```json
{
  "id": "f3a2c1b0-9d8e-7f6a-5b4c-3d2e1f0a9b8c",
  "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "userId": "8a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d",
  "serviceDate": "2026-09-22",
  "startTime": "10:00:00",
  "endTime": "12:00:00",
  "status": "ACTIVE",
  "expiresAt": "2026-09-22T10:10:00Z",
  "paymentProtectionExpiresAt": null,
  "createdAt": "2026-09-22T10:00:00Z",
  "updatedAt": "2026-09-22T10:00:00Z"
}
```

**Reglas de negocio aplicadas:**
- La reserva solo se crea si `Capacidad Disponible >= 1` (RN-RES-02) y `Capacidad Efectiva > 0`.
- Cada reserva consume **exactamente 1 cupo** (RN-CAP-06).
- **Un pedido = un cupo:** un cliente no puede tener una reserva activa para la misma franja (RN-CAP-06 / RN-RES-06). Se rechaza con `409 ALREADY_RESERVED`.
- Vigencia inicial: **10 minutos** desde su creación (`expiresAt = createdAt + 10 min`, RN-RES-03).
- **Concurrencia:** se ejecuta dentro de una transacción corta con **bloqueo pesimista** (`PESSIMISTIC_WRITE`) sobre la fila de la **configuración base o excepción ganadora** (el "registro estable" de `arquitectura-sistema.md §16.2`); tras adquirir el lock se **re-cuenta** la capacidad para proteger el último cupo del *race condition*.

**Errores:**
* `400 Bad Request` — Cuerpo JSON inválido o campos obligatorios faltantes (`storeId`, `serviceDate`, `startTime`, `endTime`).
* `403 Forbidden` — Usuario sin rol `CUSTOMER`.
* `409 Conflict` — Códigos:
  * `CAPACITY_UNAVAILABLE` — La tienda no tiene capacidad efectiva para la franja (`effectiveCapacity = 0`) o el registro de capacidad dejó de existir.
  * `CAPACITY_EXHAUSTED` — `effectiveCapacity - reserved - committed = 0` (franja saturada, RN-CAP-08 *Hard Cap*).
  * `ALREADY_RESERVED` — El cliente ya tiene una reserva activa para esa misma franja.
* `422 Unprocessable Entity` — Parámetros de franja inválidos (`INVALID_CAPACITY_QUERY`).

---

### 3. Consultar una reserva propia

```http
GET /api/v1/capacity/reservations/{id}
```

**Autenticación:** Bearer JWT (Rol `CUSTOMER`)

Solo el **propietario** (`userId` de la reserva == `principal.userId()`) puede verla.

**Response `200 OK`:** Mismo esquema que el `201` de creación.

Si la reserva está vencida, se persiste marcándola como `EXPIRED` (expiración *lazy*).

**Errores:**
* `403 Forbidden` — Usuario sin rol `CUSTOMER`.
* `404 Not Found` — Reserva inexistente **o perteneciente a otro cliente** (código `RESOURCE_NOT_FOUND`, sin fuga de información).

---

### 4. Liberar reserva (cancelar Checkout)

```http
DELETE /api/v1/capacity/reservations/{id}
```

**Autenticación:** Bearer JWT (Rol `CUSTOMER`)

Libera el cupo retenido (RN-RES-05). El cupo retornará a la disponibilidad. La operación es **idempotente**: liberar una reserva ya `RELEASED` devuelve `200 OK`. Si la reserva está vencida, primero se marca `EXPIRED`.

**Response `200 OK`:** Mismo esquema que el `201` de creación, con `status`:
* `RELEASED` — Liberada correctamente desde `ACTIVE` o `PAYMENT_PROTECTED`.
* `EXPIRED` — Vencida antes de liberarse (ya no consume cupo).

**Errores:**
* `403 Forbidden` — Usuario sin rol `CUSTOMER`.
* `404 Not Found` — Reserva inexistente o ajena (código `RESOURCE_NOT_FOUND`).
* `409 Conflict` — `RESERVATION_COMMITTED`: la reserva ya fue comprometida por un pedido confirmado y **no** puede liberarse mediante este endpoint (la liberación de una `COMMITTED` corresponde a la cancelación del pedido).

---

## Estados de la Reserva

| Estado | Consume cupo | Descripción |
|---|:---:|---|
| `ACTIVE` | Sí | Hold temporal creado al iniciar checkout. Vigencia máxima 10 min. |
| `PAYMENT_PROTECTED` | Sí | El cliente inició válidamente el pago (transición ACTIVA). Protección máxima adicional: 10 min. |
| `COMMITTED` | Sí | Compra confirmada y pedido creado. Consume capacidad definitivamente. |
| `EXPIRED` | No | El hold inicial venció sin iniciar pago. |
| `RELEASED` | No | Liberada por cancelación, fallo u otra liberación válida. |

**Transiciones válidas** (según `modelo-dominio.md`):
```
ACTIVE → PAYMENT_PROTECTED
ACTIVE → EXPIRED
ACTIVE → RELEASED
PAYMENT_PROTECTED → COMMITTED
PAYMENT_PROTECTED → RELEASED
COMMITTED → RELEASED  (solo cancelación del pedido)
```

> Las transiciones `PAYMENT_PROTECTED`, `COMMITTED` y la cancelación de pedidos **NO** se exponen en esta API; se habilitarán en las tarjetas de Pagos y Pedidos. Este endpoint expone `ACTIVE → EXPIRED`, `ACTIVE → RELEASED`, `PAYMENT_PROTECTED → RELEASED` y la comprobación defensiva `RESERVATION_COMMITTED`.

---

## Concurrencia y Contadores

### Fila bloqueada (registro estable)

Siguiendo `arquitectura-sistema.md §16.2`, el registro estable que se bloquea con `PESSIMISTIC_WRITE` es **la fila de la configuración base o de la excepción que resolvió la capacidad efectiva** para `storeId + fecha + franja`. Esto serializa la creación de reservas sobre una misma franja/capacidad.

### Consultas de conteo (franja exacta)

Los contadores consultan **exactamente** `storeId + serviceDate + startTime + endTime` (alineado con el índice compuesto `idx_capacity_reservations_store_date_time`):

* `reserved` = `ACTIVE` con `expiresAt > now` **+** `PAYMENT_PROTECTED`.
* `committed` = `COMMITTED`.
* Excluyen `EXPIRED` y `RELEASED` (no consumen capacidad, `modelo-er.md`).

---

## Ejemplo de Integración cURL

```bash
# 1) Consultar disponibilidad
curl -X GET "http://localhost:8080/api/v1/capacity/availability?storeId=a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d&date=2026-09-22&startTime=10:00&endTime=12:00" \
  -H "Authorization: Bearer <TOKEN_JWT>"

# 2) Reservar un cupo (iniciar checkout)
curl -X POST "http://localhost:8080/api/v1/capacity/reservations" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{"storeId":"a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d","serviceDate":"2026-09-22","startTime":"10:00:00","endTime":"12:00:00"}'

# 3) Liberar el cupo (cancelar checkout)
curl -X DELETE "http://localhost:8080/api/v1/capacity/reservations/f3a2c1b0-9d8e-7f6a-5b4c-3d2e1f0a9b8c" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```