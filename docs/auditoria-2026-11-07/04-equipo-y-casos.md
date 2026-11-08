# Equipo, casos y actualización de colecciones

## Reparto ajustado

El reparto inicial por módulos da Katherine 51 endpoints, Mia 40 y Josue 6. El conteo engaña: pagos tiene 43 clases de test, gateways, schedulers, refunds y carreras, aunque solo cuatro endpoints REST. Para liberar a Katherine, Josue toma administración de usuarios/tiendas (9 endpoints) y Mia catálogo público (4 endpoints). Queda Katherine 44, Mia 38 y Josue 15. El desglose efectivo de la tabla inferior es el autoritativo. Josue mantiene revisión transversal de seguridad/integridad; Mia ejecuta auth/perfil/customer; Katherine mantiene alta merchant y operación de tienda. Las responsabilidades describen pruebas futuras: nada fue ejecutado aquí.

Pesos sugeridos para agenda: CRUD simple 1, ownership/transición 2, reserva o financiera/concurrencia 4. Revisar carga después de primera ronda; no equilibrar quitándole pagos a quien prepara integridad. Katherine genera catálogo/stock/cupos; Mia consume esos fixtures; Josue verifica pago y cierre; Katherine avanza pedido y Mia verifica detalle/reseña. Refund y conciliación quedan fuera de ejecución hasta autorización separada de pruebas.

## Datos y orden

Crear posteriormente fixtures nuevos en entorno autorizado: admin A y B, merchant A/B con tiendas distintas, customer A/B, tienda activa/inactiva, categoría y producto por tienda, variante activa/inactiva, stock 1 y suficiente, cupo 1 y suficiente, modalidad pickup/delivery habilitada, fechas futuras calculadas en Lima. No copiar claves ni usuarios de environments al reporte. Referencias de pago deben ser de gateway fake; nunca inventar un ID para probar contra proveedor real.

1. Josue: baseline authz, revocación/diagnóstico y F01; acordar contrato de error. Mia: alta/login/perfil. Katherine: alta merchant, tienda y categorías.
2. Katherine: producto/variante, inventario, configuración/excepciones de capacidad. Mia: catálogo público, direcciones, favoritos, carrito.
3. Josue: checkout/pagos con dependencias preparadas; primero negativos y gateway fake. Mia verifica customer orders; Katherine operación merchant.
4. Mia: reseñas y notificaciones después de pedido elegible. Josue: concurrencia, reintentos y recuperación con Katherine/Mia como consumidores simultáneos de dos fixtures.

## Casos comunes aplicables a cada endpoint

Para cada lectura: recurso existente propio y filtros válidos → éxito indicado abajo; UUID malformado → 400; sin JWT → 401 salvo allowlist pública; rol insuficiente → 403 cuando hay restricción; recurso ajeno → resultado del servicio (403 o 404, nunca inferir uno universal); inexistente → 404 si busca recurso, lista vacía puede dar 200.

Para cada escritura: cuerpo válido → éxito indicado; campos requeridos ausentes/null/vacío y formatos/límites → 400 por Bean Validation; regla de negocio → 422 si BusinessRuleException; conflicto/duplicado → 409 si ConflictException o unique; JSON inválido → 400. Ejecutar fronteras de cada DTO en índice 02, conservar las precondiciones válidas para alcanzar la validación objetivo. Un fallo 401 previo no prueba la validación del cuerpo.

Por módulo añadir los negativos específicos de la matriz 00; los casos no afirmados por tests existentes permanecen pendientes. En permisos, distinguir autenticado sin rol y usuario con rol válido pero tenant/owner incorrecto. Pruebas que mezclan varios errores no permiten atribuir el resultado.

## Endpoints asignados con contrato de entrada y éxito

Las firmas siguientes incluyen anotaciones y parámetros reales; las restricciones de campos están en 02. El rol del filtro global también aplica aunque no aparezca PreAuthorize. Los checks imperativos del controlador se listan como evidencia adicional. Estados extraídos del return normal; webhook posee ramas de error descritas en 00.

### KATHERINE: 44 endpoints

#### `POST /api/v1/capacity-configurations` → 201

Método `create` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java:52`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityConfigurationResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCapacityConfigurationRequest request
    )
```

#### `GET /api/v1/capacity-configurations` → 200

Método `list` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java:66`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<CapacityConfigurationResponse>> list(
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `PUT /api/v1/capacity-configurations/{id}` → 200

Método `update` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java:79`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityConfigurationResponse> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCapacityConfigurationRequest request
    )
```

#### `PATCH /api/v1/capacity-configurations/{id}/activate` → 200

Método `activate` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java:94`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityConfigurationResponse> activate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id
    )
```

#### `PATCH /api/v1/capacity-configurations/{id}/deactivate` → 200

Método `deactivate` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java:105`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityConfigurationResponse> deactivate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id
    )
```

#### `POST /api/v1/capacity-exceptions` → 201

Método `create` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java:47`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityExceptionResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCapacityExceptionRequest request)
```

#### `GET /api/v1/capacity-exceptions` → 200

Método `list` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java:62`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<CapacityExceptionResponse>> list(
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `PUT /api/v1/capacity-exceptions/{id}` → 200

Método `update` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java:74`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityExceptionResponse> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCapacityExceptionRequest request)
```

#### `PATCH /api/v1/capacity-exceptions/{id}/activate` → 200

Método `activate` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java:89`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityExceptionResponse> activate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id)
```

#### `PATCH /api/v1/capacity-exceptions/{id}/deactivate` → 200

Método `deactivate` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java:99`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityExceptionResponse> deactivate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id)
```

#### `GET /api/v1/capacity/availability` → 200

Método `getAvailability` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java:46`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityAvailabilityResponse> getAvailability(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) UUID storeId)
```

Checks de rol locales: if (!principal.roles().contains("CUSTOMER")) {

#### `POST /api/v1/capacity/reservations` → 201

Método `create` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java:62`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityReservationResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCapacityReservationRequest request)
```

Checks de rol locales: if (!principal.roles().contains("CUSTOMER")) {

#### `GET /api/v1/capacity/reservations/{id}` → 200

Método `get` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java:79`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityReservationResponse> get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id)
```

Checks de rol locales: if (!principal.roles().contains("CUSTOMER")) {

#### `DELETE /api/v1/capacity/reservations/{id}` → 200

Método `release` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java:91`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacityReservationResponse> release(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id)
```

Checks de rol locales: if (!principal.roles().contains("CUSTOMER")) {

#### `GET /api/v1/capacity/slots` → 200

Método `getSlots` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacitySlotsController.java:40`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CapacitySlotsResponse> getSlots(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String date)
```

Checks de rol locales: if (principal == null || !principal.roles().contains("CUSTOMER")) {

#### `GET /api/v1/capacity/effective` → 200

Método `getEffectiveCapacity` en `backend/src/main/java/com/jaldishop/backend/capacity/web/controller/EffectiveCapacityController.java:34`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<EffectiveCapacityResponse> getEffectiveCapacity(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime)
```

#### `POST /api/v1/merchants/stores/{storeId}/categories` → 201

Método `createCategory` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryController.java:50`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateCategoryRequest request
    )
```

#### `GET /api/v1/merchants/stores/{storeId}/categories` → 200

Método `getCategories` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryController.java:66`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId
    )
```

#### `GET /api/v1/merchants/stores/{storeId}/categories/{categoryId}` → 200

Método `getCategory` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryController.java:79`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<CategoryResponse> getCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId
    )
```

#### `PUT /api/v1/merchants/stores/{storeId}/categories/{categoryId}` → 200

Método `updateCategory` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryController.java:90`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    )
```

#### `POST /api/v1/merchants/stores/{storeId}/products` → 201

Método `createProduct` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductController.java:58`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateProductRequest request
    )
```

#### `GET /api/v1/merchants/stores/{storeId}/products` → 200

Método `getProducts` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductController.java:77`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<List<ProductResponse>> getProducts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID categoryId
    )
```

#### `GET /api/v1/merchants/stores/{storeId}/products/{productId}` → 200

Método `getProduct` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductController.java:108`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<ProductResponse> getProduct(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId
    )
```

#### `PUT /api/v1/merchants/stores/{storeId}/products/{productId}` → 200

Método `updateProduct` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductController.java:125`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<ProductResponse> updateProduct(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    )
```

#### `POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants` → 201

Método `createVariant` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java:52`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<ProductVariantResponse> createVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request
    )
```

#### `GET /api/v1/merchants/stores/{storeId}/products/{productId}/variants` → 200

Método `getVariants` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java:81`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<List<ProductVariantResponse>> getVariants(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId
    )
```

#### `GET /api/v1/merchants/stores/{storeId}/products/{productId}/variants/{variantId}` → 200

Método `getVariant` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java:95`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<ProductVariantResponse> getVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @PathVariable UUID variantId
    )
```

#### `PUT /api/v1/merchants/stores/{storeId}/products/{productId}/variants/{variantId}` → 200

Método `updateVariant` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java:107`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<ProductVariantResponse> updateVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateProductVariantRequest request
    )
```

#### `POST /api/v1/auth/register/merchant` → 201

Método `registerMerchant` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java:76`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<AuthResult> registerMerchant(@Valid @RequestBody RegisterMerchantRequest request)
```

#### `GET /api/v1/inventory` → 200

Método `getStoreInventory` en `backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java:60`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<List<InventoryResponse>> getStoreInventory(
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `GET /api/v1/inventory/variants/{variantId}` → 200

Método `getVariantInventory` en `backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java:73`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<InventoryResponse> getVariantInventory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId
    )
```

#### `GET /api/v1/inventory/low-stock` → 200

Método `getLowStockInventory` en `backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java:87`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<List<InventoryResponse>> getLowStockInventory(
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `PUT /api/v1/inventory/variants/{variantId}/stock` → 200

Método `updateStock` en `backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java:100`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<InventoryResponse> updateStock(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateStockRequest request
    )
```

#### `PATCH /api/v1/inventory/variants/{variantId}/threshold` → 200

Método `updateThreshold` en `backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java:115`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<InventoryResponse> updateThreshold(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateThresholdRequest request
    )
```

#### `POST /api/v1/media/upload-signature` → 200

Método `generateUploadSignature` en `backend/src/main/java/com/jaldishop/backend/media/web/controller/MediaController.java:31`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<UploadSignatureResponse> generateUploadSignature(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody GenerateSignatureRequest request
    )
```

Checks de rol locales: if (!isMerchant && !isAdmin) {

#### `GET /api/v1/merchant/customers` → 200

Método `listCustomers` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/MerchantCustomerController.java:42`. Roles declarativos: "hasRole('MERCHANT'.

```java
public ResponseEntity<List<StoreCustomerResponse>> listCustomers(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false)
            @Size(max = 100, message = "El texto de búsqueda no puede exceder 100 caracteres") String query
    )
```

#### `GET /api/v1/store-categories` → 200

Método `getAllActiveCategories` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreCategoryController.java:22`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<StoreCategoryResponse>> getAllActiveCategories()
```

#### `POST /api/v1/stores` → 201

Método `createStore` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java:51`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<StoreResponse> createStore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateStoreRequest request
    )
```

#### `GET /api/v1/stores/me` → 200

Método `getMyStore` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java:86`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<StoreResponse> getMyStore(
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `PUT /api/v1/stores/me` → 200

Método `updateStore` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java:94`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<StoreResponse> updateStore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateStoreRequest request
    )
```

#### `DELETE /api/v1/stores/me` → 200

Método `closeMyStore` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java:125`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CloseStoreResponse> closeMyStore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody(required = false) CloseStoreRequest request
    )
```

#### `GET /api/v1/stores/slug/{slug}` → 200

Método `getStoreBySlug` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java:135`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<PublicStoreResponse> getStoreBySlug(
            @PathVariable @Size(max = 180, message = "El slug no puede exceder 180 caracteres") String slug
    )
```

#### `GET /api/v1/stores/search` → 200

Método `searchPublicStores` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java:143`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<PublicStoreResponse>> searchPublicStores(
            @RequestParam(required = false, defaultValue = "")
            @Size(max = 100, message = "El texto de búsqueda no puede exceder 100 caracteres") String q,
            @RequestParam(required = false, defaultValue = "5")
            @Min(value = 1, message = "El límite debe ser al menos 1")
            @Max(value = 50, message = "El límite no puede exceder 50") int limit
    )
```

#### `GET /api/v1/stores/featured` → 200

Método `getFeaturedStores` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java:158`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<PublicStoreResponse>> getFeaturedStores(
            @RequestParam(required = false, defaultValue = "6")
            @Min(value = 1, message = "El límite debe ser al menos 1")
            @Max(value = 50, message = "El límite no puede exceder 50") int limit
    )
```

### MIA: 38 endpoints

#### `GET /api/v1/customer/addresses` → 200

Método `getAddresses` en `backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java:38`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<CustomerAddressListResponse> getAddresses(
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `POST /api/v1/customer/addresses` → 201

Método `createAddress` en `backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java:45`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<CustomerAddressResponse> createAddress(
            @Valid @RequestBody CreateCustomerAddressRequest request,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `PUT /api/v1/customer/addresses/{id}` → 200

Método `updateAddress` en `backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java:53`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<CustomerAddressResponse> updateAddress(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCustomerAddressRequest request,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `PATCH /api/v1/customer/addresses/{id}/default` → 200

Método `setDefaultAddress` en `backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java:62`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<CustomerAddressResponse> setDefaultAddress(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `DELETE /api/v1/customer/addresses/{id}` → 204

Método `deleteAddress` en `backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java:70`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<Void> deleteAddress(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `GET /api/v1/cart` → 200

Método `getCart` en `backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java:54`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam UUID storeId
    )
```

#### `POST /api/v1/cart/items` → 201

Método `addItem` en `backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java:63`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody AddItemToCartRequest request
    )
```

#### `PUT /api/v1/cart/items/{variantId}` → 200

Método `updateItem` en `backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java:78`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateCartItemRequest request
    )
```

#### `DELETE /api/v1/cart/items/{variantId}` → 200

Método `removeItem` en `backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java:94`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @RequestParam UUID storeId
    )
```

#### `DELETE /api/v1/cart` → 200

Método `clearCart` en `backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java:109`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CartResponse> clearCart(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam UUID storeId
    )
```

#### `GET /api/v1/stores/{storeId}/categories` → 200

Método `getCategories` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicCategoryController.java:32`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<CategoryResponse>> getCategories(@PathVariable UUID storeId)
```

#### `GET /api/v1/stores/slug/{slug}/categories` → 200

Método `getCategoriesBySlug` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicCategoryController.java:37`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<CategoryResponse>> getCategoriesBySlug(
            @PathVariable @Size(max = 180, message = "El slug no puede exceder 180 caracteres") String slug
    )
```

#### `GET /api/v1/stores/{storeId}/products` → 200

Método `getStoreProducts` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicProductController.java:45`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<ProductResponse>> getStoreProducts(
            @PathVariable UUID storeId,
            @RequestParam(required = false) UUID categoryId
    )
```

#### `GET /api/v1/stores/slug/{slug}/products` → 200

Método `getStoreProductsBySlug` en `backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicProductController.java:53`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<ProductResponse>> getStoreProductsBySlug(
            @PathVariable @Size(max = 180, message = "El slug no puede exceder 180 caracteres") String slug,
            @RequestParam(required = false) UUID categoryId
    )
```

#### `GET /api/v1/customer/favorites/products` → 200

Método `getFavoriteProducts` en `backend/src/main/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteController.java:39`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<List<FavoriteProductResponse>> getFavoriteProducts(
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `POST /api/v1/customer/favorites/products/{productId}` → 201

Método `addFavoriteProduct` en `backend/src/main/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteController.java:46`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<FavoriteStatusResponse> addFavoriteProduct(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `DELETE /api/v1/customer/favorites/products/{productId}` → 200

Método `removeFavoriteProduct` en `backend/src/main/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteController.java:54`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<FavoriteStatusResponse> removeFavoriteProduct(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `GET /api/v1/customer/favorites/products/{productId}/status` → 200

Método `checkFavoriteProductStatus` en `backend/src/main/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteController.java:62`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<FavoriteStatusResponse> checkFavoriteProductStatus(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `POST /api/v1/auth/register` → 201

Método `register` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java:44`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request)
```

#### `POST /api/v1/auth/login` → 200

Método `login` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java:68`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<AuthResult> login(@Valid @RequestBody LoginRequest request)
```

#### `POST /api/v1/auth/refresh` → 200

Método `refresh` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java:102`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<AuthResult> refresh(
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `GET /api/v1/users/me` → 200

Método `getMyProfile` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/UserController.java:27`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `PUT /api/v1/users/me` → 200

Método `updateProfile` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/UserController.java:35`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateUserRequest request
    )
```

#### `GET /api/v1/notifications` → 200

Método `listNotifications` en `backend/src/main/java/com/jaldishop/backend/notification/web/controller/NotificationController.java:39`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<NotificationResponse>> listNotifications(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de página no puede exceder 100") int size)
```

#### `GET /api/v1/notifications/unread/count` → 200

Método `countUnread` en `backend/src/main/java/com/jaldishop/backend/notification/web/controller/NotificationController.java:55`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<Map<String, Long>> countUnread(
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `PATCH /api/v1/notifications/{id}/read` → 200

Método `markAsRead` en `backend/src/main/java/com/jaldishop/backend/notification/web/controller/NotificationController.java:62`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id)
```

#### `GET /api/v1/customer/orders` → 200

Método `getMyOrders` en `backend/src/main/java/com/jaldishop/backend/ordering/web/controller/CustomerOrderController.java:45`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<List<CustomerOrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `GET /api/v1/customer/orders/{orderNumber}` → 200

Método `getOrderDetail` en `backend/src/main/java/com/jaldishop/backend/ordering/web/controller/CustomerOrderController.java:54`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CustomerOrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String orderNumber
    )
```

#### `PATCH /api/v1/customer/orders/{orderNumber}/cancel` → 200

Método `cancelOrder` en `backend/src/main/java/com/jaldishop/backend/ordering/web/controller/CustomerOrderController.java:64`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CustomerOrderDetailResponse> cancelOrder(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String orderNumber,
            @Valid @RequestBody(required = false) CancelOrderRequest request
    )
```

#### `GET /api/v1/merchants/stores/{storeId}/orders` → 200

Método `listOrders` en `backend/src/main/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderController.java:61`. Roles declarativos: "hasRole('MERCHANT'; "hasRole('MERCHANT'.

```java
public ResponseEntity<MerchantOrderPageResponse> listOrders(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "El número de página no puede ser negativo") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
                                               @Max(value = 100, message = "El tamaño de página no puede ser mayor a 100") int size
    )
```

#### `GET /api/v1/merchants/stores/{storeId}/orders/{orderId}` → 200

Método `getOrderDetail` en `backend/src/main/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderController.java:86`. Roles declarativos: "hasRole('MERCHANT'; "hasRole('MERCHANT'.

```java
public ResponseEntity<MerchantOrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID orderId
    )
```

#### `PATCH /api/v1/merchants/stores/{storeId}/orders/{orderId}/status` → 200

Método `changeOrderStatus` en `backend/src/main/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderController.java:102`. Roles declarativos: "hasRole('MERCHANT'; "hasRole('MERCHANT'.

```java
public ResponseEntity<MerchantOrderDetailResponse> changeOrderStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID orderId,
            @Valid @RequestBody ChangeOrderStatusRequest request
    )
```

#### `GET /api/v1/customer/products/{productId}/reviews/eligibility` → 200

Método `checkEligibility` en `backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java:46`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<ReviewEligibilityResponse> checkEligibility(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `POST /api/v1/customer/products/{productId}/reviews` → 201

Método `createReview` en `backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java:54`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<CustomerReviewResponse> createReview(
            @PathVariable("productId") UUID productId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `PUT /api/v1/customer/products/{productId}/reviews` → 200

Método `updateReview` en `backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java:63`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<CustomerReviewResponse> updateReview(
            @PathVariable("productId") UUID productId,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `DELETE /api/v1/customer/products/{productId}/reviews` → 204

Método `deleteReview` en `backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java:72`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<Void> deleteReview(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `GET /api/v1/customer/reviews` → 200

Método `getCustomerReviews` en `backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java:80`. Roles declarativos: "hasRole('CUSTOMER'.

```java
public ResponseEntity<List<CustomerReviewResponse>> getCustomerReviews(
            @AuthenticationPrincipal JwtPrincipal principal)
```

#### `GET /api/v1/products/{productId}/reviews` → 200

Método `getProductReviews` en `backend/src/main/java/com/jaldishop/backend/review/web/controller/PublicProductReviewController.java:23`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<ProductReviewsSummaryResponse> getProductReviews(@PathVariable("productId") UUID productId)
```

### JOSUE: 15 endpoints

#### `POST /api/v1/checkout` → 201

Método `initiateCheckout` en `backend/src/main/java/com/jaldishop/backend/checkout/web/controller/CheckoutController.java:31`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CheckoutResponse> initiateCheckout(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody InitiateCheckoutRequest request
    )
```

#### `GET /api/v1/admin/users` → 200

Método `listUsers` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java:50`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<List<AdminUserSummaryResponse>> listUsers(
            @RequestParam(required = false)
            @Size(max = 100, message = "El texto de búsqueda no puede exceder 100 caracteres") String query,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) UserStatus status
    )
```

#### `GET /api/v1/admin/users/{id}` → 200

Método `getUserById` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java:64`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<AdminUserDetailResponse> getUserById(@PathVariable UUID id)
```

#### `PATCH /api/v1/admin/users/{id}/suspend` → 200

Método `suspendUser` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java:70`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<AdminUserDetailResponse> suspendUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `PATCH /api/v1/admin/users/{id}/activate` → 200

Método `activateUser` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java:80`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<AdminUserDetailResponse> activateUser(@PathVariable UUID id)
```

#### `PATCH /api/v1/admin/users/{id}/roles` → 200

Método `updateUserRoles` en `backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java:86`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<AdminUserDetailResponse> updateUserRoles(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateUserRolesRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    )
```

#### `POST /api/v1/admin/payments/attempts/{attemptId}/reconcile-order` → 200

Método `reconcile` en `backend/src/main/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationController.java:24`. Roles declarativos: "hasRole('ADMIN'.

```java
public OrderReconciliationCandidateResult reconcile(@PathVariable UUID attemptId)
```

#### `POST /api/v1/admin/payments/attempts/{attemptId}/investigate-order` → 200

Método `investigate` en `backend/src/main/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationController.java:29`. Roles declarativos: "hasRole('ADMIN'.

```java
public ManualOrderInvestigationService.Result investigate(@PathVariable UUID attemptId)
```

#### `POST /api/v1/checkout/mercado-pago/preference` → 201

Método `createPreference` en `backend/src/main/java/com/jaldishop/backend/payment/web/controller/MercadoPagoPreferenceController.java:28`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<CreatePaymentPreferenceResponse> createPreference(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreatePaymentPreferenceRequest request
    )
```

#### `POST /api/v1/checkout/mercado-pago/webhook` → 200

Método `receiveWebhook` en `backend/src/main/java/com/jaldishop/backend/payment/web/controller/MercadoPagoWebhookController.java:32`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public ResponseEntity<?> receiveWebhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam Map<String, String> queryParams,
            @RequestBody(required = false) String rawBody
    )
```

#### `GET /api/v1/health` → 200

Método `health` en `backend/src/main/java/com/jaldishop/backend/shared/web/HealthController.java:12`. Roles declarativos: aplica SecurityConfig y checks de servicio.

```java
public String health()
```

#### `GET /api/v1/admin/stores` → 200

Método `listStores` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/AdminStoreController.java:41`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<List<AdminStoreSummaryResponse>> listStores(
            @RequestParam(required = false)
            @Size(max = 100, message = "El texto de búsqueda no puede exceder 100 caracteres") String query,
            @RequestParam(required = false) StoreStatus status
    )
```

#### `GET /api/v1/admin/stores/{id}` → 200

Método `getStoreById` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/AdminStoreController.java:54`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<AdminStoreDetailResponse> getStoreById(@PathVariable UUID id)
```

#### `PATCH /api/v1/admin/stores/{id}/suspend` → 200

Método `suspendStore` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/AdminStoreController.java:60`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<AdminStoreDetailResponse> suspendStore(@PathVariable UUID id)
```

#### `PATCH /api/v1/admin/stores/{id}/activate` → 200

Método `activateStore` en `backend/src/main/java/com/jaldishop/backend/store/web/controller/AdminStoreController.java:66`. Roles declarativos: "hasRole('ADMIN'.

```java
public ResponseEntity<AdminStoreDetailResponse> activateStore(@PathVariable UUID id)
```

## Plan Postman y Bruno

1. Mantener 97 firmas canónicas y etiquetar tres duplicados como variantes de escenario, no eliminarlos solo por duplicidad. Revisar tabla de duplicados 01. No añadir rutas que el backend no implementa.
2. Comparar para cada firma cuerpo/DTO, query obligatorios, paths UUID, token/rol y códigos del documento 00. La presencia de ruta ya está cubierta; falta profundidad. En particular refresh protegido, DELETE reserva devuelve 200 con cuerpo, y create preference devuelve 201 incluso al devolver sesión existente.
3. Separar escenarios success, validación, authz, tenant y conflictos, con aserciones de status y code. Para éxito comprobar identidad/owner/estado y postcondiciones; para errores no aceptar un simple “no es 500”. Añadir casos F01-F05 y límites SQL. No usar sleeps como prueba de expiración; controlar reloj en tests automáticos.
4. Aserciones Postman: revisar 24 requests con pm.test y scripts heredados; scripts que solo guardan IDs no validan comportamiento. Evitar scripts de prerequest que ejecuten operaciones ocultas. Documentar las dependencias en vez de reusar IDs de otro usuario.
5. Recuperar Bruno externo solo como referencia; comparar sus firmas con las 97 canónicas antes de elegir carpeta base. Crear posteriormente colección dentro de repositorio, con entornos de ejemplo sin secretos y README de guardas. No importada ni cambiada en esta auditoría.
6. Mantener pagos/webhooks/refunds/reconcile/investigate en carpeta excluida de runs generales; son operaciones con efectos. Las pruebas de gateway fake no autorizan una llamada real. Ningún run automático sobre jaldishop_test o servicios desplegados.
7. Registrar resultados por escenario (pendiente/aprobado/fallido/bloqueado) y evidencia redactada; ruta presente y test escrito no cuentan como aprobado. Actualización de colecciones también espera autorización.
