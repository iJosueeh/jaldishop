# Contrato API: Contadores de Estados de Pedidos Merchant

## 1. Resumen

Endpoint para obtener agregaciones de pedidos por estado a nivel de tienda en PostgreSQL.
Elimina la dependencia del frontend de calcular totales basándose en listas paginadas en memoria.

- **Ruta**: `GET /api/v1/merchants/stores/{storeId}/orders/counts`
- **Autenticación**: Obligatoria (`Bearer <JWT>`)
- **Autorización**: Rol `MERCHANT` y pertenencia validada de la tienda (`validateStoreOwnership`)

---

## 2. Parámetros

| Parámetro | Tipo | Ubicación | Requerido | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `storeId` | UUID | Path | Sí | Identificador único de la tienda |
| `date` | String (`YYYY-MM-DD`) | Query | No | Fecha operativa de servicio (`service_date`). Si se omite, retorna histórico completo. |

---

## 3. Reglas de Negocio Implementadas

- **RN-COUNT-01 — Histórico de pedidos**:
  Sin parámetro `date`, retorna el conteo acumulado de toda la historia de la tienda.
- **RN-COUNT-02 — Pestaña Todos**:
  `all = confirmed + inPreparation + ready + outForDelivery + completed + cancelled`.
  Incluye explícitamente los pedidos en estado `CANCELLED`. No representa volumen financiero/ventas netas.
- **RN-COUNT-03 — Pedidos activos del Sidebar**:
  Pedidos activos = `CONFIRMED + IN_PREPARATION + READY`.
  Excluye `OUT_FOR_DELIVERY`, `COMPLETED` y `CANCELLED`.
- **RN-COUNT-04 — Pedidos operativos del día**:
  Cuando se envía `date`, el filtro se aplica contra `orders.service_date`, no contra `confirmed_at`.
  Zona horaria de referencia operativa: `America/Lima`.

---

## 4. Contrato de Respuesta

### Código HTTP 200 OK

```json
{
  "all": 48,
  "confirmed": 12,
  "inPreparation": 6,
  "ready": 4,
  "outForDelivery": 2,
  "completed": 20,
  "cancelled": 4
}
```

### Semántica de Campos

- `all` (long): Total de pedidos contabilizados (`confirmed + inPreparation + ready + outForDelivery + completed + cancelled`).
- `confirmed` (long): Pedidos en estado `CONFIRMED`.
- `inPreparation` (long): Pedidos en estado `IN_PREPARATION`.
- `ready` (long): Pedidos en estado `READY`.
- `outForDelivery` (long): Pedidos en estado `OUT_FOR_DELIVERY`.
- `completed` (long): Pedidos en estado `COMPLETED`.
- `cancelled` (long): Pedidos en estado `CANCELLED`.

Tiendas o fechas sin pedidos retornan todos los campos en valor `0`.

---

## 5. Códigos de Error

| Código | Condición |
| :--- | :--- |
| `400 Bad Request` | Formato inválido de UUID en `storeId` o formato inválido de fecha en `date` (no ISO `YYYY-MM-DD`). |
| `401 Unauthorized` | Petición sin token JWT o con token expirado/inválido. |
| `403 Forbidden` | Usuario autenticado sin rol `MERCHANT`, o usuario `MERCHANT` intentando acceder a una tienda que no le pertenece. |
| `404 Not Found` | Tienda inexistente. |
| `500 Internal Server Error` | Error de infraestructura o persistencia. Los errores se propagan sin enmascarar en ceros ficticios. |

---

## 6. Ejemplos cURL

### Histórico completo de la tienda

```bash
curl -X GET "https://api.jaldishop.pe/api/v1/merchants/stores/b6534571-0672-4632-9cb8-b57022db6f5e/orders/counts" \
  -H "Authorization: Bearer <MERCHANT_JWT_TOKEN>" \
  -H "Accept: application/json"
```

### Contadores de una fecha operativa específica

```bash
curl -X GET "https://api.jaldishop.pe/api/v1/merchants/stores/b6534571-0672-4632-9cb8-b57022db6f5e/orders/counts?date=2026-11-14" \
  -H "Authorization: Bearer <MERCHANT_JWT_TOKEN>" \
  -H "Accept: application/json"
```
