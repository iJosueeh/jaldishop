---
description: API de Excepciones de Capacidad - JaldiShop
---

# API de Excepciones de Capacidad

[![Endpoint](https://img.shields.io/badge/Endpoints-REST-blue)](#endpoints)
[![Auth](https://img.shields.io/badge/Auth-JWT-green)](#autenticación)
[![Role](https://img.shields.io/badge/Role-MERCHANT-orange)](#roles)

La **API de Excepciones de Capacidad** permite a un comerciante definir ajustes temporales y puntuales de capacidad para fechas específicas (ej. feriados, eventos especiales, campañas de alta demanda o días de mantenimiento).

> 📌 **Regla de Negocio Central (RN-CAP-02):** La excepción de capacidad **reemplaza por completo** la configuración base recurrente para esa fecha y franja horaria. **No se suma a ella**. Si se define capacidad `0`, el negocio o franja se considera cerrado.

---

## Endpoints

### 1. Crear excepción de capacidad

```http
POST /api/v1/capacity-exceptions
```

**Autenticación:** Bearer JWT (Rol `MERCHANT`)

**Request Body:**
```json
{
  "serviceDate": "2026-12-25",
  "startTime": "09:00",
  "endTime": "14:00",
  "exceptionCapacity": 0,
  "reason": "Cerrado por Navidad"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|:---:|:---:|---|
| `serviceDate` | string (LocalDate: `YYYY-MM-DD`) | **Sí** | Fecha de la excepción (debe ser `>= hoy`) |
| `startTime` | string (`HH:mm` o `HH:mm:ss`) | No | Hora de inicio de la franja (null = todo el día) |
| `endTime` | string (`HH:mm` o `HH:mm:ss`) | No | Hora de fin de la franja (null = todo el día) |
| `exceptionCapacity` | integer | **Sí** | Capacidad efectiva para ese día/franja (`>= 0`) |
| `reason` | string | No | Motivo de la excepción (máx. 255 caracteres) |

**Response `201 Created`:**
```json
{
  "id": "c3f7b8a1-4e2a-4b9e-9b3f-1a2b3c4d5e6f",
  "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "serviceDate": "2026-12-25",
  "startTime": "09:00:00",
  "endTime": "14:00:00",
  "exceptionCapacity": 0,
  "reason": "Cerrado por Navidad",
  "status": "ACTIVE",
  "createdAt": "2026-09-22T12:00:00",
  "updatedAt": "2026-09-22T12:00:00"
}
```

**Errores:**
* `400 Bad Request` — Datos inválidos (fecha pasada, `startTime >= endTime`, capacidad negativa, etc.).
* `403 Forbidden` — Usuario no autenticado o sin rol `MERCHANT`.
* `409 Conflict` — Solapamiento de franjas de excepción para la misma fecha y tienda.

---

### 2. Listar excepciones de la tienda

```http
GET /api/v1/capacity-exceptions
```

**Autenticación:** Bearer JWT (Rol `MERCHANT`)

**Response `200 OK`:**
```json
[
  {
    "id": "c3f7b8a1-4e2a-4b9e-9b3f-1a2b3c4d5e6f",
    "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "serviceDate": "2026-12-25",
    "startTime": "09:00:00",
    "endTime": "14:00:00",
    "exceptionCapacity": 0,
    "reason": "Cerrado por Navidad",
    "status": "ACTIVE",
    "createdAt": "2026-09-22T12:00:00",
    "updatedAt": "2026-09-22T12:00:00"
  },
  {
    "id": "e9b2c3d4-5f6a-7b8c-9d0e-1f2a3b4c5d6e",
    "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "serviceDate": "2026-02-14",
    "startTime": "10:00:00",
    "endTime": "18:00:00",
    "exceptionCapacity": 40,
    "reason": "Campaña San Valentín (Capacidad Ampliada)",
    "status": "ACTIVE",
    "createdAt": "2026-09-22T12:05:00",
    "updatedAt": "2026-09-22T12:05:00"
  }
]
```

---

### 3. Actualizar excepción de capacidad

```http
PUT /api/v1/capacity-exceptions/{id}
```

**Autenticación:** Bearer JWT (Rol `MERCHANT`)

**Request Body:**
```json
{
  "serviceDate": "2026-02-14",
  "startTime": "09:00",
  "endTime": "20:00",
  "exceptionCapacity": 50,
  "reason": "Campaña San Valentín (Capacidad Máxima 50)"
}
```

**Response `200 OK`:** Objeto `CapacityExceptionResponse` con los datos actualizados.

**Errores:**
* `404 Not Found` — Excepción no encontrada o no pertenece a la tienda del comerciante.
* `400 Bad Request` — Datos inválidos en los campos de actualización.
* `409 Conflict` — Conflicto de solapamiento horario con otra excepción activa.

---

### 4. Activar excepción de capacidad

```http
PATCH /api/v1/capacity-exceptions/{id}/activate
```

**Autenticación:** Bearer JWT (Rol `MERCHANT`)

**Response `200 OK`:**
```json
{
  "id": "c3f7b8a1-4e2a-4b9e-9b3f-1a2b3c4d5e6f",
  "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "status": "ACTIVE",
  "updatedAt": "2026-09-22T12:10:00"
}
```

---

### 5. Desactivar excepción de capacidad

```http
PATCH /api/v1/capacity-exceptions/{id}/deactivate
```

**Autenticación:** Bearer JWT (Rol `MERCHANT`)

**Response `200 OK`:**
```json
{
  "id": "c3f7b8a1-4e2a-4b9e-9b3f-1a2b3c4d5e6f",
  "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "status": "INACTIVE",
  "updatedAt": "2026-09-22T12:12:00"
}
```

---

## Estados del Ciclo de Vida

| Estado | Descripción | Impacto Operativo |
|---|---|---|
| `ACTIVE` | Excepción activa y vigente | Sustituye la configuración base para la fecha y franja configurada. |
| `INACTIVE` | Excepción pausada o inactiva | Se ignora en el cálculo; el sistema evalúa la configuración base del día. |

---

## Reglas de Dominio

1. **Reemplazo Directo:** La excepción anula y reemplaza cualquier `CapacityConfiguration` base aplicable a ese día de la semana.
2. **Capacidad Cero:** Si `exceptionCapacity == 0`, la franja o el día se consideran cerrados sin cupos disponibles.
3. **Consistencia de Rango:** Si se especifica `startTime`, debe proveerse `endTime` y satisfacer `startTime < endTime`.
4. **Restricción Multitienda:** Cada excepción está aislada por `store_id`, garantizando que ningún comercio acceda o altere fechas de otro.
5. **No Retroactividad:** Las fechas de excepción no pueden crearse en el pasado (`serviceDate >= today`).

---

## Ejemplo de Integración cURL

```bash
# 1. Crear excepción de cierre por Feriado
curl -X POST http://localhost:8080/api/v1/capacity-exceptions \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceDate": "2026-12-25",
    "startTime": "00:00",
    "endTime": "23:59",
    "exceptionCapacity": 0,
    "reason": "Cerrado por Feriado Navideño"
  }'

# 2. Listar todas las excepciones
curl -X GET http://localhost:8080/api/v1/capacity-exceptions \
  -H "Authorization: Bearer <TOKEN_JWT>"

# 3. Desactivar temporalmente una excepción
curl -X PATCH http://localhost:8080/api/v1/capacity-exceptions/{id}/deactivate \
  -H "Authorization: Bearer <TOKEN_JWT>"
```
