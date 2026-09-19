---
description: API de Configuración de Capacidad - JaldiShop
---

# API de Configuración de Capacidad

[![Endpoint](https://img.shields.io/badge/Endpoints-REST-blue)](#endpoints)
[![Auth](https://img.shields.io/badge/Auth-JWT-green)](#autenticaci%C3%B3n)
[![Role](https://img.shields.io/badge/Role-MERCHANT-orange)](#roles)

## Endpoints

### 1. Crear configuración de capacidad

```
POST /api/v1/capacity-configurations
```

**Autenticación:** Bearer JWT (MERCHANT)

**Request:**
```json
{
  "dayOfWeek": 1,
  "startTime": "09:00",
  "endTime": "17:00",
  "maxCapacity": 15
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `dayOfWeek` | int | Sí | Día de la semana (0=Domingo, 6=Sábado) |
| `startTime` | string | No | Hora inicio (HH:mm) - null = todo el día |
| `endTime` | string | No | Hora fin (HH:mm) - null = todo el día |
| `maxCapacity` | int | Sí | Capacidad máxima (>= 0) |

**Response 201 Created:**
```json
{
  "id": "uuid",
  "storeId": "uuid",
  "dayOfWeek": 1,
  "startTime": "09:00",
  "endTime": "17:00",
  "maxCapacity": 15,
  "status": "ACTIVE",
  "createdAt": "2026-01-10T08:00:00Z",
  "updatedAt": "2026-01-10T08:00:00Z"
}
```

**Errores:**
- `400 Bad Request` - Datos inválidos (startTime >= endTime, dayOfWeek fuera de rango, etc.)
- `403 Forbidden` - Usuario sin rol MERCHANT
- `409 Conflict` - Solapamiento de franjas horarias (`CAPACITY_OVERLAP`)

---

### 2. Listar configuraciones de mi tienda

```
GET /api/v1/capacity-configurations
```

**Autenticación:** Bearer JWT (MERCHANT)

**Response 200 OK:**
```json
[
  {
    "id": "uuid",
    "storeId": "uuid",
    "dayOfWeek": 0,
    "startTime": null,
    "endTime": null,
    "maxCapacity": 10,
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  {
    "id": "uuid",
    "storeId": "uuid",
    "dayOfWeek": 1,
    "startTime": "09:00",
    "endTime": "17:00",
    "maxCapacity": 20,
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  }
]
```

---

### 3. Actualizar configuración de capacidad

```
PUT /api/v1/capacity-configurations/{id}
```

**Autenticación:** Bearer JWT (MERCHANT)

**Request:** Mismo schema que crear configuración.

**Response 200 OK:** Objeto configuración actualizado.

**Errores:**
- `404 Not Found` - Configuración no encontrada o no pertenece a la tienda
- `409 Conflict` - Solapamiento de franjas horarias

---

### 4. Activar configuración

```
PATCH /api/v1/capacity-configurations/{id}/activate
```

**Autenticación:** Bearer JWT (MERCHANT)

**Response 200 OK:** Configuración con `status: "ACTIVE"`

---

### 5. Desactivar configuración

```
PATCH /api/v1/capacity-configurations/{id}/deactivate
```

**Autenticación:** Bearer JWT (MERCHANT)

**Response 200 OK:** Configuración con `status: "INACTIVE"`

---

## Estados

| Estado | Descripción |
|--------|-------------|
| `ACTIVE` | Configuración activa y aplicable |
| `INACTIVE` | Configuración desactivada temporalmente |

## Reglas de Dominio

1. **Franjas horarias:** Si se especifica `startTime`, se debe especificar `endTime` y viceversa
2. **Orden temporal:** `startTime` debe ser anterior a `endTime`
3. **Solapamiento:** No se permite solapamiento de franjas horarias en el mismo día y tienda
4. **Full day:** Si `startTime` y `endTime` son `null`, aplica todo el día (no se verifica solapamiento)
5. **Propiedad:** Solo el dueño de la tienda puede gestionar sus configuraciones

## Ejemplo de Uso

```bash
# Crear configuración para lunes 9am-5pm con capacidad 15
curl -X POST http://localhost:8080/api/v1/capacity-configurations \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"dayOfWeek": 1, "startTime": "09:00", "endTime": "17:00", "maxCapacity": 15}'

# Listar configuraciones de mi tienda
curl http://localhost:8080/api/v1/capacity-configurations \
  -H "Authorization: Bearer <token>"

# Desactivar configuración
curl -X PATCH http://localhost:8080/api/v1/capacity-configurations/<id>/deactivate \
  -H "Authorization: Bearer <token>"
```
