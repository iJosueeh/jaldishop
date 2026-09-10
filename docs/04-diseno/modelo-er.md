# Modelo Entidad-Relación

### JaldiShop — Diseño de Persistencia v1.6.1

[![Estado](https://img.shields.io/badge/Estado-Listo%20para%20DDL-green?style=for-the-badge&logo=checkmarx&logoColor=white)](./modelo-er.md)
[![Versión](https://img.shields.io/badge/Versión-v1.6.1-blue?style=for-the-badge)](./modelo-er.md)
[![Fase](https://img.shields.io/badge/Fase-Listo%20para%20implementación-orange?style=for-the-badge)](../03-requisitos/modelo-dominio.md)

---

`📍 Docs` > `04-Diseno` > **Modelo ER**
[🏠 Índice General](../../README.md) | [Modelo de Dominio ➡](../03-requisitos/modelo-dominio.md)

---

> 📌 **Nota:** Este documento traduce el modelo conceptual del dominio de JaldiShop a una propuesta de persistencia relacional para PostgreSQL. No representa todavía el DDL final ni el mapeo JPA. Las decisiones de dominio permanecen documentadas en: docs/03-requisitos/modelo-dominio.md

---

## 1. Principios de Diseño

- PostgreSQL como base de datos relacional.
- Diseño orientado a 3FN.
- Los Value Objects no requieren necesariamente tablas propias.
- Los snapshots históricos de Pedido son desnormalizaciones intencionales.
- PK y UNIQUE generan índices automáticamente en PostgreSQL.
- Las FK no generan índices automáticamente.
- No crear índices sobre todas las FK sin una consulta que los justifique.
- La selección final de índices podrá revisarse mediante EXPLAIN ANALYZE.
- El modelo evita tablas innecesarias para conceptos derivados o infraestructura.
- PK UUID generadas por aplicación (Spring/JPA), no por PostgreSQL.
- No utilizar PostgreSQL ENUM; usar VARCHAR + CHECK.

**Aclaraciones:**

| Capa | Responsabilidad |
|---|---|
| Dominio | Define responsabilidades e invariantes |
| Modelo ER | Define persistencia y relaciones físicas |
| JPA | Se definirá después del modelo ER |

---

## 2. Inventario Final de Tablas v1.0

| # | Tabla | Propósito | PK | Tipo PK |
|---|---|---|---|---|
| 1 | users | Usuarios de la plataforma | id | UUID |
| 2 | roles | Catálogo de roles | id | SMALLINT |
| 3 | user_roles | Relación usuario-rol | (user_id, role_id) | Compuesta |
| 4 | stores | Tiendas de los comerciantes | id | UUID |
| 5 | categories | Categorías de productos | id | UUID |
| 6 | products | Productos dentro de una tienda | id | UUID |
| 7 | product_variants | Variantes/presentaciones de productos | id | UUID |
| 8 | variant_attributes | Atributos de cada variante | (variant_id, name) | Compuesta |
| 9 | inventories | Stock de variantes con control | variant_id | FK |
| 10 | carts | Carritos de compra | id | UUID |
| 11 | cart_items | Items dentro de un carrito | (cart_id, variant_id) | Compuesta |
| 12 | discounts | Descuentos definidos por tienda | id | UUID |
| 13 | capacity_configurations | Capacidad base recurrente | id | UUID |
| 14 | capacity_exceptions | Excepciones de capacidad | id | UUID |
| 15 | capacity_reservations | Reservas temporales de cupo | id | UUID |
| 16 | payments | Procesos de pago | id | UUID |
| 17 | payment_attempts | Intentos de pago | id | UUID |
| 18 | orders | Pedidos confirmados | id | UUID |
| 19 | order_items | Detalles de cada pedido | id | UUID |
| 20 | order_status_history | Historial de estados | id | UUID |
| 21 | favorites | Favoritos de usuarios | (user_id, product_id) | Compuesta |
| 22 | reviews | Reseñas de productos | id | UUID |
| 23 | notifications | Notificaciones a usuarios | id | UUID |

**Total: 23 tablas**

---

## 3. Estrategia de Primary Keys

### Entidades principales: UUID

Se aplica UUID a las siguientes tablas:

- users
- stores
- categories
- products
- product_variants
- carts
- discounts
- capacity_configurations
- capacity_exceptions
- capacity_reservations
- payments
- payment_attempts
- orders
- order_items
- order_status_history
- reviews
- notifications

### Tablas con PK natural/compuesta

| Tabla | PK | Justificación |
|---|---|---|
| roles | id (SMALLINT) | PK artificial mínima para relación |
| user_roles | (user_id, role_id) | PK natural de la relación |
| variant_attributes | (variant_id, name) | PK natural del atributo |
| inventories | variant_id | PK coincide con FK a variante |
| cart_items | (cart_id, variant_id) | PK natural del item |
| favorites | (user_id, product_id) | PK natural de la relación |

---

## 4. Generación de UUID

**Decisión cerrada:** Los UUID son generados por Spring/JPA, no por PostgreSQL.

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

**PostgreSQL almacena:** `UUID PRIMARY KEY`

**NO se utiliza:** `gen_random_uuid()` como DEFAULT.

**Motivos:**
- Independencia de funciones específicas de PostgreSQL
- IDs disponibles desde la aplicación
- Mejor coherencia con JPA/tests
- Estrategia uniforme en entidades

---

## 5. Tipos PostgreSQL Base

| Categoría | Tipo PostgreSQL | Aplicación |
|---|---|---|
| Identidades | UUID | PK de entidades principales |
| Identidades pequeñas | SMALLINT | roles.id |
| Dinero | NUMERIC(12,2) | amounts, precios, totales |
| Porcentaje | NUMERIC(5,2) | tax_rate, percentage values |
| Cantidades | INTEGER | quantities, capacities |
| Booleanos | BOOLEAN | flags (pickup_enabled, etc.) |
| Moneda | CHAR(3) | ISO currency codes |
| Fechas operativas | DATE | service_date |
| Horas operativas | TIME | start_time, end_time |
| Instantes/auditoría | TIMESTAMPTZ | created_at, updated_at, changed_at |
| Coordenadas | NUMERIC(9,6) | latitude, longitude |
| Snapshots históricos | JSONB | attributes_snapshot |
| Textos libres | TEXT | description, comment, message, error_message, reason |

---

## 6. Longitudes Recomendadas

| Campo | Longitud | Justificación |
|---|---|---|
| users.email | VARCHAR(254) | RFC 5321 |
| users.first_name | VARCHAR(100) | — |
| users.last_name | VARCHAR(100) | — |
| users.phone | VARCHAR(30) | Internacional con + |
| users.status | VARCHAR(20) | CHECK restrictivo |
| stores.name | VARCHAR(160) | — |
| stores.slug | VARCHAR(180) | — |
| stores.description | TEXT | — |
| categories.name | VARCHAR(120) | — |
| products.name | VARCHAR(160) | — |
| products.slug | VARCHAR(180) | — |
| products.description | TEXT | — |
| products.image_url | TEXT | — |
| product_variants.presentation_name | VARCHAR(120) | — |
| product_variants.sku | VARCHAR(100) | — |
| discounts.code | VARCHAR(80) | — |
| discounts.name | VARCHAR(160) | — |
| orders.order_number | VARCHAR(50) | Formato JAL-AAAA-NNNNNN |
| orders.currency | CHAR(3) | ISO 4217 |
| variant_attributes.value | TEXT | — |
| reviews.comment | TEXT | — |
| notifications.title | VARCHAR(180) | — |
| notifications.message | TEXT | — |
| payment_attempts.provider | VARCHAR(30) | MERCADO_PAGO |
| payment_attempts.payment_method | VARCHAR(30) | CARD |
| payment_attempts.idempotency_key | VARCHAR(64) | UUID extendido |
| payment_attempts.provider_payment_id | VARCHAR(100) | ID de Mercado Pago |
| payment_attempts.provider_status | VARCHAR(50) | Estado del proveedor |
| payment_attempts.provider_status_detail | VARCHAR(100) | Detalle del estado |
| payment_attempts.error_code | VARCHAR(100) | Código de error |
| payment_attempts.error_message | TEXT | — |
| capacity_exception.reason | TEXT | — |

**Nota:** Las longitudes podrán ajustarse si la interfaz o requisitos finales lo justifican.

---

## 7. Dinero y NUMERIC

**Utilizar NUMERIC, nunca FLOAT/REAL/DOUBLE.**

### Campos con NUMERIC(12,2)

- product_variants.price_amount
- stores.delivery_fee_amount
- discounts.value
- discounts.minimum_purchase_amount
- cart_items.reference_price_amount
- payments.amount
- payments.refund_amount
- orders.products_subtotal_amount
- orders.discount_amount
- orders.delivery_fee_amount
- orders.included_tax_amount
- orders.total_amount
- order_items.unit_price_amount
- order_items.subtotal_amount

### Campos con NUMERIC(5,2)

- stores.tax_rate
- discounts.value cuando type = PERCENTAGE (CHECK <= 100)

### Constraints de dinero

- `amount > 0` para payments.amount
- `>= 0` para todos los demás campos monetarios
- `refund_amount <= amount` para payments

---

## 8. Coordenadas

| Campo | Tipo | Constraints |
|---|---|---|
| stores.latitude | NUMERIC(9,6) | BETWEEN -90 AND 90 |
| stores.longitude | NUMERIC(9,6) | BETWEEN -180 AND 180 |
| orders.delivery_latitude | NUMERIC(9,6) | BETWEEN -90 AND 90 |
| orders.delivery_longitude | NUMERIC(9,6) | BETWEEN -180 AND 180 |

**Nota:** Las coordenadas son opcionales. No se utiliza PostGIS en el MVP.

---

## 9. Estados y ENUMs

**Decisión cerrada:** NO utilizar PostgreSQL ENUM.

**Estrategia:** VARCHAR + CHECK

**Motivos:**
- Migraciones Flyway más simples
- Agregar/modificar valores requiere menos fricción
- Evita acoplamiento innecesario a CREATE TYPE

### Valores de estados por entidad

**users.status:** VARCHAR(30)
- ACTIVE
- INACTIVE
- SUSPENDED

**stores.status:** VARCHAR(30)
- ACTIVE
- INACTIVE
- SUSPENDED
- CLOSED

**categories.status:** VARCHAR(30)
- ACTIVE
- INACTIVE

**products.status:** VARCHAR(30)
- ACTIVE
- INACTIVE

**product_variants.status:** VARCHAR(30)
- ACTIVE
- INACTIVE

**discounts.type:** VARCHAR(30)
- PERCENTAGE
- FIXED_AMOUNT

**discounts.modality:** VARCHAR(30)
- AUTOMATIC
- CODE

**discounts.status:** VARCHAR(30)
- ACTIVE
- INACTIVE

**capacity_configurations.status:** VARCHAR(30)
- ACTIVE
- INACTIVE

**capacity_exceptions.status:** VARCHAR(30)
- ACTIVE
- INACTIVE

**capacity_reservations.status:** VARCHAR(30)
- ACTIVE
- PAYMENT_PROTECTED
- COMMITTED
- EXPIRED
- RELEASED

**payments.status:** VARCHAR(30)
- PENDING
- PROCESSING
- APPROVED
- FAILED

**payments.refund_status:** VARCHAR(30)
- NOT_REQUIRED
- PENDING
- PROCESSING
- COMPLETED
- FAILED

**payment_attempts.status:** VARCHAR(30)
- CREATED
- PROCESSING
- APPROVED
- REJECTED
- ERROR

**orders.status:** VARCHAR(30)
- CONFIRMED
- IN_PREPARATION
- READY
- OUT_FOR_DELIVERY
- COMPLETED
- CANCELLED

**orders.delivery_mode:** VARCHAR(30)
- PICKUP
- DELIVERY

**reviews.status:** VARCHAR(30)
- PUBLISHED
- HIDDEN

**notifications.type:** VARCHAR(30)
- NEW_ORDER
- ORDER_STATUS_CHANGED
- LOW_STOCK
- SYSTEM

**notifications.status:** VARCHAR(30)
- UNREAD
- READ

---

## 10. users

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| email | VARCHAR(254) | NOT NULL, UNIQUE |
| password_encoded | VARCHAR(255) | NOT NULL |
| first_name | VARCHAR(100) | NOT NULL |
| last_name | VARCHAR(100) | NOT NULL |
| phone | VARCHAR(30) | NULL |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, INACTIVE, SUSPENDED) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Notas:**
- No almacenar `nombreCompleto` porque es derivado.
- Email se normaliza a lowercase antes de persistir.

---

## 11. roles y user_roles

### roles

| Columna | Tipo | Restricciones |
|---|---|---|
| id | SMALLINT | PK |
| name | VARCHAR(20) | NOT NULL, UNIQUE |

**Valores iniciales:** CUSTOMER, MERCHANT, ADMIN

### user_roles

| Columna | Tipo | Restricciones |
|---|---|---|
| user_id | UUID | PK, FK -> users.id, ON DELETE CASCADE |
| role_id | SMALLINT | PK, FK -> roles.id, ON DELETE RESTRICT |

**PK compuesta:** (user_id, role_id)

**Índice adicional:** INDEX(role_id)

---

## 12. stores

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| merchant_user_id | UUID | FK -> users.id, UNIQUE, ON DELETE RESTRICT |
| name | VARCHAR(160) | NOT NULL |
| slug | VARCHAR(180) | UNIQUE |
| description | TEXT | |
| contact_phone | VARCHAR(30) | |
| address | TEXT | |
| address_reference | TEXT | |
| latitude | NUMERIC(9,6) | CHECK BETWEEN -90 AND 90 |
| longitude | NUMERIC(9,6) | CHECK BETWEEN -180 AND 180 |
| pickup_enabled | BOOLEAN | NOT NULL |
| delivery_enabled | BOOLEAN | NOT NULL |
| delivery_fee_amount | NUMERIC(12,2) | CHECK >= 0 |
| delivery_fee_currency | CHAR(3) | |
| tax_applies | BOOLEAN | NOT NULL |
| tax_rate | NUMERIC(5,2) | CHECK > 0 AND <= 100 |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, INACTIVE, SUSPENDED, CLOSED) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones:**
- UNIQUE(merchant_user_id) — máximo una Tienda por comerciante.
- UNIQUE(slug)

**Semántica de entrega:**
| delivery_enabled | delivery_fee_amount | Significado |
|---|---|---|
| true | >= 0 | Delivery disponible (gratuito si 0) |
| false | NULL | Delivery no disponible |

**Semántica tributaria:**
| tax_applies | tax_rate | Significado |
|---|---|---|
| false | NULL | Sin impuesto |
| true | > 0 | Impuesto aplicado |

---

## 13. categories

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| store_id | UUID | FK -> stores.id, ON DELETE RESTRICT |
| name | VARCHAR(120) | NOT NULL |
| description | TEXT | |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, INACTIVE) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Índice:** INDEX(store_id)

**UNIQUE funcional parcial:**
```
UNIQUE(store_id, (lower(trim(name)))) WHERE status = 'ACTIVE'
```

**Normalización de nombre (aplicación/backend):**
- trim
- lowercase
- espacios repetidos normalizados

**Invariante de aplicación:** Categorías activas con nombre normalizado equivalente único dentro de la Tienda.

---

## 14. products

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| store_id | UUID | FK -> stores.id, ON DELETE RESTRICT |
| category_id | UUID | FK -> categories.id, ON DELETE RESTRICT |
| name | VARCHAR(160) | NOT NULL |
| slug | VARCHAR(180) | NOT NULL |
| description | TEXT | |
| image_url | TEXT | |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, INACTIVE) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricción:**
- UNIQUE(store_id, slug)

**Índices adicionales:**
- INDEX(category_id)
- INDEX(store_id, status)

---

## 15. product_variants

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| product_id | UUID | FK -> products.id, ON DELETE CASCADE |
| presentation_name | VARCHAR(120) | NOT NULL |
| sku | VARCHAR(100) | NULL |
| price_amount | NUMERIC(12,2) | NOT NULL, CHECK > 0 |
| price_currency | CHAR(3) | NOT NULL |
| tracks_inventory | BOOLEAN | NOT NULL |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, INACTIVE) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Índices:**
- INDEX(product_id, status)
- INDEX(sku) WHERE sku IS NOT NULL

**Notas:**
- SKU opcional. Se normaliza a uppercase antes de persistir.
- La unicidad por Tienda se valida en aplicación.

---

## 16. variant_attributes

| Columna | Tipo | Restricciones |
|---|---|---|
| variant_id | UUID | PK, FK -> product_variants.id, ON DELETE CASCADE |
| name | VARCHAR(120) | PK |
| value | TEXT | NOT NULL |

**PK compuesta:** (variant_id, name)

---

## 17. inventories

| Columna | Tipo | Restricciones |
|---|---|---|
| variant_id | UUID | PK, FK -> product_variants.id, ON DELETE CASCADE |
| quantity | INTEGER | NOT NULL, CHECK >= 0 |
| low_stock_threshold | INTEGER | CHECK >= 0 |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones:**
- (low_stock_threshold IS NULL) OR (low_stock_threshold >= 0)

---

## 18. carts

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK -> users.id, ON DELETE CASCADE |
| store_id | UUID | FK -> stores.id, ON DELETE CASCADE |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricción:**
- UNIQUE(user_id, store_id)

---

## 19. cart_items

| Columna | Tipo | Restricciones |
|---|---|---|
| cart_id | UUID | PK, FK -> carts.id, ON DELETE CASCADE |
| variant_id | UUID | PK, FK -> product_variants.id, ON DELETE CASCADE |
| quantity | INTEGER | NOT NULL, CHECK > 0 |
| reference_price_amount | NUMERIC(12,2) | NOT NULL, CHECK >= 0 |
| reference_price_currency | CHAR(3) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**PK compuesta:** (cart_id, variant_id)

**Nota:** reference_price NO es precio contractual. Checkout debe revalidar precio vigente.

---

## 20. discounts

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| store_id | UUID | FK -> stores.id, ON DELETE RESTRICT |
| name | VARCHAR(160) | NOT NULL |
| type | VARCHAR(30) | NOT NULL, CHECK IN (PERCENTAGE, FIXED_AMOUNT) |
| modality | VARCHAR(30) | NOT NULL, CHECK IN (AUTOMATIC, CODE) |
| value | NUMERIC(12,2) | NOT NULL, CHECK > 0 |
| code | VARCHAR(80) | NULL |
| minimum_purchase_amount | NUMERIC(12,2) | CHECK >= 0 |
| starts_at | TIMESTAMPTZ | |
| ends_at | TIMESTAMPTZ | |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, INACTIVE) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones conceptuales:**
- type = PERCENTAGE → value <= 100
- modality = CODE → code NOT NULL
- modality = AUTOMATIC → code IS NULL
- ends_at > starts_at (si ambos existen)

**UNIQUE parcial:**
```
UNIQUE(store_id, code) WHERE code IS NOT NULL
```

**Índice:** INDEX(store_id, modality, status)

**Normalización de código:** trim + uppercase antes de persistir.

---

## 21. capacity_configurations

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| store_id | UUID | FK -> stores.id, ON DELETE RESTRICT |
| day_of_week | INTEGER | NOT NULL, CHECK 0-6 |
| start_time | TIME | |
| end_time | TIME | |
| max_capacity | INTEGER | NOT NULL, CHECK >= 0 |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, INACTIVE) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones:**
- (start_time IS NULL) = (end_time IS NULL)
- start_time < end_time (si existen)

**Índice:**
```
INDEX(store_id, day_of_week, status, start_time, end_time)
```

---

## 22. capacity_exceptions

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| store_id | UUID | FK -> stores.id, ON DELETE RESTRICT |
| service_date | DATE | NOT NULL |
| start_time | TIME | |
| end_time | TIME | |
| exception_capacity | INTEGER | NOT NULL, CHECK >= 0 |
| reason | TEXT | |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, INACTIVE) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones:**
- (start_time IS NULL) = (end_time IS NULL)
- start_time < end_time (si existen)

**Índice:** INDEX(store_id, service_date, status)

---

## 23. capacity_reservations

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| store_id | UUID | FK -> stores.id, ON DELETE RESTRICT |
| user_id | UUID | FK -> users.id, ON DELETE RESTRICT |
| service_date | DATE | NOT NULL |
| start_time | TIME | |
| end_time | TIME | |
| status | VARCHAR(30) | NOT NULL, CHECK IN (ACTIVE, PAYMENT_PROTECTED, COMMITTED, EXPIRED, RELEASED) |
| expires_at | TIMESTAMPTZ | NOT NULL |
| payment_protection_expires_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Consumen capacidad:** ACTIVE, PAYMENT_PROTECTED, COMMITTED

**No consumen:** EXPIRED, RELEASED

**Índices:**
- INDEX(store_id, service_date, start_time, end_time, status)
- INDEX(expires_at) WHERE status = 'ACTIVE'
- INDEX(payment_protection_expires_at) WHERE status = 'PAYMENT_PROTECTED'

---

## 24. payments

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| capacity_reservation_id | UUID | FK -> capacity_reservations.id, UNIQUE, ON DELETE RESTRICT |
| amount | NUMERIC(12,2) | NOT NULL, CHECK > 0 |
| currency | CHAR(3) | NOT NULL, CHECK = 'PEN' |
| status | VARCHAR(30) | NOT NULL, CHECK IN (PENDING, PROCESSING, APPROVED, FAILED) |
| refund_status | VARCHAR(30) | NOT NULL, CHECK IN (NOT_REQUIRED, PENDING, PROCESSING, COMPLETED, FAILED) |
| refund_amount | NUMERIC(12,2) | CHECK >= 0 |
| refund_reference | VARCHAR(100) | NULL |
| approved_at | TIMESTAMPTZ | NULL |
| refunded_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones:**
- refund_amount <= amount

**Un Pago puede contener múltiples IntentoPago. Un Pago aprobado genera como máximo un Pedido.**

**Protección de idempotencia Pago → Pedido:** UNIQUE(orders.payment_id)

---

## 25. payment_attempts

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| payment_id | UUID | FK -> payments.id, ON DELETE CASCADE |
| attempt_number | SMALLINT | NOT NULL, CHECK > 0 |
| provider | VARCHAR(30) | NOT NULL, CHECK = 'MERCADO_PAGO' |
| payment_method | VARCHAR(30) | NOT NULL, CHECK = 'CARD' |
| idempotency_key | VARCHAR(64) | NOT NULL |
| provider_payment_id | VARCHAR(100) | NULL |
| provider_status | VARCHAR(50) | NULL |
| provider_status_detail | VARCHAR(100) | NULL |
| status | VARCHAR(30) | NOT NULL, CHECK IN (CREATED, PROCESSING, APPROVED, REJECTED, ERROR) |
| error_code | VARCHAR(100) | NULL |
| error_message | TEXT | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| completed_at | TIMESTAMPTZ | NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones:**
- UNIQUE(payment_id, attempt_number)
- UNIQUE(provider, idempotency_key)

**Constraints adicionales:**
- UNIQUE(provider, provider_payment_id) WHERE provider_payment_id IS NOT NULL

**Relación:** IntentoPago pertenece a un Pago. payment_id es obligatorio.

**Semántica de completed_at:**
- CREATED / PROCESSING → completed_at permanece NULL
- APPROVED / REJECTED / ERROR → completed_at contiene el instante de finalización

**Nota:** Esta coherencia es una invariante de aplicación.

### Conceptos diferenciados

| Concepto | Descripción | Ejemplo |
|---|---|---|
| idempotency_key | Identificador generado por JaldiShop para una operación lógica | `550e8400-e29b-41d4-a716-446655440000` |
| provider_payment_id | Identificador generado por Mercado Pago para la operación externa | `1234567890` |
| provider_status | Estado reportado por el proveedor (opcional, informativo) | `approved` |
| provider_status_detail | Detalle adicional del estado del proveedor | `accredited` |

### idempotency_key

- Generada por JaldiShop antes de iniciar la operación con el proveedor.
- Identifica una operación lógica concreta contra Mercado Pago.
- Se conserva durante reintentos técnicos de la misma operación.
- Un retry técnico reutiliza la MISMA idempotency_key y el MISMO IntentoPago.
- Un nuevo intento del cliente después de un resultado definitivo utiliza una NUEVA idempotency_key.

### provider_payment_id

- Identificador asignado por Mercado Pago a la operación.
- Se almacena NULL inicialmente porque JaldiShop puede crear el IntentoPago antes de recibir respuesta.
- NO se aplica ninguna transformación: se conserva exactamente como es recibido.
- UNIQUE parcial evita asociar accidentalmente una misma operación externa a múltiples IntentoPago.

### Estados internos vs Estados del proveedor

- **status:** Estado interno de JaldiShop (CREATED, PROCESSING, APPROVED, REJECTED, ERROR)
- **provider_status / provider_status_detail:** Información opcional recibida del proveedor

**No se copian directamente los estados de Mercado Pago como estados del dominio.**

---

## 27. Integración con Mercado Pago (MVP)

### Proveedor del MVP

**Decisión cerrada:** Mercado Pago es el proveedor de pagos del MVP.

- payment_attempts.provider = 'MERCADO_PAGO'
- El dominio permanece conceptualmente desacoplado del proveedor
- La primera implementación utiliza Mercado Pago

### Credenciales

**NO se almacenan en las tablas del dominio:**
- Access Token de Mercado Pago
- Public Key
- Client Secret
- Datos completos de tarjeta
- CVV
- Secretos de webhook

Las credenciales pertenecen a configuración/secrets del backend.

### Idempotencia y reintentos

| Escenario | Comportamiento |
|---|---|
| Retry técnico (timeout sin respuesta conocida) | Mismo IntentoPago + misma idempotency_key |
| Nuevo intento después de REJECTED/ERROR | Nuevo IntentoPago + nueva idempotency_key |
| Notificación duplicada del webhook | Confirmación idempotente, sin efectos duplicados |

### Confirmación idempotente

La recepción repetida de una confirmación o resultado correspondiente al mismo Pago no puede:
- Crear múltiples Pedidos
- Volver a aplicar los efectos de una compra confirmada

**Efectos protegidos:**
- Creación del Pedido
- Descuento definitivo de inventario
- Compromiso definitivo de capacidad
- Aplicación del descuento
- Notificaciones derivadas

**Protección disponible:** UNIQUE(orders.payment_id)

### Webhooks

- No se crea tabla webhook_events en el MVP
- No se crea infraestructura WebSocket/STOMP relacionada con pagos
- Mercado Pago puede comunicar actualizaciones de forma asíncrona
- La integración debe tolerar notificaciones repetidas
- Los mecanismos HTTP/webhook pertenecen a infraestructura, no al dominio

---

## 26. orders

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| order_number | VARCHAR(50) | UNIQUE |
| user_id | UUID | FK -> users.id, ON DELETE RESTRICT |
| store_id | UUID | FK -> stores.id, ON DELETE RESTRICT |
| payment_id | UUID | FK -> payments.id, UNIQUE, ON DELETE RESTRICT |
| capacity_reservation_id | UUID | FK -> capacity_reservations.id, UNIQUE, ON DELETE RESTRICT |
| status | VARCHAR(30) | NOT NULL, CHECK IN (CONFIRMED, IN_PREPARATION, READY, OUT_FOR_DELIVERY, COMPLETED, CANCELLED) |
| delivery_mode | VARCHAR(30) | NOT NULL, CHECK IN (PICKUP, DELIVERY) |
| service_date | DATE | NOT NULL |
| service_start_time | TIME | |
| service_end_time | TIME | |
| customer_name | VARCHAR(200) | NOT NULL |
| customer_phone | VARCHAR(30) | NOT NULL |
| customer_email | VARCHAR(254) | NOT NULL |
| delivery_address | TEXT | |
| delivery_reference | TEXT | |
| delivery_latitude | NUMERIC(9,6) | CHECK BETWEEN -90 AND 90 |
| delivery_longitude | NUMERIC(9,6) | CHECK BETWEEN -180 AND 180 |
| currency | CHAR(3) | NOT NULL, CHECK = 'PEN' |
| products_subtotal_amount | NUMERIC(12,2) | NOT NULL, CHECK >= 0 |
| discount_amount | NUMERIC(12,2) | NOT NULL, CHECK >= 0 |
| discount_code | VARCHAR(80) | NULL |
| delivery_fee_amount | NUMERIC(12,2) | NOT NULL, CHECK >= 0 |
| included_tax_amount | NUMERIC(12,2) | NOT NULL, CHECK >= 0 |
| total_amount | NUMERIC(12,2) | NOT NULL, CHECK >= 0 |
| confirmed_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones:**
- delivery_mode = DELIVERY → delivery_address NOT NULL
- service_start_time < service_end_time (si existen)
- discount_amount <= products_subtotal_amount

**Fórmula:** total = products_subtotal_amount - discount_amount + delivery_fee_amount

**Índices:**
- INDEX(store_id, confirmed_at DESC)
- INDEX(user_id, confirmed_at DESC)
- INDEX(store_id, status)
- INDEX(store_id, service_date)

---

## 27. order_items

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| order_id | UUID | FK -> orders.id, ON DELETE CASCADE |
| variant_id | UUID | FK -> product_variants.id, ON DELETE RESTRICT |
| product_name | VARCHAR(160) | NOT NULL |
| variant_name | VARCHAR(120) | NOT NULL |
| attributes_snapshot | JSONB | |
| quantity | INTEGER | NOT NULL, CHECK > 0 |
| unit_price_amount | NUMERIC(12,2) | NOT NULL, CHECK >= 0 |
| subtotal_amount | NUMERIC(12,2) | NOT NULL, CHECK >= 0 |

**Índices:**
- INDEX(order_id)
- INDEX(variant_id)

---

## 28. order_status_history

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| order_id | UUID | FK -> orders.id, ON DELETE CASCADE |
| changed_by_user_id | UUID | FK -> users.id, ON DELETE SET NULL |
| status | VARCHAR(30) | NOT NULL |
| reason | TEXT | |
| changed_at | TIMESTAMPTZ | NOT NULL |

**Índice:** INDEX(order_id, changed_at)

---

## 29. favorites

| Columna | Tipo | Restricciones |
|---|---|---|
| user_id | UUID | PK, FK -> users.id, ON DELETE CASCADE |
| product_id | UUID | PK, FK -> products.id, ON DELETE CASCADE |
| created_at | TIMESTAMPTZ | NOT NULL |

**PK compuesta:** (user_id, product_id)

---

## 30. reviews

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK -> users.id, ON DELETE RESTRICT |
| product_id | UUID | FK -> products.id, ON DELETE RESTRICT |
| rating | INTEGER | NOT NULL, CHECK >= 1 AND <= 5 |
| comment | TEXT | |
| status | VARCHAR(30) | NOT NULL, CHECK IN (PUBLISHED, HIDDEN) |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

**Restricciones:**
- UNIQUE(user_id, product_id)

**Índice:** INDEX(product_id, status)

---

## 31. notifications

| Columna | Tipo | Restricciones |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK -> users.id, ON DELETE CASCADE |
| type | VARCHAR(30) | NOT NULL, CHECK IN (NEW_ORDER, ORDER_STATUS_CHANGED, LOW_STOCK, SYSTEM) |
| title | VARCHAR(180) | NOT NULL |
| message | TEXT | NOT NULL |
| status | VARCHAR(30) | NOT NULL, CHECK IN (UNREAD, READ) |
| created_at | TIMESTAMPTZ | NOT NULL |
| read_at | TIMESTAMPTZ | |

**Restricciones semánticas:**
- status = 'UNREAD' → read_at IS NULL
- status = 'READ' → read_at IS NOT NULL

**Índices:**
- INDEX(user_id, created_at DESC)
- INDEX(user_id, created_at DESC) WHERE status = 'UNREAD'

---

## 32. Generación de order_number

**Formato:** `JAL-{AÑO}-{SECUENCIA}`

**Ejemplo:** `JAL-2026-000184`

### Estrategia

1. PostgreSQL SEQUENCE dedicada para la parte secuencial
2. Backend construye el valor visible
3. La secuencia NO se reinicia anualmente

**Ejemplo de generación:**
```
JAL-2026-000184  (2026)
JAL-2027-000185  (2027, continúa secuencia)
```

**Notas:**
- Los huecos en la secuencia son aceptables
- No reutilizar números de transacciones fallidas
- UNIQUE(order_number) como protección final

---

## 33. Política ON UPDATE

**Decisión cerrada:** `ON UPDATE NO ACTION` para todas las FK.

Las PK/UUID no deben modificarse una vez creadas.

---

## 34. Matriz ON DELETE

| Columna FK | Referencia | ON DELETE |
|---|---|---|
| user_roles.user_id | users.id | CASCADE |
| user_roles.role_id | roles.id | RESTRICT |
| stores.merchant_user_id | users.id | RESTRICT |
| categories.store_id | stores.id | RESTRICT |
| products.store_id | stores.id | RESTRICT |
| products.category_id | categories.id | RESTRICT |
| product_variants.product_id | products.id | CASCADE |
| variant_attributes.variant_id | product_variants.id | CASCADE |
| inventories.variant_id | product_variants.id | CASCADE |
| carts.user_id | users.id | CASCADE |
| carts.store_id | stores.id | CASCADE |
| cart_items.cart_id | carts.id | CASCADE |
| cart_items.variant_id | product_variants.id | CASCADE |
| discounts.store_id | stores.id | RESTRICT |
| capacity_configurations.store_id | stores.id | RESTRICT |
| capacity_exceptions.store_id | stores.id | RESTRICT |
| capacity_reservations.store_id | stores.id | RESTRICT |
| capacity_reservations.user_id | users.id | RESTRICT |
| payments.capacity_reservation_id | capacity_reservations.id | RESTRICT |
| payment_attempts.payment_id | payments.id | CASCADE |
| orders.user_id | users.id | RESTRICT |
| orders.store_id | stores.id | RESTRICT |
| orders.payment_id | payments.id | RESTRICT |
| orders.capacity_reservation_id | capacity_reservations.id | RESTRICT |
| order_items.order_id | orders.id | CASCADE |
| order_items.variant_id | product_variants.id | RESTRICT |
| order_status_history.order_id | orders.id | CASCADE |
| order_status_history.changed_by_user_id | users.id | SET NULL |
| favorites.user_id | users.id | CASCADE |
| favorites.product_id | products.id | CASCADE |
| reviews.user_id | users.id | RESTRICT |
| reviews.product_id | products.id | RESTRICT |
| notifications.user_id | users.id | CASCADE |

---

## 35. Justificación de Borrado

### CASCADE — Componentes internos/efímeros

Producto → Variant → Attributes → Inventory

Si un Producto nunca tuvo Pedidos, puede eliminarse físicamente con CASCADE sobre sus componentes.

### RESTRICT — Histórico/transaccional

Product → Variant → OrderItem

OrderItem.variant_id tiene RESTRICT, bloqueando la eliminación de Variant si tiene historial.

**Resultado:** El Producto debe pasar a INACTIVE en lugar de eliminarse.

### SET NULL — Referencia histórica opcional

order_status_history.changed_by_user_id → users.id

El usuario puede eliminarse, pero queremos mantener el historial de quién hizo cada transición.

---

## 36. Slug: Generación y Estabilidad

### Generación

Backend genera slugs con las siguientes reglas:
1. trim
2. lowercase
3. normalizar diacríticos cuando sea posible
4. espacios/separadores → guion
5. eliminar caracteres no permitidos
6. evitar guiones duplicados
7. evitar guiones al inicio/final

**Ejemplo:** `"Café & Postres"` → `"cafe-postres"`

### Estabilidad

**Decisión cerrada:** El slug se genera al crear la Tienda/Producto.

Cambiar el nombre NO regenera automáticamente el slug.

**Ejemplo:**
```
name: "Torta Tres Leches"
slug: "torta-tres-leches"

name: "Torta Tres Leches Premium"
slug: "torta-tres-leches"  (sin cambios)
```

**Motivo:** No romper URLs existentes.

**Nota:** No implementar historial/redirecciones de slug en el MVP.

---

## 37. Resumen de Índices Adicionales

| Tabla | Índice | Propósito |
|---|---|---|
| user_roles | INDEX(role_id) | Consultar usuarios por rol |
| categories | INDEX(store_id) | Categorías por tienda |
| categories | UNIQUE(store_id, lower(trim(name))) WHERE status = 'ACTIVE' | Unicidad de nombre normalizado |
| products | INDEX(category_id) | Productos por categoría |
| products | INDEX(store_id, status) | Productos por tienda y estado |
| product_variants | INDEX(product_id, status) | Variantes por producto y estado |
| product_variants | INDEX(sku) WHERE sku IS NOT NULL | Búsqueda/validación SKU |
| discounts | INDEX(store_id, modality, status) | Descuentos por tienda y modalidad |
| capacity_configurations | INDEX(store_id, day_of_week, status, start_time, end_time) | Configuraciones por tienda y día |
| capacity_exceptions | INDEX(store_id, service_date, status) | Excepciones por tienda y fecha |
| capacity_reservations | INDEX(store_id, service_date, start_time, end_time, status) | Disponibilidad por periodo |
| capacity_reservations | INDEX(expires_at) WHERE status = 'ACTIVE' | Limpieza de reservas expiradas |
| capacity_reservations | INDEX(payment_protection_expires_at) WHERE status = 'PAYMENT_PROTECTED' | Limpieza de protecciones expiradas |
| payment_attempts | UNIQUE(payment_id, attempt_number) | Numeración ordinal por Pago |
| payment_attempts | UNIQUE(provider, idempotency_key) | Idempotencia de operaciones |
| payment_attempts | UNIQUE(provider, provider_payment_id) WHERE provider_payment_id IS NOT NULL | Evitar duplicación de operaciones externas |
| payment_attempts | INDEX(payment_id) | Consultas por Pago |
| orders | INDEX(store_id, confirmed_at DESC) | Pedidos por tienda |
| orders | INDEX(user_id, confirmed_at DESC) | Pedidos por cliente |
| orders | INDEX(store_id, status) | Pedidos por estado |
| orders | INDEX(store_id, service_date) | Pedidos por fecha de atención |
| order_items | INDEX(order_id) | Items por pedido |
| order_items | INDEX(variant_id) | Trazabilidad de variante |
| order_status_history | INDEX(order_id, changed_at) | Historial por pedido |
| reviews | INDEX(product_id, status) | Reseñas por producto |
| notifications | INDEX(user_id, created_at DESC) | Notificaciones por usuario |
| notifications | INDEX(user_id, created_at DESC) WHERE status = 'UNREAD' | Notificaciones pendientes |

**Tablas sin índice adicional** (cubiertos por PK/UNIQUE):
- users
- roles
- stores
- variant_attributes
- inventories
- carts
- cart_items
- payments
- favorites

---

## 38. UNIQUE / Integridad vs INDEX / Rendimiento

**UNIQUE y PRIMARY KEY:**
- Son principalmente mecanismos de integridad
- PostgreSQL crea sus índices automáticamente

**INDEX:**
- Se agrega para acelerar consultas
- No representa por sí mismo una regla de negocio

**FK:**
- Protege integridad referencial
- PostgreSQL no crea automáticamente índice para la columna referenciante

**Evitar índices duplicados** cuando PK/UNIQUE ya cubre exactamente el mismo prefijo de consulta.

---

## 39. Normalización 3FN

El modelo se considera conceptualmente compatible con 3FN.

**Separaciones normalizadas:**
- users / roles / user_roles
- stores / categories
- products / product_variants
- variant_attributes por variante
- inventories separado de catálogo
- carts / cart_items
- payments / payment_attempts
- orders / order_items / status_history
- favorites como relación
- reviews como entidad propia

**Snapshots deliberadamente desnormalizados:**

| Entidad | Atributos históricos |
|---|---|
| orders | customer_name, customer_phone, customer_email, delivery_address, valores monetarios |
| order_items | product_name, variant_name, attributes_snapshot, unit_price_amount, subtotal_amount |

**Justificación:** Estos campos representan hechos históricos propios de una transacción y no deben reconstruirse con datos actuales. No deben tratarse como errores de normalización.

---

## 40. Conceptos Sin Tabla

**Value Objects / conceptos sin tabla propia:**

| Concepto | Tratamiento |
|---|---|
| Dinero | Atributos sueltos (amount, currency) |
| UbicacionTienda | Atributos en stores |
| ConfiguracionEntrega | Atributos sueltos en stores |
| ConfiguracionTributaria | Atributos sueltos en stores |
| PeriodoCapacidad | Atributos sueltos (start_time, end_time) |
| DatosClientePedido | Atributos en orders (customer_*) |
| DireccionEntrega | Atributos en orders (delivery_*) |
| ResumenMonetario | Atributos en orders (*_amount, total_amount) |
| CodigoDescuento | Atributo code en discounts (normalizado uppercase) |
| AtributoVariante | Tabla variant_attributes |
| ReputacionProducto | Calculada, no almacenada |
| CapacidadDisponible | Calculada, no almacenada |
| Checkout | Proceso, no tabla |
| SeguimientoPedido | Derivado de orders + history |
| HistorialPedidos | Consulta derivada |
| Entrega | Modalidad, no tabla |
| WebSocket | Infraestructura |
| STOMP | Protocolo de transporte |
| Maps | Integración externa |
| GPS tracking | Infraestructura |

---

## 41. Reglas de Aplicación (No Impuestas por SQL)

Las siguientes reglas permanecen en dominio/aplicación y no como constraints de base de datos:

1. merchant_user_id debe pertenecer a un Usuario con rol MERCHANT
2. Producto debe tener al menos una Variante
3. SKU único dentro de la Tienda (cuando existe)
4. Categorías activas con nombre normalizado equivalente único dentro de la Tienda (invariante de aplicación, no columna)
5. Configuraciones de capacidad activas no solapadas por tienda/día
6. Excepciones de capacidad activas no solapadas por tienda/fecha
7. Prioridad de excepción full-day > franja > base
8. Máximo un descuento automático vigente/aplicable por Tienda
9. Código válido tiene prioridad sobre descuento automático
10. Carrito solo contiene variantes de su misma Tienda
11. Stock de múltiples variantes se descuenta en una misma transacción
12. Reseña requiere Pedido COMPLETED con ese Producto
13. Tienda operativa requiere al menos pickup_enabled o delivery_enabled
14. Reintentos de Pago no extienden protección de capacidad
15. ConfirmPurchase debe ser idempotente
16. CancelOrder debe ser idempotente
17. Email almacenado en forma canónica (lowercase, trim)
18. Slug generado al crear, no regenerado al modificar nombre
19. JaldiShop genera idempotency_key antes de iniciar operación con Mercado Pago
20. Retry técnico de la misma operación conserva la misma idempotency_key
21. Nuevo intento después de resultado definitivo utiliza nueva idempotency_key
22. provider_payment_id se conserva exactamente como es recibido (sin transformación)
23. Estados de Mercado Pago se traducen a estados internos de JaldiShop
24. Un mismo Pago aprobado no puede generar múltiples Pedidos
25. Notificaciones repetidas del proveedor no deben repetir efectos de confirmación
26. Credenciales de Mercado Pago nunca se persisten en tablas del dominio
27. tracks_inventory = true requiere Inventory asociado
28. Variant con tracks_inventory = false no participa en disponibilidad comercial
29. 1 Pedido = 1 Cupo en el MVP

---

## 41.1 Invariantes de Aplicación / Backend

Las siguientes invariantes son verificables y mantenidas por la capa de aplicación/backend, no por constraints de base de datos:

**Normalización:**
- email: trim + lowercase antes de persistir
- discount code: trim + uppercase antes de persistir
- SKU: trim + uppercase antes de persistir
- category name: trim + lowercase + normalizar espacios antes de verificar unicidad

**Integridad de catálogo:**
- Producto vendible debe tener al menos una Variant ACTIVE
- Variant con tracks_inventory = true debe tener Inventory asociado
- SKU único dentro de la misma Tienda (no solo único global)

**Carrito y Store:**
- Toda Variant agregada a Cart debe pertenecer al Product de la misma Store del Cart
- CartItem.quantity > 0; si llega a cero, eliminar el CartItem

**Capacidad:**
- Configuraciones de capacidad activas del mismo día/franja no deben solaparse dentro de una Tienda
- Excepciones activas no deben solaparse por fecha dentro de una Tienda
- Excepción de día completo tiene prioridad sobre franja
- Excepción reemplaza capacidad base configurada
- Una excepción debe corresponder coherentemente al periodo configurado

**Descuentos:**
- Máximo un descuento por compra
- Máximo un descuento automático simultáneamente vigente por Tienda
- Código válido tiene prioridad sobre descuento automático
- code IS NOT NULL cuando modality = CODE
- code IS NULL cuando modality = AUTOMATIC

**Pagos:**
- Retry técnico reutiliza el mismo PaymentAttempt e idempotency_key
- Nuevo intento financiero utiliza nuevo PaymentAttempt y nueva idempotency_key
- ConfirmPurchase es atómico e idempotente
- CancelOrder es idempotente
- Efectos de confirmación no se repiten ante notificaciones duplicadas del proveedor

**Revisión de elegibilidad:**
- Review solo puede ser creada por usuario que tuvo Order COMPLETED conteniendo ese Product

**Stores operativos:**
- Tienda operativa requiere al menos pickup_enabled = true o delivery_enabled = true

---

## 42. Decisiones Cerradas

Las siguientes decisiones están **CERRADAS** y no deben reabrirse:

| Decisión | Estado |
|---|---|
| UUID para entidades principales | ✅ Cerrada |
| UUID generado por JPA, no PostgreSQL | ✅ Cerrada |
| SMALLINT para roles.id | ✅ Cerrada |
| PK compuesta para user_roles, variant_attributes, cart_items, favorites | ✅ Cerrada |
| FK variant_id como PK de inventories | ✅ Cerrada |
| NUMERIC(12,2) para dinero | ✅ Cerrada |
| NUMERIC(5,2) para porcentajes | ✅ Cerrada |
| NUMERIC(9,6) para coordenadas | ✅ Cerrada |
| TIMESTAMPTZ para auditoría | ✅ Cerrada |
| VARCHAR + CHECK para estados (no ENUM) | ✅ Cerrada |
| Email normalizado lowercase por aplicación | ✅ Cerrada |
| Slug generado por backend, estable | ✅ Cerrada |
| categories.normalized_name eliminado (índice funcional代替) | ✅ Cerrada v1.6 |
| Código descuento normalizado uppercase | ✅ Cerrada |
| SKU normalizado uppercase | ✅ Cerrada |
| order_number con SEQUENCE, formato JAL-AAAA-NNNNNN | ✅ Cerrada |
| JSONB solo para attributes_snapshot | ✅ Cerrada |
| ON UPDATE NO ACTION global | ✅ Cerrada |
| Índice parcial para SKU | ✅ Cerrada |
| **Proveedor de pagos MVP: Mercado Pago** | ✅ Cerrada |
| **idempotency_key generada por JaldiShop** | ✅ Cerrada |
| **idempotency_key pertenece a IntentoPago** | ✅ Cerrada |
| **Retry técnico conserva misma idempotency_key** | ✅ Cerrada |
| **Nuevo intento usa nueva idempotency_key** | ✅ Cerrada |
| **provider_payment_id separado de idempotency_key** | ✅ Cerrada |
| **provider_payment_id VARCHAR(100), sin transformación** | ✅ Cerrada |
| **Estados internos separados de estados del proveedor** | ✅ Cerrada |
| **UNIQUE(provider, idempotency_key)** | ✅ Cerrada |
| **UNIQUE parcial de provider_payment_id** | ✅ Cerrada |
| **UNIQUE(orders.payment_id)** | ✅ Cerrada |
| **Confirmación de compra idempotente** | ✅ Cerrada |
| **Credenciales del proveedor fuera del modelo relacional** | ✅ Cerrada |
| **password_encoded VARCHAR(255) NOT NULL** | ✅ Cerrada |
| **Flyway utilizado desde migración inicial** | ✅ Cerrada |
| **stores.status: ACTIVE, INACTIVE, SUSPENDED, CLOSED** | ✅ Cerrada |
| **orders.status: CONFIRMED, IN_PREPARATION, READY, OUT_FOR_DELIVERY, COMPLETED, CANCELLED** | ✅ Cerrada |
| **payment_attempts.status: CREATED, PROCESSING, APPROVED, REJECTED, ERROR** | ✅ Cerrada |
| **currency CHECK = 'PEN' para payments y orders** | ✅ Cerrada |
| **provider CHECK = 'MERCADO_PAGO'** | ✅ Cerrada |
| **payment_method CHECK = 'CARD'** | ✅ Cerrada |
| **payment_attempts.attempt_number SMALLINT CHECK > 0 + UNIQUE(payment_id, attempt_number)** | ✅ Cerrada |
| **payment_attempts.completed_at TIMESTAMPTZ NULL como invariante de aplicación** | ✅ Cerrada |
| **Flyway forward-only + backup/restore para rollback** | ✅ Cerrada |

---

## 43. Pendientes Reales Antes del DDL

Después de esta actualización, quedan como pendientes:

1. ~~Revisión final de nombres físicos exactos de tablas/columnas~~ ✅ Completada en v1.3
2. ~~Confirmar valores VARCHAR exactos de todos los CHECK de estados~~ ✅ Cerrado en v1.4
3. ~~Revisar si se utilizará Flyway desde el primer commit de esquema~~ ✅ Cerrado: Flyway será utilizado
4. ~~Definir estrategia de rollback para migraciones Flyway~~ ✅ Cerrado: forward-only + backup/restore
5. Generar diagrama ER final → **EN PROGRESO** (asignado Sprint 2)
6. Generar V1__initial_schema.sql → **EN PROGRESO** (asignado Sprint 2)
7. Ejecutar migración sobre Neon PostgreSQL → Pendiente post-DDL
8. Revisar errores/restricciones reales del DDL → Pendiente post-DDL
9. Validar consultas críticas con EXPLAIN ANALYZE posteriormente → Pendiente post-DDL
10. Realizar mapeo JPA después de estabilizar el esquema → Pendiente post-DDL
11. ~~Definir longitud exacta de password_hash según algoritmo~~ ✅ Cerrado: VARCHAR(255)
12. Diseño técnico del procesamiento de webhooks de Mercado Pago → Pendiente futura

**NO vuelven a listarse como pendientes:**
- UUID vs BIGINT (cerrado)
- Tipos físicos base (cerrados)
- Estrategia dinero NUMERIC (cerrada)
- Estrategia timestamps (cerrada)
- VARCHAR + CHECK vs ENUM (cerrado)
- Políticas de normalización case-insensitive (cerradas)
- ON DELETE de las FK (cerrada)
- ON UPDATE NO ACTION (cerrada)
- Uso de JSONB solo en snapshot (cerrado)
- Índice parcial de SKU (cerrado)
- **Proveedor de pagos MVP (cerrado: Mercado Pago)**
- **Estrategia de idempotencia de pagos (cerrada)**
- **Diferenciación idempotency_key / provider_payment_id (cerrada)**
- **Flyway desde primer commit (cerrado)**
- **Estrategia rollback Flyway (cerrada: forward-only + backup)**
- **password_hash VARCHAR(255) (cerrado)**

---

## 44. Defaults

Candidates seguros/obvios:

| Columna | Default |
|---|---|
| created_at | CURRENT_TIMESTAMP |
| updated_at | CURRENT_TIMESTAMP (inicial) |
| refund_amount | 0 |
| refund_status | 'NOT_REQUIRED' al crear Pago |
| notification.status | 'UNREAD' al crear |
| read_at | NULL inicialmente |

**No asignar defaults indiscriminadamente** para booleanos si la ausencia debe detectarse.

---

## 46. Inventario Completo de CHECK

### CHECK de Valores Discretos (VARCHAR + CHECK)

| Tabla | Columna | Tipo | Valores |
|---|---|---|---|
| users | status | VARCHAR(30) | ACTIVE, INACTIVE, SUSPENDED |
| stores | status | VARCHAR(30) | ACTIVE, INACTIVE, SUSPENDED, CLOSED |
| categories | status | VARCHAR(30) | ACTIVE, INACTIVE |
| products | status | VARCHAR(30) | ACTIVE, INACTIVE |
| product_variants | status | VARCHAR(30) | ACTIVE, INACTIVE |
| discounts | type | VARCHAR(30) | PERCENTAGE, FIXED_AMOUNT |
| discounts | modality | VARCHAR(30) | AUTOMATIC, CODE |
| discounts | status | VARCHAR(30) | ACTIVE, INACTIVE |
| capacity_configurations | status | VARCHAR(30) | ACTIVE, INACTIVE |
| capacity_exceptions | status | VARCHAR(30) | ACTIVE, INACTIVE |
| capacity_reservations | status | VARCHAR(30) | ACTIVE, PAYMENT_PROTECTED, COMMITTED, EXPIRED, RELEASED |
| payments | status | VARCHAR(30) | PENDING, PROCESSING, APPROVED, FAILED |
| payments | refund_status | VARCHAR(30) | NOT_REQUIRED, PENDING, PROCESSING, COMPLETED, FAILED |
| payments | currency | CHAR(3) | PEN |
| payment_attempts | provider | VARCHAR(30) | MERCADO_PAGO |
| payment_attempts | payment_method | VARCHAR(30) | CARD |
| payment_attempts | status | VARCHAR(30) | CREATED, PROCESSING, APPROVED, REJECTED, ERROR |
| orders | status | VARCHAR(30) | CONFIRMED, IN_PREPARATION, READY, OUT_FOR_DELIVERY, COMPLETED, CANCELLED |
| orders | delivery_mode | VARCHAR(30) | PICKUP, DELIVERY |
| orders | currency | CHAR(3) | PEN |
| reviews | status | VARCHAR(30) | PUBLISHED, HIDDEN |
| notifications | type | VARCHAR(30) | NEW_ORDER, ORDER_STATUS_CHANGED, LOW_STOCK, SYSTEM |
| notifications | status | VARCHAR(30) | UNREAD, READ |
| roles | name | VARCHAR(20) | CUSTOMER, MERCHANT, ADMIN |

### CHECK Numéricos y Estructurales

| Tabla | Columna | Constraint |
|---|---|---|
| inventories | quantity | >= 0 |
| inventories | low_stock_threshold | IS NULL OR >= 0 |
| cart_items | quantity | > 0 |
| cart_items | reference_price_amount | >= 0 |
| product_variants | price_amount | > 0 |
| discounts | value | > 0 |
| discounts | value | <= 100 (when type = PERCENTAGE) |
| discounts | minimum_purchase_amount | >= 0 |
| capacity_configurations | day_of_week | 0-6 |
| capacity_configurations | max_capacity | >= 0 |
| capacity_exceptions | exception_capacity | >= 0 |
| payments | amount | > 0 |
| payments | refund_amount | >= 0 |
| payments | refund_amount | <= amount |
| order_items | quantity | > 0 |
| order_items | unit_price_amount | >= 0 |
| order_items | subtotal_amount | >= 0 |
| orders | products_subtotal_amount | >= 0 |
| orders | discount_amount | >= 0 |
| orders | discount_amount | <= products_subtotal_amount |
| orders | delivery_fee_amount | >= 0 |
| orders | included_tax_amount | >= 0 |
| orders | total_amount | >= 0 |
| orders | delivery_latitude | BETWEEN -90 AND 90 |
| orders | delivery_longitude | BETWEEN -180 AND 180 |
| reviews | rating | BETWEEN 1 AND 5 |
| stores | latitude | BETWEEN -90 AND 90 |
| stores | longitude | BETWEEN -180 AND 180 |
| stores | delivery_fee_amount | >= 0 |
| stores | tax_rate | > 0 AND <= 100 |

### CHECK de Coherencia Semántica

| Tabla | Constraint |
|---|---|
| capacity_configurations | (start_time IS NULL) = (end_time IS NULL) |
| capacity_configurations | start_time < end_time (when both exist) |
| capacity_exceptions | (start_time IS NULL) = (end_time IS NULL) |
| capacity_exceptions | start_time < end_time (when both exist) |
| orders | delivery_mode = 'DELIVERY' → delivery_address NOT NULL |
| orders | service_start_time < service_end_time (when both exist) |
| discounts | code IS NOT NULL (when modality = CODE) |
| discounts | code IS NULL (when modality = AUTOMATIC) |
| discounts | ends_at > starts_at (when both exist) |
| notifications | status = 'UNREAD' → read_at IS NULL |
| notifications | status = 'READ' → read_at IS NOT NULL |

---

## 45. Estado del Documento

| Aspecto | Estado |
|---|---|
| Inventario de tablas | ✅ Completo v1.0 |
| Relaciones principales | ✅ Definidas |
| Constraints conceptuales | ✅ Definidos |
| Índices iniciales | ✅ Definidos |
| Normalización 3FN | ✅ Revisada conceptualmente |
| Decisiones físicas | ✅ Cerradas v1.6 |
| Políticas ON DELETE/UPDATE | ✅ Definidas |
| Integración pagos | ✅ Proveedor y estrategia definidos |
| Nomenclatura física | ✅ Congelada v1.3 |
| Valores CHECK | ✅ Cerrados v1.5 |
| categories.normalized_name | ✅ Eliminado en v1.6 (índice funcional) |
| Estrategia Flyway | ✅ Aceptada |
| Estrategia rollback | ✅ Forward-only + backup |
| payment_method | ✅ Cerrado: CARD |
| Invariantes de aplicación | ✅ Documentadas |
| Estado general | ✅ Listo para DDL |

> 📌 El diseño relacional v1.6 de JaldiShop cuenta con tablas, relaciones, PK/FK, constraints conceptuales, índices iniciales, tipos físicos, estrategias de normalización y políticas de eliminación definidas. La columna normalized_name fue eliminada de categories y reemplazada por un índice funcional parcial. Todas las decisiones físicas necesarias para la migración inicial V1 están cerradas. El modelo está listo para generar V1__initial_schema.sql.

---

[⬅ Modelo de Dominio](../03-requisitos/modelo-dominio.md) | [🏠 Índice General](../../README.md)
