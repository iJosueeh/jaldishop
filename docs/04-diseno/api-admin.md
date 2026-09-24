# 🛡️ Especificación de API: Módulo de Administración (BE-ADMIN-01)

### JaldiShop — Contrato y Especificación de Endpoints Administrativos para Rol `ADMIN`

[![Estado](https://img.shields.io/badge/Estado-Completado-success?style=for-the-badge)](./api-admin.md)
[![Rol](https://img.shields.io/badge/Rol-ADMIN-red?style=for-the-badge)](./api-admin.md)
[![Capa](https://img.shields.io/badge/Capa-REST_API-blue?style=for-the-badge)](./api-admin.md)

---

`📍 Docs` > `04-Diseño` > **API Administración (BE-ADMIN-01)**  
[🏠 Volver al Índice General](../../README.md) | [Sprint 03 ➡](../06-scrum/sprint-03.md) | [Contrato de Errores ➡](./contrato-api-errores.md)

---

## 1. Visión General

El módulo de administración (`/api/v1/admin/**`) expone los endpoints de supervisión global de usuarios, comerciantes y tiendas para la plataforma JaldiShop. 

### 🔒 Reglas de Seguridad & Control de Acceso:
* **Autenticación:** Obligatoria mediante JWT (`Authorization: Bearer <token>`).
* **Autorización:** Estrictamente reservado para usuarios con rol `ADMIN` (`ROLE_ADMIN`).
* **Respuestas de Acceso:**
  * Si no se envía token o es inválido: `401 Unauthorized`.
  * Si el usuario autenticado tiene rol `MERCHANT` o `CUSTOMER`: `403 Forbidden`.
* **Regla de Negocio `RN-ADM-01` (No Auto-Suspensión):** Un administrador **no puede suspender su propia cuenta** (retorna `409 Conflict` con código `NO_SELF_SUSPENSION`).

---

## 2. Endpoints: Gestión de Usuarios (`/api/v1/admin/users`)

### 2.1. Listar Usuarios
* **Método & Ruta:** `GET /api/v1/admin/users`
* **Parámetros de Consulta (Query Params - Opcionales):**
  * `query` *(string)*: Filtra por coincidencia parcial insensible a mayúsculas en `firstName`, `lastName` o `email`.
  * `role` *(enum)*: `ADMIN`, `MERCHANT`, `CUSTOMER`.
  * `status` *(enum)*: `ACTIVE`, `SUSPENDED`.

#### Respuesta Exitosa (`200 OK`):
```json
[
  {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e1",
    "email": "merchant.demo@jaldishop.com",
    "firstName": "Carlos",
    "lastName": "García",
    "phone": "+51987654321",
    "status": "ACTIVE",
    "roles": ["MERCHANT"],
    "store": {
      "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
      "name": "Pastelería Dulce Deleite",
      "slug": "dulce-deleite",
      "status": "ACTIVE"
    },
    "createdAt": "2026-09-23T13:20:00Z",
    "updatedAt": "2026-09-23T13:20:00Z"
  },
  {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e3",
    "email": "admin.demo@jaldishop.com",
    "firstName": "Admin",
    "lastName": "JaldiShop",
    "phone": "+51999999999",
    "status": "ACTIVE",
    "roles": ["ADMIN"],
    "store": null,
    "createdAt": "2026-09-23T13:20:00Z",
    "updatedAt": "2026-09-23T13:20:00Z"
  }
]
```

---

### 2.2. Consultar Detalle de Usuario por ID
* **Método & Ruta:** `GET /api/v1/admin/users/{id}`
* **Parámetros de Ruta:** `id` *(UUID)*.

#### Respuesta Exitosa (`200 OK`):
```json
{
  "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e1",
  "email": "merchant.demo@jaldishop.com",
  "firstName": "Carlos",
  "lastName": "García",
  "phone": "+51987654321",
  "status": "ACTIVE",
  "roles": ["MERCHANT"],
  "store": {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
    "name": "Pastelería Dulce Deleite",
    "slug": "dulce-deleite",
    "status": "ACTIVE"
  },
  "createdAt": "2026-09-23T13:20:00Z",
  "updatedAt": "2026-09-23T13:20:00Z"
}
```

---

### 2.3. Suspender Usuario
* **Método & Ruta:** `PATCH /api/v1/admin/users/{id}/suspend`
* **Parámetros de Ruta:** `id` *(UUID del usuario a suspender)*.
* **Comportamiento:** Cambia el estado del usuario a `SUSPENDED`.

#### Respuesta Exitosa (`200 OK`):
```json
{
  "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e1",
  "email": "merchant.demo@jaldishop.com",
  "firstName": "Carlos",
  "lastName": "García",
  "phone": "+51987654321",
  "status": "SUSPENDED",
  "roles": ["MERCHANT"],
  "store": {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
    "name": "Pastelería Dulce Deleite",
    "slug": "dulce-deleite",
    "status": "ACTIVE"
  },
  "createdAt": "2026-09-23T13:20:00Z",
  "updatedAt": "2026-09-23T18:00:00Z"
}
```

#### Respuesta de Error por Auto-Suspensión (`409 Conflict`):
```json
{
  "code": "NO_SELF_SUSPENSION",
  "message": "Un administrador no puede suspender su propia cuenta.",
  "errors": null
}
```

---

### 2.4. Reactivar Usuario
* **Método & Ruta:** `PATCH /api/v1/admin/users/{id}/activate`
* **Parámetros de Ruta:** `id` *(UUID del usuario a activar)*.
* **Comportamiento:** Cambia el estado del usuario a `ACTIVE`.

#### Respuesta Exitosa (`200 OK`):
```json
{
  "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e1",
  "email": "merchant.demo@jaldishop.com",
  "firstName": "Carlos",
  "lastName": "García",
  "phone": "+51987654321",
  "status": "ACTIVE",
  "roles": ["MERCHANT"],
  "store": {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
    "name": "Pastelería Dulce Deleite",
    "slug": "dulce-deleite",
    "status": "ACTIVE"
  },
  "createdAt": "2026-09-23T13:20:00Z",
  "updatedAt": "2026-09-23T18:05:00Z"
}
```

---

## 3. Endpoints: Gestión de Tiendas (`/api/v1/admin/stores`)

### 3.1. Listar Tiendas Globalmente
* **Método & Ruta:** `GET /api/v1/admin/stores`
* **Parámetros de Consulta (Query Params - Opcionales):**
  * `query` *(string)*: Filtra por coincidencia parcial en `name`, `slug` o `address`.
  * `status` *(enum)*: `ACTIVE`, `SUSPENDED`.

#### Respuesta Exitosa (`200 OK`):
```json
[
  {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
    "name": "Pastelería Dulce Deleite",
    "slug": "dulce-deleite",
    "contactPhone": "+51987654321",
    "address": "Av. La Marina 1234, San Miguel, Lima",
    "status": "ACTIVE",
    "owner": {
      "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e1",
      "email": "merchant.demo@jaldishop.com",
      "firstName": "Carlos",
      "lastName": "García",
      "phone": "+51987654321",
      "status": "ACTIVE"
    },
    "createdAt": "2026-09-23T13:20:00Z",
    "updatedAt": "2026-09-23T13:20:00Z"
  }
]
```

---

### 3.2. Consultar Detalle de Tienda por ID
* **Método & Ruta:** `GET /api/v1/admin/stores/{id}`
* **Parámetros de Ruta:** `id` *(UUID de la tienda)*.

#### Respuesta Exitosa (`200 OK`):
```json
{
  "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
  "name": "Pastelería Dulce Deleite",
  "slug": "dulce-deleite",
  "description": "Tortas artesanales y postres premium para celebraciones.",
  "contactPhone": "+51987654321",
  "address": "Av. La Marina 1234, San Miguel, Lima",
  "addressReference": "Frente a Plaza San Miguel",
  "latitude": -12.076842,
  "longitude": -77.086431,
  "pickupEnabled": true,
  "deliveryEnabled": true,
  "deliveryFeeAmount": 5.00,
  "deliveryFeeCurrency": "PEN",
  "taxApplies": true,
  "taxRate": 18.00,
  "status": "ACTIVE",
  "owner": {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e1",
    "email": "merchant.demo@jaldishop.com",
    "firstName": "Carlos",
    "lastName": "García",
    "phone": "+51987654321",
    "status": "ACTIVE"
  },
  "createdAt": "2026-09-23T13:20:00Z",
  "updatedAt": "2026-09-23T13:20:00Z"
}
```

---

### 3.3. Suspender Tienda
* **Método & Ruta:** `PATCH /api/v1/admin/stores/{id}/suspend`
* **Parámetros de Ruta:** `id` *(UUID de la tienda)*.
* **Comportamiento:** Cambia el estado de la tienda a `SUSPENDED`.

#### Respuesta Exitosa (`200 OK`):
```json
{
  "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
  "name": "Pastelería Dulce Deleite",
  "slug": "dulce-deleite",
  "status": "SUSPENDED",
  "owner": {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e1",
    "email": "merchant.demo@jaldishop.com",
    "firstName": "Carlos",
    "lastName": "García",
    "phone": "+51987654321",
    "status": "ACTIVE"
  },
  "createdAt": "2026-09-23T13:20:00Z",
  "updatedAt": "2026-09-23T18:10:00Z"
}
```

---

### 3.4. Reactivar Tienda
* **Método & Ruta:** `PATCH /api/v1/admin/stores/{id}/activate`
* **Parámetros de Ruta:** `id` *(UUID de la tienda)*.
* **Comportamiento:** Cambia el estado de la tienda a `ACTIVE`.

#### Respuesta Exitosa (`200 OK`):
```json
{
  "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e5",
  "name": "Pastelería Dulce Deleite",
  "slug": "dulce-deleite",
  "status": "ACTIVE",
  "owner": {
    "id": "80acf0ba-5a79-4888-b00d-b5aef8fcf9e1",
    "email": "merchant.demo@jaldishop.com",
    "firstName": "Carlos",
    "lastName": "García",
    "phone": "+51987654321",
    "status": "ACTIVE"
  },
  "createdAt": "2026-09-23T13:20:00Z",
  "updatedAt": "2026-09-23T18:12:00Z"
}
```

---

## 4. Matriz de Errores Específicos

| HTTP Status | Código `code` | Motivo |
|:---:|:---|:---|
| **`401`** | `UNAUTHORIZED` | Token JWT ausente o inválido en encabezado `Authorization`. |
| **`403`** | `FORBIDDEN` | Usuario no posee el rol `ROLE_ADMIN`. |
| **`404`** | `RESOURCE_NOT_FOUND` | Usuario o Tienda con el UUID proporcionado no existe en la base de datos. |
| **`409`** | `NO_SELF_SUSPENSION` | El administrador autenticado intentó suspender su propia cuenta. |
