# Colección Canónica de API — JaldiShop

Colección HTTP canónica para el backend de JaldiShop (Spring Boot 3 + PostgreSQL), formateada bajo el estándar **Postman Collection v2.1.0 JSON** y 100% compatible con **Postman** y **Bruno**.

---

## 1. Archivos Canónicos y Entornos

- [`jaldishop.postman_collection.json`](file:///home/ijosueeh/Documentos/Proyectos/jaldishop/postman/jaldishop.postman_collection.json): Colección unificada con **92 endpoints por método y ruta** (95 solicitudes en total, incluidas 3 variantes de login), con esquemas de parámetros, contratos DTO de Spring Boot y ejemplos de respuesta.
- [`jaldishop.local.env.json`](file:///home/ijosueeh/Documentos/Proyectos/jaldishop/postman/jaldishop.local.env.json): Entorno para desarrollo local (`http://localhost:8080`), con variables semilla para desarrollo (`jaldishop_test`) y variables de fecha/hora (`serviceDate`, `startTime`, `endTime`).
- [`jaldishop.render.env.json`](file:///home/ijosueeh/Documentos/Proyectos/jaldishop/postman/jaldishop.render.env.json): Entorno para pruebas en Render (`https://jaldishop-api.onrender.com`), con variables en blanco para evitar mezclar datos.
- [`jaldishop.postman_environment.json`](file:///home/ijosueeh/Documentos/Proyectos/jaldishop/postman/jaldishop.postman_environment.json): Alias por defecto sincronizado con el entorno local.

> [!IMPORTANT]
> Los entornos se versionan en Git sin contraseñas ni tokens reales (`authToken: ""`, `authPassword: ""`). Nunca incluyas credenciales productivas ni access tokens de Mercado Pago en los archivos de entorno.

---

## 2. Instrucciones de Importación

### En Bruno (v1.x / v4.x)
1. Abrir **Bruno**.
2. En la pantalla inicial o menú, hacer clic en **Import Collection** > **Postman Collection**.
3. Seleccionar `postman/jaldishop.postman_collection.json` y elegir una carpeta destino en tu máquina para los archivos `.bru`.
4. Importar los entornos:
   - Ir a la colección importada en Bruno > clic en el menú `...` o en el engranaje de configuración.
   - En **Environments**, hacer clic en **Import Environment** y cargar `postman/jaldishop.local.env.json`.
   - Repetir el proceso para importar `postman/jaldishop.render.env.json`.
5. Seleccionar en el selector superior derecho el entorno activo (`JaldiShop Local Dev` o `JaldiShop Render (Producción)`).

### En Postman
1. Abrir **Postman** > clic en **Import** (`Ctrl + O`).
2. Cargar `postman/jaldishop.postman_collection.json` y los archivos de entorno deseados.
3. Seleccionar el entorno en la esquina superior derecha.

---

## 3. Automatización de Variables Dinámicas

La colección incluye scripts en los tests de respuesta (`pm.response.code === 200 || 201`) para encadenar las llamadas sin necesidad de copiar y pegar UUIDs manualmente:

| Endpoint | Variables Capturadas en el Entorno |
| :--- | :--- |
| `POST /api/v1/auth/login` (cualquier rol) | `authToken`, `userId` |
| `POST /api/v1/auth/refresh` | `authToken`, `userId` |
| `POST /api/v1/stores` | `storeId`, `storeSlug` |
| `POST /api/v1/merchants/stores/{storeId}/products` | `productId` |
| `POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants` | `variantId` |
| `POST /api/v1/capacity/reservations` | `reservationId` |
| `POST /api/v1/checkout` | `reservationId` (reserva temporal; no crea pedido) |
| `POST /api/v1/checkout/mercado-pago/preference` | `paymentId`, `preferenceId`, `checkoutReferenceId` |
| `GET /api/v1/customer/orders` | `orderNumber` (del primer pedido en la lista) |
| `GET /api/v1/customer/orders/{orderNumber}` | `orderNumber`, `orderId` |
| `PATCH /api/v1/customer/orders/{orderNumber}/cancel` | `orderNumber` |

Variables estáticas incluidas en los entornos:

| Variable | Uso |
| :--- | :--- |
| `serviceDate` | Fecha de servicio (`YYYY-MM-DD`) para capacidad y checkout |
| `startTime`, `endTime` | Franja horaria (`HH:mm:ss`) para capacidad y checkout |
| `storeSearchQuery` | Texto de búsqueda de tiendas (`GET /api/v1/stores/search`) |
| `checkoutReferenceId`, `providerPaymentId`, `providerOrderId` | Referencias para preferencia, webhook de pago y webhook de orden |
| `webhookRequestId` | `x-request-id` de ejemplo para webhooks; no es una firma válida |
| `webhookSignature` | Firma HMAC real de Mercado Pago; debe generarse fuera de la colección |

> [!NOTE]
> En Bruno, estos scripts se convierten nativamente a llamadas `bru.setEnvVar(key, val)`.

---

## 4. Secuencia para Probar el Ciclo Completo (E2E)

```
00. Iniciar Sesión — Customer ➔ 01. Customer > 05. Carrito > 02. Agregar Ítem al Carrito ➔ 01. Customer > 07. Checkout y Pagos > 01. Iniciar Checkout ➔ 02. Generar Preferencia Mercado Pago ➔ [Aprobación de Pago / Webhook] ➔ 01. Customer > 08. Pedidos > 03. Cancelar Pedido (Temprano)
```

1. **Autenticación Cliente:**
   - Ejecutar `00. Configuración y autenticación > 02. Iniciar Sesión — Customer`.
   - Se captura `authToken` de cliente.
2. **Preparar Carrito:**
   - Ejecutar `01. Customer > 05. Carrito > 02. Agregar Ítem al Carrito` (utiliza `{{variantId}}`).
3. **Iniciar Checkout:**
   - Ejecutar `01. Customer > 07. Checkout y Pagos > 01. Iniciar Checkout`.
   - Reserva inventario y cupo de capacidad.
   - Captura `reservationId` en el entorno. No crea el pedido.
4. **Generar Preferencia de Pago:**
   - Ejecutar `01. Customer > 07. Checkout y Pagos > 02. Generar Preferencia Mercado Pago`.
   - Envía `capacityReservationId` (`{{reservationId}}`).
   - Con `MERCADOPAGO_CHECKOUT_MODE=PREFERENCES`, devuelve `preferenceId` e `initPoint`.
   - Con `MERCADOPAGO_CHECKOUT_MODE=ORDERS`, `preferenceId` contiene el ID de la orden del proveedor.
   - Captura `preferenceId`, `checkoutReferenceId` y `paymentId`.
5. **Confirmación del Pago (Transición a `CONFIRMED`):**
   - El pedido se crea al aprobarse el pago vía webhook. *Ver sección 5 para las limitaciones de simulación.*
6. **Cancelar Pedido:**
   - Ejecutar `01. Customer > 08. Pedidos > 03. Cancelar Pedido (Temprano)` (`PATCH /api/v1/customer/orders/{{orderNumber}}/cancel`).
   - Restituye stock e incrementa inventario atómicamente.
   - Libera capacidad (`COMMITTED -> RELEASED`).
   - Deja reembolso pendiente con `refund_status = PENDING`.

---

## 5. Simulación Segura de Mercado Pago y Limitaciones

### ¿Qué se puede probar directamente desde Bruno / Postman?
- **Creación de preferencia (`POST /api/v1/checkout/mercado-pago/preference`):**
  - Si el backend tiene configurado un token válido de sandbox de Mercado Pago, la preferencia se genera exitosamente y entrega una URL `initPoint` en modo `PREFERENCES`.
  - En modo `ORDERS`, la respuesta entrega el identificador de la orden del proveedor en `preferenceId`.
  - Si el token es simulado o inválido, el backend devolverá un error de integración con la pasarela.
- **Estados de pago:** `PaymentStatus` usa `PENDING`, `PROCESSING`, `APPROVED`, `FAILED`; `PaymentAttemptStatus` usa `CREATED`, `PROCESSING`, `APPROVED`, `REJECTED`, `ERROR`.

### ¿Por qué no se debe simular el Webhook directamente desde Bruno contra producción?
1. **Validación de Firma Criptográfica:** El endpoint `/api/v1/checkout/mercado-pago/webhook` exige la cabecera `x-signature` generada mediante HMAC-SHA256 con el `webhook-secret` del backend. Enviar firmas ficticias resultará en `401 INVALID_WEBHOOK_SIGNATURE`.
2. **Verificación en la Pasarela:** El webhook acepta eventos `payment` y `order`. Para pagos, el backend consulta la API de Mercado Pago (`GET /v1/payments/{id}`) para verificar el estado real y el monto antes de confirmar la comanda. Si el ID no existe realmente en Mercado Pago, la orden no se confirmará.
3. **Seguridad Financiera:** No utilices tarjetas de crédito reales ni intentes forzar aprobaciones en entornos de producción.
4. **Validación Exhaustiva:** Para validar el ciclo de pago y webhook con mocks completos, utiliza la suite de integración de Spring Boot (`PaymentFlowEndToEndIntegrationTest` y `OrderCancellationPostgresIntegrationTest`), las cuales levantan servidores MockServer locales aislados con bases de datos PostgreSQL de prueba.

---

## 6. Advertencias para el Entorno Render (`JaldiShop Render`)

> [!CAUTION]
> Al cambiar al entorno **JaldiShop Render (Producción)**:
> 1. Todas las peticiones impactan directamente la base de datos **Neon** compartida en la nube.
> 2. No ejecutes `DELETE /api/v1/stores/me`, `POST /api/v1/cart/items` masivos ni creaciones de órdenes innecesarias sin autorización.
> 3. Los schedulers de reconciliación y reembolsos están **desactivados por defecto** en Render, por lo que las órdenes no confirmadas por webhook no se reconciliarán automáticamente en segundo plano.
