# Guía de Uso de la Colección Postman — JaldiShop API

**Entregable 1 — Modelado de Datos + Backend API (Núcleo Transaccional)**

La colección oficial de Postman para JaldiShop se encuentra disponible en:
- `docs/04-diseno/jaldishop-api.postman_collection.json`
- `backend/jaldishop-api.postman_collection.json`

---

## 1. Importación Rápida en Postman / Insomnia

1. Abre **Postman** (o **Insomnia**).
2. Haz clic en el botón superior **Import**.
3. Selecciona el archivo [`jaldishop-api.postman_collection.json`](file:///C:/Users/IJosueeh/Documents/Proyectos/jaldishop/docs/04-diseno/jaldishop-api.postman_collection.json).
4. La colección se importará con el nombre **"JaldiShop API - Entregable 1"** conteniendo todas las carpetas estructuradas y variables configuradas.

---

## 2. Variables de Colección Configuradas

| Variable | Valor por Defecto | Descripción |
| :--- | :--- | :--- |
| `baseUrl` | `http://localhost:8080` | URL base del servidor Spring Boot local. |
| `merchantToken` | *(Dinámico / Auto-guardado)* | JWT con rol `MERCHANT` obtenido al hacer Login. |
| `customerToken` | *(Dinámico / Auto-guardado)* | JWT con rol `CUSTOMER` obtenido al hacer Login. |
| `storeId` | `a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d` | UUID de la tienda activa. |
| `categoryId` | *(Dinámico)* | UUID de la categoría de productos creada. |
| `productId` | *(Dinámico)* | UUID del producto creado. |
| `orderId` | *(Dinámico)* | UUID de la comanda registrada. |

> [!TIP]
> **Autenticación Automática (Zero Copy-Paste):**
> Al ejecutar el request **"03. Login Merchant"** o **"04. Login Customer"**, un script de test integrado en Postman extrae automáticamente el `token` de la respuesta JSON y lo inyecta en las variables `merchantToken` / `customerToken`. Todos los endpoints subsiguientes ya consumen `Bearer {{merchantToken}}`.

---

## 3. Flujo de Demostración Paso a Paso para la Evaluación

### Paso 1: Salud del Sistema
* **Request:** `GET {{baseUrl}}/api/v1/health`
* **Resultado:** `HTTP 200 OK` confirmando que la aplicación Spring Boot está arriba y conectada a PostgreSQL.

### Paso 2: Registro e Inicio de Sesión (JWT con Roles)
1. Ejecutar **"01. Register Merchant (Admin de Tienda)"**
   - Payload con credenciales, datos personales y tienda inicial.
   - Retorna HTTP 201 con JWT y rol `MERCHANT`.
2. Ejecutar **"03. Login Merchant"**
   - Captura y almacena automáticamente el `merchantToken`.

### Paso 3: Configuración de la Tienda Multi-tenant
1. Ejecutar **"02. Get My Store"**
   - Retorna la información de la tienda vinculada al Merchant.
2. Ejecutar **"03. Update Store Details"**
   - Actualiza fletes de delivery, geolocalización y políticas de despacho.

### Paso 4: Creación de Categorías y Catálogo de Productos
1. Ejecutar **"01. Create Category"**
   - Crea la categoría *"Tortas y Pasteles"*. Guarda `categoryId`.
2. Ejecutar **"03. Create Product with Variants & Inventory"**
   - Crea la *"Torta de Chocolate Fudge Supremo"* con variantes (12 porc. S/ 78, 24 porc. S/ 135), control de stock inicial y atributos descriptivos.
3. Ejecutar **"04. List Store Products"**
   - Retorna el listado del catálogo activo.

### Paso 5: Motor de Capacidad y Slots de Producción
1. Ejecutar **"01. Create Capacity Configuration"**
   - Configura cupos para los días sábados (Tope: 8 pedidos por bloque).
2. Ejecutar **"03. Create Capacity Exception"**
   - Sobrescribe la capacidad ordinaria para una fecha pico (ej. Halloween: 15 pedidos).

### Paso 6: Registro de Comandas y Ciclo de Vida del Pedido
1. Ejecutar **"01. Create Order / Comanda Operativa"**
   - Registra una comanda multicanal (`WHATSAPP`), con modalidad `DELIVERY` y desglose de ítems calculados.
2. Ejecutar **"03. Update Order Status (Avanzar Estado)"**
   - Pasa la comanda de `CONFIRMED` a `IN_PREPARATION` y genera el registro en `order_status_history`.
