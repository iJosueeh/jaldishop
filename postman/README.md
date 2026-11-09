# JaldiShop API — Colección Postman Local y Demostración

Colección Postman v2.1 para Spring Boot 4.1.1 simplificada y organizada para el desarrollo local y demostración de JaldiShop.
Cubre **97 rutas HTTP** con **111 peticiones** en la colección principal y mantiene separados los **494 escenarios negativos y regresiones** en una colección secundaria.

## Estructura de Navegación Principal

La colección principal `jaldishop.postman_collection.json` está organizada en 8 módulos funcionales:

1. **`01. Autenticación`**: Registro y login separados por rol (`Customer`, `Merchant`, `Admin`), refresh token y gestión de perfil.
2. **`02. Customer`**: Gestión de direcciones, lista de favoritos y sistema de reseñas.
3. **`03. Tiendas y catálogo`**: Health check, tiendas destacadas, búsqueda pública, consulta por slug e ID, y listado de productos/categorías.
4. **`04. Carrito y checkout`**: Ciclo del carrito (agregar, actualizar, listar, vaciar), consulta de franjas horarias y disponibilidad, y reserva de checkout.
5. **`05. Pedidos y pagos`**: Consulta y cancelación de pedidos del cliente, integración local de Mercado Pago (fake sandbox) e investigación/reconciliación de pagos administrativos.
6. **`06. Merchant`**: Gestión de tienda propia, catálogo de productos y variantes, control de inventario y stock físico, configuración de capacidad y excepciones, pedidos de la tienda y clientes. Además incluye la administración de usuarios y tiendas de la plataforma.
7. **`07. Notificaciones`**: Listado de notificaciones, conteo de no leídas y marcado como leída.
8. **`08. Regresiones`**: Casos de regresión explícitos (R001 a R011) verificados.

## Separación de Pruebas Negativas

- **Colección Principal (`jaldishop.postman_collection.json`):** Permite desarrollar y demostrar todos los flujos sin barreras. No impone `runCase` ni bloquea peticiones de lectura cotidianas.
- **Colección Secundaria (`jaldishop.regressions.postman_collection.json`):** Contiene los más de 480 escenarios negativos (campos ausentes, nulos, vacíos, validaciones de formato, UUIDs inválidos y casos de seguridad) para pruebas exhaustivas.

## Automatización de Variables y Aislamiento por Rol

Las peticiones exitosas capturan y actualizan automáticamente las variables del entorno:
- `customerToken` y `customerUserId`: Capturados al registrar o iniciar sesión como Customer (`M039`, `M001`).
- `merchantToken` y `merchantUserId`: Capturados al registrar o iniciar sesión como Merchant (`K025`, `M009`).
- `adminToken`: Capturado al iniciar sesión como Admin (`M017`).
- `storeId` y `storeSlug`: Capturados en endpoints de tienda del comerciante (`K058`, `K073`, `K075`). No se sobrescriben en búsquedas públicas.
- `categoryId`: Capturado al crear o listar categorías de tienda (`K095`, `K091`).
- `productId` y `variantId`: Capturados al crear productos o variantes (`K121`, `K154`).
- `cartId`: Capturado al consultar el carrito (`M164`).
- `reservationId` y `orderNumber`: Capturados en checkout y pedidos (`J002`, `M182`, `M217`).
- `addressId`, `capacityConfigId`, `capacityExceptionId`, `notificationId`: Capturados al crear los recursos correspondientes.

Prevención de sesiones obsoletas: Cada login limpia preventivamente el token anterior de su propio rol (`pm.environment.set("<rol>Token", "")`).

## Flujo Demostrativo Customer

1. **Registrar Customer:** `POST /api/v1/auth/register` (captura `customerUserId`).
2. **Iniciar Sesión:** `POST /api/v1/auth/login` (captura `customerToken`).
3. **Consultar Tiendas:** `GET /api/v1/stores/featured` o `GET /api/v1/stores/search?q=pasteleria`.
4. **Consultar Catálogo:** `GET /api/v1/stores/slug/{{storeSlug}}/products` o `GET /api/v1/stores/{{storeId}}/products`.
5. **Agregar al Carrito:** `POST /api/v1/cart/items` con `variantId` y cantidad.
6. **Consultar Carrito:** `GET /api/v1/cart` (captura `cartId`).
7. **Ejecutar Checkout:** `POST /api/v1/checkout` con dirección de entrega y franja horaria.
8. **Consultar Pedido:** `GET /api/v1/customer/orders` y `GET /api/v1/customer/orders/{{orderNumber}}`.

## Flujo Demostrativo Merchant

1. **Registrar Merchant y Tienda:** `POST /api/v1/auth/register/merchant` (captura `merchantToken`, `merchantUserId`).
2. **Iniciar Sesión:** `POST /api/v1/auth/login` con credenciales de comerciante.
3. **Consultar Mi Tienda:** `GET /api/v1/stores/me` (captura `storeId`, `storeSlug`).
4. **Crear Catálogo:**
   - Crear categoría: `POST /api/v1/merchants/stores/{{storeId}}/categories` (captura `categoryId`).
   - Crear producto: `POST /api/v1/merchants/stores/{{storeId}}/products` (captura `productId`).
   - Crear variante: `POST /api/v1/merchants/stores/{{storeId}}/products/{{productId}}/variants` (captura `variantId`).
5. **Configurar Stock y Capacidad:**
   - Actualizar stock: `PUT /api/v1/inventory/variants/{{variantId}}/stock`.
   - Crear configuración de franjas: `POST /api/v1/capacity-configurations` (captura `capacityConfigId`).
6. **Consultar Pedidos:** `GET /api/v1/merchants/stores/{{storeId}}/orders`.
7. **Gestionar Estado:** `PATCH /api/v1/merchants/stores/{{storeId}}/orders/{{orderId}}/status` con `CONFIRMED` o `IN_PREPARATION`.

## Limitaciones Reales: Checkout y Pagos

- **Sin Mercado Pago real en local:** El entorno local de desarrollo no incluye credenciales privadas ni conecta con los servidores externos de Mercado Pago.
- **Protección de endpoints financieros:** Los endpoints de pagos (`/api/v1/checkout/mercado-pago/*`) exigen `financialFakeReady=true` en el entorno para evitar llamadas accidentales.
- **Operaciones Destructivas:** La eliminación de tienda (`DELETE /api/v1/stores/me`) exige `allowDestructiveOperations=true`.
- **Checkout:** Requiere que la tienda esté activa (`ACTIVE`), que el carrito contenga ítems válidos y que exista disponibilidad de capacidad e inventario.

## Variables

| Variable | Uso |
|---|---|
| `addressId` | Identificador de dirección de cliente. |
| `adminEmail` | Email sintético de administrador. |
| `adminPassword` | Contraseña de administrador local. |
| `adminToken` | JWT de administrador local. |
| `allowDestructiveOperations` | Bandera de seguridad (`true`) para operaciones destructivas como eliminar tienda. |
| `attemptId` | Identificador de intento de pago para investigación/reconciliación. |
| `baseUrl` | URL del servidor local, por defecto `http://127.0.0.1:8080`. |
| `capacityConfigId` | Identificador de configuración de capacidad horaria. |
| `capacityExceptionId` | Identificador de excepción de capacidad para fechas especiales. |
| `cartId` | Identificador del carrito del usuario. |
| `categoryId` | Identificador de categoría de producto. |
| `checkoutReferenceId` | Identificador de referencia de checkout. |
| `customerEmail` | Email sintético del cliente local. |
| `customerPassword` | Contraseña del cliente local. |
| `customerToken` | JWT del cliente autenticado. |
| `customerUserId` | Identificador UUID del usuario cliente. |
| `disposableFixturesReady` | Bandera opcional para suite de regresiones desechables. |
| `duplicateStreetAddress` | Dirección sintética para pruebas de duplicados (R001/R002). |
| `duplicateUbigeoCode` | UBIGEO sintético para pruebas de duplicados. |
| `endTime` | Fin de franja horaria (ej. `12:00:00`). |
| `existingMerchantEmail` | Email de merchant existente para pruebas de conflicto. |
| `existingStreetAddress` | Dirección actual para pruebas de actualización (R003). |
| `existingUbigeoCode` | UBIGEO actual para pruebas de actualización. |
| `financialFakeReady` | Bandera de seguridad (`true`) para ejecutar pagos contra gateway fake local. |
| `fixtureAddress` | Dirección sintética de ejemplo. |
| `fixtureAddressReference` | Referencia de dirección sintética. |
| `fixtureCustomerName` | Nombre de cliente sintético. |
| `fixtureCustomerPhone` | Teléfono de cliente sintético. |
| `fixtureDeliveryAddress` | Dirección de despacho sintética. |
| `fixtureDeliveryReference` | Referencia de despacho sintética. |
| `fixtureEmail` | Email de prueba sintético. |
| `fixtureFirstName` | Nombre de pila sintético. |
| `fixtureImageUrl` | URL de imagen de prueba sintética. |
| `fixtureLastName` | Apellido sintético. |
| `fixturePassword` | Contraseña sintética para pruebas. |
| `fixturePhone` | Teléfono sintético. |
| `fixtureRecipientName` | Nombre del destinatario sintético. |
| `fixtureRecipientPhone` | Teléfono del destinatario sintético. |
| `fixtureReference` | Referencia sintética. |
| `fixtureStreetAddress` | Calle y número sintéticos. |
| `invalidShortPassword` | Contraseña corta para pruebas negativas. |
| `merchantEmail` | Email sintético del comerciante local. |
| `merchantOnlyToken` | JWT de comerciante sin rol customer para pruebas de autorización. |
| `merchantPassword` | Contraseña del comerciante local. |
| `merchantToken` | JWT del comerciante autenticado. |
| `merchantUserId` | Identificador UUID del comerciante. |
| `missingAddressId` | UUID sintético de dirección inexistente. |
| `newRegistrationEmail` | Email para nuevos registros. |
| `notificationId` | Identificador de notificación. |
| `orderId` | Identificador interno de pedido (UUID). |
| `orderNumber` | Código público de pedido (ej. `ORD-202611-0001`). |
| `otherAddressId` | Dirección ajena para pruebas de aislamiento. |
| `otherStoreId` | Tienda ajena para pruebas de aislamiento. |
| `paymentId` | Identificador de pago local. |
| `preferenceId` | Identificador de preferencia local. |
| `productId` | Identificador de producto. |
| `providerOrderId` | Identificador de orden en proveedor fake. |
| `providerPaymentId` | Identificador de pago en proveedor fake. |
| `providerUserId` | Identificador de usuario en proveedor fake. |
| `reservationId` | Identificador de reserva de capacidad. |
| `revokedAdminToken` | Token revocado para pruebas de autorización. |
| `runCase` | Identificador de caso para ejecuciones individuales en la suite exhaustiva. |
| `serviceDate` | Fecha de servicio en formato YYYY-MM-DD. |
| `startTime` | Inicio de franja horaria (ej. `10:00:00`). |
| `storeId` | Identificador UUID de la tienda activa. |
| `storeSearchQuery` | Texto de búsqueda para tiendas (ej. `pasteleria`). |
| `storeSlug` | Slug identificador de tienda. |
| `userId` | Identificador de usuario para endpoints de administración. |
| `variantId` | Identificador de variante de producto. |
| `webhookRequestId` | ID de petición para webhooks simulados. |
| `webhookSignature` | Firma para webhooks simulados. |

## Regresiones Explícitas

- **R001:** POST dirección duplicada devuelve 409 / `ADDRESS_ALREADY_EXISTS`.
- **R002:** PUT hacia otra dirección existente devuelve 409 / `ADDRESS_ALREADY_EXISTS`.
- **R003:** PUT que conserva ubicación propia devuelve 200 OK.
- **R004:** Búsqueda sin tilde devuelve resultados coincidentes.
- **R005:** Búsqueda con porcentaje literal (`%25`) procesada de forma segura.
- **R006:** Búsqueda con guion bajo literal (`_`) procesada de forma segura.
- **R007:** Límite inválido 0 devuelve 400 / `VALIDATION_ERROR`.
- **R008:** Límite inválido 51 devuelve 400 / `VALIDATION_ERROR`.
- **R009:** Fechas de clientes en formato ISO y nulos permitidos.
- **R010:** Método no soportado PATCH en direcciones conserva cabecera Allow con 405.
- **R011:** Ruta inexistente devuelve 404 / `RESOURCE_NOT_FOUND`.
