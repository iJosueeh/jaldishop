---
description: API de Consulta de Capacidad Efectiva - JaldiShop
---

# API de Consulta de Capacidad Efectiva

[![Endpoint](https://img.shields.io/badge/Endpoints-REST-blue)](#endpoints)
[![Auth](https://img.shields.io/badge/Auth-JWT-green)](#autenticación)
[![Role](https://img.shields.io/badge/Role-MERCHANT-orange)](#roles)

La **API de Capacidad Efectiva** permite a un comerciante consultar cuál es la capacidad operativa real de su tienda para una **fecha y franja horaria** determinadas, resolviendo automáticamente la jerarquía entre la **configuración base** recurrente y las **excepciones** definidas para esa fecha.

> 📌 **Regla de Negocio Central (RN-CAP-01):** La capacidad efectiva se resuelve como `Excepción > Configuración Base`. Si no existe ninguna cobertura vigente, la tienda se considera **cerrada** (`effectiveCapacity = 0`, `source = NONE`).

---

## Endpoints

### 1. Consultar capacidad efectiva

```http
GET /api/v1/capacity/effective?date=2026-09-22&startTime=12:30&endTime=13:30
```

**Autenticación:** Bearer JWT (Rol `MERCHANT`)

**Query Parameters:**

| Parámetro | Tipo | Requerido | Descripción |
|---|:---:|:---:|---|
| `date` | string (LocalDate: `YYYY-MM-DD`) | **Sí** | Fecha a consultar |
| `startTime` | string (`HH:mm` o `HH:mm:ss`) | **Sí** | Inicio de la franja horaria |
| `endTime` | string (`HH:mm` o `HH:mm:ss`) | **Sí** | Fin de la franja horaria |

> El `storeId` no se envía: se resuelve automáticamente del comerciante autenticado.

**Response `200 OK`:**
```json
{
  "storeId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "serviceDate": "2026-09-22",
  "startTime": "12:30:00",
  "endTime": "13:30:00",
  "effectiveCapacity": 4,
  "source": "EXCEPTION"
}
```

| Campo | Tipo | Descripción |
|---|:---:|---|
| `storeId` | string (UUID) | Tienda del comerciante autenticado |
| `serviceDate` | string (LocalDate) | Fecha consultada (echo) |
| `startTime` | string (LocalTime) | Inicio de franja consultado (echo) |
| `endTime` | string (LocalTime) | Fin de franja consultado (echo) |
| `effectiveCapacity` | integer | Capacidad efectiva (`>= 0`); `0` = cerrado/sin cobertura |
| `source` | enum | **`EXCEPTION`** · **`BASE`** · **`NONE`** |

**Errores:**
* `400 Bad Request` — Formato de parámetros inválido (fecha u hora mal formateada).
* `403 Forbidden` — Usuario no autenticado o sin rol `MERCHANT`.
* `422 Unprocessable Entity` — Falta la fecha, falta la franja (`startTime`/`endTime`) o `startTime >= endTime` (código `INVALID_CAPACITY_QUERY`).

---

## Semántica del campo `source`

| `source` | Significado |
|---|---|
| `EXCEPTION` | Se aplicó una excepción vigente para esa fecha (`effectiveCapacity = exceptionCapacity`) |
| `BASE` | No hay excepción; se aplicó la configuración base recurrente del día (`effectiveCapacity = maxCapacity`) |
| `NONE` | No hay configuración base ni excepción aplicable → tienda cerrada (`effectiveCapacity = 0`) |

---

## Algoritmo de Resolución

Se evalúa en este orden estricto, **priorizando la excepción** por sobre la configuración base:

1. **Excepciones** (`CapacityException` activas para `storeId + serviceDate`):
   - Aplica la primera que cubra la franja solicitada (`startTime >= cfgStart && endTime <= cfgEnd`).
   - Una excepción **sin franja (full day)** tiene prioridad sobre cualquier excepción con franja de la misma fecha.
2. **Configuración base** (`CapacityConfiguration` activas para `storeId + day_of_week`):
   - `day_of_week` se deriva de la fecha: `getDayOfWeek().getValue() % 7` (`0` = Domingo, `1` = Lunes … `6` = Sábado).
   - Dentro de la base, la configuración **con franja horaria** tiene prioridad sobre la de día completo.
3. **Sin cobertura:** `effectiveCapacity = 0` y `source = NONE`.

**Reglas de dominio:**
1. Las entidades con estado `INACTIVE` se ignoran en el cálculo.
2. La franja solicitada debe estar **contenida** dentro de la franja de la configuración/excepción aplicable.
3. Capacidad `0` no significa error: representa a un negocio o franja **cerrado**.
4. El cálculo es **multitenant**: solo aplica a la tienda del comerciante autenticado.

---

## Ejemplo de Integración cURL

```bash
# Consultar capacidad efectiva para el 22/09/2026 de 12:30 a 13:30
curl -X GET "http://localhost:8080/api/v1/capacity/effective?date=2026-09-22&startTime=12:30&endTime=13:30" \
  -H "Authorization: Bearer <TOKEN_JWT>"
```