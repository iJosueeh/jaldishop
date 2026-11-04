# API de Inventario (Merchant) — BE-13

### JaldiShop — Gestión de Existencias y Alertas de Stock Bajo

---

`📍 Docs` > `04-Diseño` > **API Inventario**  
[🏠 Volver al Índice General](../../README.md) | [Sprint 03](../06-scrum/sprint-03.md) | [Modelo ER](modelo-er.md)

---

## 1. Propósito y Alcance

El módulo de inventario (**BE-13**) permite a los comerciantes administradores gestionar el stock de productos y variantes de su tienda, configurar alertas tempranas de existencias bajas y supervisar la disponibilidad operativa respetando el aislamiento multitenant.

### Principios de Diseño
1. **Aislamiento Multitenant Absoluto:** Todo endpoint está protegido por `@PreAuthorize("hasRole('MERCHANT')")`. La tienda del comerciante se infiere exclusivamente del token JWT autenticado a través de `StoreContextService.requireStoreId(principal)`. No se confía en parámetros externos.
2. **Coexistencia con Reservas Transaccionales:** El módulo convive con los flujos de checkout (`ReserveInventoryService`), confirmación de compra (`CommitInventoryReservationService`) y cancelaciones (`RestockInventoryService`), sin duplicar descuentos ni sobreescribir holds vigentes.
3. **Semántica Absoluta en Ajustes:** El endpoint `PUT /stock` establece una **cantidad física absoluta** (conteo físico en anaquel o almacén), no un delta incremental.

---

## 2. Definición de Estados y Reglas de Existencias

### Estados de Inventario (`inventoryStatus`)
- **`NOT_TRACKED`**: La variante tiene `tracksInventory = false`. No gestiona stock. Las cantidades física, reservada y disponible se reportan como `null`, y `isLowStock = false`.
- **`NOT_CONFIGURED`**: La variante tiene `tracksInventory = true`, pero aún no cuenta con un registro en la tabla `inventories` (estado posterior a su creación en catálogo). No equivale a existencias en cero; requiere inicialización explícita mediante `PUT /stock`.
- **`CONFIGURED`**: La variante tiene control de inventario activo y una fila asociada en `inventories`. Se reportan:
  - `physicalQuantity`: Stock real en estante (`inventories.quantity`).
  - `reservedQuantity`: Cantidad en holds activos y no vencidos (`sumActiveReservedQuantity`).
  - `availableQuantity`: `max(0, physicalQuantity - reservedQuantity)`.

### Regla de Alerta de Stock Bajo (`isLowStock`)
Se activa exclusivamente cuando se cumplen simultáneamente las siguientes condiciones:
1. `tracksInventory = true`
2. `lowStockThreshold != null`
3. `physicalQuantity <= lowStockThreshold`

---

## 3. Consideración Operativa: Concurrencia entre Conteos Físicos y Movimientos

Dado que `PUT /api/v1/inventory/variants/{variantId}/stock` establece un valor absoluto:
- **Protección contra Sobrevendidos:** El sistema valida que el nuevo `quantity` sea mayor o igual a las reservas activas vigentes (`sumActiveReservedQuantity`). Si el comerciante intenta fijar un número inferior a las unidades reservadas en transacciones en curso (ej. clientes en pasarela de pago), la operación es rechazada con `409 Conflict` (`CANNOT_REDUCE_BELOW_RESERVED`).
- **Conteo Físico Concurrente:** Si un comerciante realiza un ajuste manual de inventario mientras se procesan compras o cancelaciones concurrentes:
  - Cada mutación adquiere un bloqueo pesimista ordenado (`ProductVariant` -> `inventories`).
  - La transacción que finalice al último determinará el stock físico final registrado. Se recomienda a los comerciantes realizar ajustes de inventario fuera de horas punta o basar los conteos en existencia física desvinculada de pedidos ya preparados.

---

## 4. Catálogo de Endpoints

### 4.1. Consultar Inventario de la Tienda
- **Método y Ruta:** `GET /api/v1/inventory`
- **Seguridad:** Requiere rol `MERCHANT`.
- **Descripción:** Retorna la totalidad de variantes de la tienda del comerciante, incluyendo variantes rastreables y no rastreables.
- **Respuesta Exitosa (200 OK):**
```json
[
  {
    "variantId": "80acf0ba-5a79-4888-b00d-b5aef8fcf302",
    "productId": "80acf0ba-5a79-4888-b00d-b5aef8fcf201",
    "productName": "Café Bourbon Especial",
    "presentationName": "Bolsa 250g en grano",
    "sku": "CAF-BOU-250G",
    "tracksInventory": true,
    "status": "ACTIVE",
    "inventoryStatus": "CONFIGURED",
    "physicalQuantity": 15,
    "reservedQuantity": 2,
    "availableQuantity": 13,
    "lowStockThreshold": 5,
    "isLowStock": false,
    "updatedAt": "2026-11-04T16:00:00Z"
  }
]
```

---

### 4.2. Consultar Inventario por Variante
- **Método y Ruta:** `GET /api/v1/inventory/variants/{variantId}`
- **Seguridad:** Requiere rol `MERCHANT`.
- **Descripción:** Obtiene el estado detallado de inventario de una variante específica de la tienda.
- **Respuestas de Error:**
  - `404 Not Found` (`VARIANT_NOT_FOUND`): Si la variante no existe o pertenece a otra tienda.

---

### 4.3. Consultar Variantes con Stock Bajo
- **Método y Ruta:** `GET /api/v1/inventory/low-stock`
- **Seguridad:** Requiere rol `MERCHANT`.
- **Descripción:** Retorna las variantes de la tienda que cumplen con la condición `physicalQuantity <= lowStockThreshold`.

---

### 4.4. Configurar / Ajustar Stock Físico (Semántica Absoluta)
- **Método y Ruta:** `PUT /api/v1/inventory/variants/{variantId}/stock`
- **Seguridad:** Requiere rol `MERCHANT`.
- **Cuerpo de la Solicitud:**
```json
{
  "quantity": 25
}
```
- **Validaciones:**
  - `quantity >= 0`: Valor obligatorio entero no negativo.
  - La variante debe tener `tracksInventory = true`. Si es `false`, retorna `422 Unprocessable Content` (`INVENTORY_TRACKING_DISABLED`).
  - `quantity >= reservedQuantity`: Si el valor es menor que los holds vigentes, retorna `409 Conflict` (`CANNOT_REDUCE_BELOW_RESERVED`).
- **Respuesta Exitosa (200 OK):** Retorna `InventoryResponse` con el nuevo estado.

---

### 4.5. Configurar Umbral de Stock Bajo
- **Método y Ruta:** `PATCH /api/v1/inventory/variants/{variantId}/threshold`
- **Seguridad:** Requiere rol `MERCHANT`.
- **Cuerpo de la Solicitud:**
```json
{
  "lowStockThreshold": 8
}
```
- **Validaciones:**
  - `lowStockThreshold >= 0` o `null` (para desactivar alertas).
  - La variante debe pertenecer a la tienda y tener `tracksInventory = true`.
  - Requiere que la variante tenga inventario previamente configurado. Si falta fila en `inventories`, retorna `409 Conflict` (`INVENTORY_NOT_CONFIGURED`).
