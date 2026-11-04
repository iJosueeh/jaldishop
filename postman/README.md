# Colección Canónica de API — JaldiShop

Colección HTTP canónica para el backend de JaldiShop (Spring Boot 3 + PostgreSQL), formateada bajo el estándar **Postman Collection v2.1.0 JSON** y 100% compatible con **Postman** y **Bruno**.

---

## 1. Archivos Canónicos Unificados

- [`jaldishop.postman_collection.json`](file:///home/ijosueeh/Documentos/Proyectos/jaldishop/postman/jaldishop.postman_collection.json): Colección unificada con los **86 endpoints REST únicos** implementados en el backend (89 solicitudes en total al incluir 4 variantes de inicio de sesión por rol), con esquemas de parámetros, cuerpos de solicitud compatibles con los DTOs de Spring Boot y ejemplos de respuesta (éxito y errores normalizados).
- [`jaldishop.postman_environment.json`](file:///home/ijosueeh/Documentos/Proyectos/jaldishop/postman/jaldishop.postman_environment.json): Entorno y variables unificadas para desarrollo local (`http://localhost:8080`), conteniendo todas las variables necesarias para todos los flujos y roles (Customer, Merchant, Admin, Notifications).

> [!IMPORTANT]
> El entorno se versiona en Git con `authToken: ""` y `authPassword: ""` (ambos de tipo `secret`), garantizando que **nunca se almacenen tokens, contraseñas ni credenciales reales en el repositorio**. Al ejecutar cualquier login en Postman, el token se autocompleta mediante scripts automatizados.

---

## 2. Instrucciones de Importación

### En Postman
1. Abrir **Postman**.
2. En la barra superior izquierda, hacer clic en **Import** (o `Ctrl + O`).
3. Seleccionar los 2 archivos:
   - `postman/jaldishop.postman_collection.json`
   - `postman/jaldishop.postman_environment.json`
4. Seleccionar en el selector superior derecho el entorno **JaldiShop Local Dev**.

### En Bruno (v1.x / v4.x)
1. Abrir **Bruno**.
2. En la pantalla de inicio o menú de opciones, hacer clic en **Import Collection** > **Postman Collection**.
3. Seleccionar `postman/jaldishop.postman_collection.json` y definir la carpeta local de destino para los archivos `.bru`.
4. Para el entorno: en el panel izquierdo de la colección en Bruno, acceder al menú `...` > **Settings** > **Environments** > **Import Postman Environment** y seleccionar `postman/jaldishop.postman_environment.json`.
5. Seleccionar el entorno activo en la esquina superior derecha de Bruno.

---

## 3. Automatización de Tokens y Manejo de Sesión por Rol

### Herencia Automática de Token
La raíz de la colección define autenticación de tipo `Bearer Token` referenciando la variable `{{authToken}}`. Los 73 endpoints protegidos heredan esta configuración de forma transparente.

### Captura Automática de JWT (Login y Refresh)
Las solicitudes de inicio de sesión y renovación incorporan un script post-respuesta que intercepta respuestas exitosas (`HTTP 200` o `201`):

```javascript
if (pm.response.code === 200 || pm.response.code === 201) {
    try {
        var jsonData = pm.response.json();
        if (jsonData && typeof jsonData.token === 'string' && jsonData.token.trim().length > 0) {
            pm.environment.set("authToken", jsonData.token);
            if (typeof jsonData.userId === 'string' && jsonData.userId.trim().length > 0) {
                pm.environment.set("userId", jsonData.userId);
            }
        }
    } catch (e) {
        // Fallback seguro
    }
}
```

* **Actualización en el entorno activo:** Tras autenticarse exitosamente, `authToken` y `userId` se actualizan automáticamente en el entorno seleccionado.
* **Preservación en caso de fallo:** Si el login falla (`401 Credenciales Inválidas`, `422 Usuario Inactivo`), la condición no se cumple y el token previo **no se sobrescribe**.

### Variantes de Login por Rol
Todos los usuarios se autentican a través del endpoint canónico `POST /api/v1/auth/login`. Para facilitar las pruebas, la carpeta `02. Identity > Auth` provee 4 variantes:

1. **`Iniciar Sesión — Customer`**: Utiliza `cliente.demo@jaldishop.com` y `{{authPassword}}`.
2. **`Iniciar Sesión — Merchant`**: Utiliza `merchant.demo@jaldishop.com` y `{{authPassword}}`.
3. **`Iniciar Sesión — Admin`**: Utiliza `admin.demo@jaldishop.com` y `{{authPassword}}`.
4. **`Iniciar Sesión — Personalizada`**: Utiliza `{{userEmail}}` y `{{authPassword}}` para cuentas arbitrarias.

### Flujo de Trabajo para Probar Diferentes Roles
Para cambiar de rol sin que los tokens ni los IDs de contexto se sobreescriban entre compañeros o pruebas:

```
Seleccionar Entorno (ej. Merchant) ➔ Asignar contraseña local en {{authPassword}} ➔ Ejecutar "Iniciar Sesión — Merchant" ➔ {{authToken}} queda listo para /api/v1/merchants/**
```

1. Seleccionar el entorno del rol a probar (ej. `JaldiShop — Merchant Dev`).
2. En la variable `authPassword` del entorno (valor local, no committeado), colocar la contraseña correspondiente (en seeds locales demo: `Password123!`).
3. Ejecutar la solicitud correspondiente (ej. `Iniciar Sesión — Merchant`).
4. El script captura el nuevo JWT y lo almacena en `authToken`. A partir de ese momento, todas las llamadas de dicho rol funcionarán sin necesidad de copiar tokens a mano.

### Mecanismo de Renovación (Refresh)
El endpoint `POST /api/v1/auth/refresh`:
* Requiere que el usuario **ya cuente con un token Bearer vigente** en la cabecera (`Authorization: Bearer {{authToken}}`).
* Genera un nuevo JWT con expiración extendida y actualiza automáticamente `authToken`.
* **No funciona con tokens ya expirados:** Si el token ya venció, Spring Security rechazará la petición con `401 Unauthorized` antes de llegar al controlador. En tal caso, debe ejecutarse nuevamente la solicitud de **Login**.

---

## 4. Contratos de Error Normalizados

Todos los endpoints siguen el contrato unificado `ApiError` (`code`, `message`, `errors`):

| Escenario | HTTP Status | Código `code` | Descripción |
| :--- | :--- | :--- | :--- |
| **Petición sin token en endpoint protegido** | **401 Unauthorized** | `UNAUTHORIZED` | Cabecera `Authorization` ausente o token JWT expirado/inválido. |
| **Login con credenciales incorrectas** | **401 Unauthorized** | `INVALID_CREDENTIALS` | Correo o contraseña no válidos en `/api/v1/auth/login`. |
| **Permisos insuficientes o Tienda ajena** | **403 Forbidden** | `FORBIDDEN` | Rol insuficiente o intento de operar sobre una tienda ajena. |
| **Recurso inexistente** | **404 Not Found** | `RESOURCE_NOT_FOUND` | Identificador UUID o entidad no encontrada en base de datos. |
| **Conflicto de estado del recurso** | **409 Conflict** | *Específico del dominio* | Ejemplo: `USER_ALREADY_SUSPENDED`, `STORE_ALREADY_ACTIVE`, `VARIANT_INACTIVE`. |
| **Regla de negocio violada** | **422 Unprocessable** | `NOTIFICATION_ALREADY_READ` / `CAPACITY_EXHAUSTED` | Operación semánticamente inválida según las reglas del dominio. |
| **Error de validación de entrada** | **400 Bad Request** | `VALIDATION_ERROR` | Violaciones de Bean Validation en campos o parámetros (detallados en `errors`). |
| **JSON malformado** | **400 Bad Request** | `MALFORMED_JSON` | Sintaxis JSON no parseable en el cuerpo de la solicitud. |

---

## 5. Inventario de Endpoints por Módulo (86 Endpoints REST Únicos)

### 01. Health (1 endpoint)
- `GET /api/v1/health`

### 02. Identity (11 endpoints únicos, 14 solicitudes en colección)
- **Auth (4 endpoints únicos — 7 solicitudes):**
  - `POST /api/v1/auth/register` (Registrar Cliente)
  - `POST /api/v1/auth/login` (*Variante Customer*)
  - `POST /api/v1/auth/login` (*Variante Merchant*)
  - `POST /api/v1/auth/login` (*Variante Admin*)
  - `POST /api/v1/auth/login` (*Variante Personalizada*)
  - `POST /api/v1/auth/register/merchant` (Registrar Comerciante con Tienda)
  - `POST /api/v1/auth/refresh` (Refrescar Token)
- **User Profile (2 endpoints únicos):**
  - `GET /api/v1/users/me`
  - `PUT /api/v1/users/me`
- **Admin Users (5 endpoints únicos):**
  - `GET /api/v1/admin/users`
  - `GET /api/v1/admin/users/{userId}`
  - `PATCH /api/v1/admin/users/{userId}/suspend`
  - `PATCH /api/v1/admin/users/{userId}/activate`
  - `PATCH /api/v1/admin/users/{userId}/roles`

### 03. Stores (13 endpoints)
- **Store Categories (Público):** `GET /api/v1/store-categories`
- **Store Management (Merchant):**
  - `POST /api/v1/stores`
  - `GET /api/v1/stores/me`
  - `PUT /api/v1/stores/me`
  - `DELETE /api/v1/stores/me`
  - `GET /api/v1/merchant/customers`
- **Store Discovery (Público):**
  - `GET /api/v1/stores/slug/{storeSlug}`
  - `GET /api/v1/stores/search`
  - `GET /api/v1/stores/featured`
- **Admin Stores:**
  - `GET /api/v1/admin/stores`
  - `GET /api/v1/admin/stores/{storeId}`
  - `PATCH /api/v1/admin/stores/{storeId}/suspend`
  - `PATCH /api/v1/admin/stores/{storeId}/activate`

### 04. Catalog (16 endpoints)
- **Categories (Merchant):** `POST`, `GET`, `GET /{categoryId}`, `PUT /{categoryId}` en `/api/v1/merchants/stores/{storeId}/categories`
- **Categories (Público):** `GET /api/v1/stores/{storeId}/categories`, `GET /api/v1/stores/slug/{storeSlug}/categories`
- **Products (Merchant):** `POST`, `GET`, `GET /{productId}`, `PUT /{productId}` en `/api/v1/merchants/stores/{storeId}/products`
- **Products (Público):** `GET /api/v1/stores/{storeId}/products`, `GET /api/v1/stores/slug/{storeSlug}/products`
- **Product Variants (Merchant):** `POST`, `GET`, `GET /{variantId}`, `PUT /{variantId}` en `/api/v1/merchants/stores/{storeId}/products/{productId}/variants`

### 05. Capacity (16 endpoints)
- **Configurations:** `POST`, `GET`, `PUT /{id}`, `PATCH /{id}/activate`, `PATCH /{id}/deactivate` en `/api/v1/capacity-configurations`
- **Exceptions:** `POST`, `GET`, `PUT /{id}`, `PATCH /{id}/activate`, `PATCH /{id}/deactivate` en `/api/v1/capacity-exceptions`
- **Availability & Slots:** `GET /api/v1/capacity/availability`, `GET /api/v1/capacity/slots`, `GET /api/v1/capacity/effective`
- **Reservations:** `POST`, `GET /{id}`, `DELETE /{id}` en `/api/v1/capacity/reservations`

### 06. Cart (5 endpoints)
- `GET /api/v1/cart`, `POST /api/v1/cart/items`, `PUT /api/v1/cart/items/{variantId}`, `DELETE /api/v1/cart/items/{variantId}`, `DELETE /api/v1/cart`

### 07. Checkout (1 endpoint)
- `POST /api/v1/checkout`

### 08. Orders (5 endpoints)
- **Customer Orders:** `GET /api/v1/customer/orders`, `GET /api/v1/customer/orders/{orderNumber}`
- **Merchant Orders:** `GET /api/v1/merchants/stores/{storeId}/orders`, `GET /{orderId}`, `PATCH /{orderId}/status`

### 09. Notifications (3 endpoints)
- `GET /api/v1/notifications`, `GET /api/v1/notifications/unread/count`, `PATCH /api/v1/notifications/{notificationId}/read`

### 10. Customer Addresses (5 endpoints)
- `GET`, `POST`, `PUT /{addressId}`, `PATCH /{addressId}/default`, `DELETE /{addressId}` en `/api/v1/customer/addresses`

### 11. Customer Favorites (4 endpoints)
- `GET`, `POST /{productId}`, `DELETE /{productId}`, `GET /{productId}/status` en `/api/v1/customer/favorites/products`

### 12. Reviews (5 endpoints)
- `GET /api/v1/customer/products/{productId}/reviews/eligibility`, `POST`, `PUT`, `GET /api/v1/customer/reviews`, `GET /api/v1/products/{productId}/reviews` (Público)

### 13. Media (1 endpoint)
- `POST /api/v1/media/upload-signature`

---

## 6. Endpoints Omitidos Justificadamente

- **Módulo Inventory:** No posee controladores REST expuestos. Las reservas y compromisos de stock se ejecutan internamente como servicios de dominio orquestados por el módulo `Checkout`.
- **Canales WebSocket (`/ws/**`):** Son sockets bidireccionales basados en STOMP sobre WebSocket, fuera del alcance del protocolo HTTP REST.

---

## 7. Compatibilidad y Limitaciones Conocidas entre Postman y Bruno

1. **Traducción de Scripts en Bruno:**
   Bruno incluye en su traductor oficial (`@usebruno/converters`) soporte para convertir automáticamente `pm.environment.set` en `bru.setEnvVar`, `pm.response.code` en `res.getStatus()` y `pm.response.json()` en `res.getBody()`. Los scripts de esta colección utilizan exclusivamente esta API estándar para asegurar que la importación en Bruno sea limpia y funcional.
2. **Path Variables (`:param`):**
   Postman almacena variables de ruta con sintaxis `:variable` mientras que Bruno las traduce a su formato nativo `.bru`. Las variables de ruta mantienen nombres semánticos coincidentes con las variables de entorno (`:storeId`, `:productId`, `:orderId`, `:userId`, etc.).
3. **Variables Secretas en Bruno:**
   Bruno soporta variables de entorno secretas mediante la propiedad `"secret": true`. Al importar entornos de Postman, las variables de tipo `"secret"` (`authToken`, `authPassword`) son reconocidas para evitar que sus valores se muestren en texto plano en la interfaz.
