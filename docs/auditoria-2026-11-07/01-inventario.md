# Inventario REST y cobertura de colección

Extracción estática de @RestController y mappings; identidad = método + ruta normalizada (nombres de variables no distinguen endpoints). Query no crea endpoint adicional. Estados de éxito extraídos del controlador; confirmar ramas en fuente. No incluye Actuator ni STOMP, que no son controladores REST propios.

32 controladores; 97 mappings; 97 endpoints únicos; 100 requests Postman; 97 firmas únicas Postman; 0 endpoints sin request; 0 requests sin mapping equivalente. Cobertura de presencia, no funcional.

| Módulo | Endpoints | Con Postman | Sin Postman | Responsable |
|---|---:|---:|---:|---|
| address | 5 | 5 | 0 | Mia |
| capacity | 16 | 16 | 0 | Katherine |
| cart | 5 | 5 | 0 | Mia |
| catalog | 16 | 16 | 0 | Katherine |
| checkout | 1 | 1 | 0 | Josue |
| favorite | 4 | 4 | 0 | Mia |
| identity | 11 | 11 | 0 | Mia |
| inventory | 5 | 5 | 0 | Katherine |
| media | 1 | 1 | 0 | Katherine |
| notification | 3 | 3 | 0 | Mia |
| ordering | 6 | 6 | 0 | Mia |
| payment | 4 | 4 | 0 | Josue |
| review | 6 | 6 | 0 | Mia |
| shared | 1 | 1 | 0 | Josue |
| store | 13 | 13 | 0 | Katherine |

## Todos los endpoints

| Módulo | Método y ruta | Método Java / evidencia | Éxito | Requests Postman |
|---|---|---|---:|---:|
| address | `GET /api/v1/customer/addresses` | [getAddresses](../../backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java#L38) | 200 | 1 |
| address | `POST /api/v1/customer/addresses` | [createAddress](../../backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java#L45) | 201 | 1 |
| address | `PUT /api/v1/customer/addresses/{id}` | [updateAddress](../../backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java#L53) | 200 | 1 |
| address | `PATCH /api/v1/customer/addresses/{id}/default` | [setDefaultAddress](../../backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java#L62) | 200 | 1 |
| address | `DELETE /api/v1/customer/addresses/{id}` | [deleteAddress](../../backend/src/main/java/com/jaldishop/backend/address/web/controller/CustomerAddressController.java#L70) | 204 | 1 |
| capacity | `POST /api/v1/capacity-configurations` | [create](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java#L52) | 201 | 1 |
| capacity | `GET /api/v1/capacity-configurations` | [list](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java#L66) | 200 | 1 |
| capacity | `PUT /api/v1/capacity-configurations/{id}` | [update](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java#L79) | 200 | 1 |
| capacity | `PATCH /api/v1/capacity-configurations/{id}/activate` | [activate](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java#L94) | 200 | 1 |
| capacity | `PATCH /api/v1/capacity-configurations/{id}/deactivate` | [deactivate](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityConfigurationController.java#L105) | 200 | 1 |
| capacity | `POST /api/v1/capacity-exceptions` | [create](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java#L47) | 201 | 1 |
| capacity | `GET /api/v1/capacity-exceptions` | [list](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java#L62) | 200 | 1 |
| capacity | `PUT /api/v1/capacity-exceptions/{id}` | [update](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java#L74) | 200 | 1 |
| capacity | `PATCH /api/v1/capacity-exceptions/{id}/activate` | [activate](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java#L89) | 200 | 1 |
| capacity | `PATCH /api/v1/capacity-exceptions/{id}/deactivate` | [deactivate](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityExceptionController.java#L99) | 200 | 1 |
| capacity | `GET /api/v1/capacity/availability` | [getAvailability](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java#L46) | 200 | 1 |
| capacity | `POST /api/v1/capacity/reservations` | [create](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java#L62) | 201 | 1 |
| capacity | `GET /api/v1/capacity/reservations/{id}` | [get](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java#L79) | 200 | 1 |
| capacity | `DELETE /api/v1/capacity/reservations/{id}` | [release](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java#L91) | 200 | 1 |
| capacity | `GET /api/v1/capacity/slots` | [getSlots](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacitySlotsController.java#L40) | 200 | 1 |
| capacity | `GET /api/v1/capacity/effective` | [getEffectiveCapacity](../../backend/src/main/java/com/jaldishop/backend/capacity/web/controller/EffectiveCapacityController.java#L34) | 200 | 1 |
| cart | `GET /api/v1/cart` | [getCart](../../backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java#L54) | 200 | 1 |
| cart | `POST /api/v1/cart/items` | [addItem](../../backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java#L63) | 201 | 1 |
| cart | `PUT /api/v1/cart/items/{variantId}` | [updateItem](../../backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java#L78) | 200 | 1 |
| cart | `DELETE /api/v1/cart/items/{variantId}` | [removeItem](../../backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java#L94) | 200 | 1 |
| cart | `DELETE /api/v1/cart` | [clearCart](../../backend/src/main/java/com/jaldishop/backend/cart/web/controller/CartController.java#L109) | 200 | 1 |
| catalog | `POST /api/v1/merchants/stores/{storeId}/categories` | [createCategory](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryController.java#L50) | 201 | 1 |
| catalog | `GET /api/v1/merchants/stores/{storeId}/categories` | [getCategories](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryController.java#L66) | 200 | 1 |
| catalog | `GET /api/v1/merchants/stores/{storeId}/categories/{categoryId}` | [getCategory](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryController.java#L79) | 200 | 1 |
| catalog | `PUT /api/v1/merchants/stores/{storeId}/categories/{categoryId}` | [updateCategory](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantCategoryController.java#L90) | 200 | 1 |
| catalog | `POST /api/v1/merchants/stores/{storeId}/products` | [createProduct](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductController.java#L58) | 201 | 1 |
| catalog | `GET /api/v1/merchants/stores/{storeId}/products` | [getProducts](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductController.java#L77) | 200 | 1 |
| catalog | `GET /api/v1/merchants/stores/{storeId}/products/{productId}` | [getProduct](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductController.java#L108) | 200 | 1 |
| catalog | `PUT /api/v1/merchants/stores/{storeId}/products/{productId}` | [updateProduct](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductController.java#L125) | 200 | 1 |
| catalog | `POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants` | [createVariant](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java#L52) | 201 | 1 |
| catalog | `GET /api/v1/merchants/stores/{storeId}/products/{productId}/variants` | [getVariants](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java#L81) | 200 | 1 |
| catalog | `GET /api/v1/merchants/stores/{storeId}/products/{productId}/variants/{variantId}` | [getVariant](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java#L95) | 200 | 1 |
| catalog | `PUT /api/v1/merchants/stores/{storeId}/products/{productId}/variants/{variantId}` | [updateVariant](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/MerchantProductVariantController.java#L107) | 200 | 1 |
| catalog | `GET /api/v1/stores/{storeId}/categories` | [getCategories](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicCategoryController.java#L32) | 200 | 1 |
| catalog | `GET /api/v1/stores/slug/{slug}/categories` | [getCategoriesBySlug](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicCategoryController.java#L37) | 200 | 1 |
| catalog | `GET /api/v1/stores/{storeId}/products` | [getStoreProducts](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicProductController.java#L45) | 200 | 1 |
| catalog | `GET /api/v1/stores/slug/{slug}/products` | [getStoreProductsBySlug](../../backend/src/main/java/com/jaldishop/backend/catalog/web/controller/PublicProductController.java#L53) | 200 | 1 |
| checkout | `POST /api/v1/checkout` | [initiateCheckout](../../backend/src/main/java/com/jaldishop/backend/checkout/web/controller/CheckoutController.java#L31) | 201 | 1 |
| favorite | `GET /api/v1/customer/favorites/products` | [getFavoriteProducts](../../backend/src/main/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteController.java#L39) | 200 | 1 |
| favorite | `POST /api/v1/customer/favorites/products/{productId}` | [addFavoriteProduct](../../backend/src/main/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteController.java#L46) | 201 | 1 |
| favorite | `DELETE /api/v1/customer/favorites/products/{productId}` | [removeFavoriteProduct](../../backend/src/main/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteController.java#L54) | 200 | 1 |
| favorite | `GET /api/v1/customer/favorites/products/{productId}/status` | [checkFavoriteProductStatus](../../backend/src/main/java/com/jaldishop/backend/favorite/web/controller/CustomerFavoriteController.java#L62) | 200 | 1 |
| identity | `GET /api/v1/admin/users` | [listUsers](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java#L50) | 200 | 1 |
| identity | `GET /api/v1/admin/users/{id}` | [getUserById](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java#L64) | 200 | 1 |
| identity | `PATCH /api/v1/admin/users/{id}/suspend` | [suspendUser](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java#L70) | 200 | 1 |
| identity | `PATCH /api/v1/admin/users/{id}/activate` | [activateUser](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java#L80) | 200 | 1 |
| identity | `PATCH /api/v1/admin/users/{id}/roles` | [updateUserRoles](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AdminUserController.java#L86) | 200 | 1 |
| identity | `POST /api/v1/auth/register` | [register](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java#L44) | 201 | 1 |
| identity | `POST /api/v1/auth/login` | [login](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java#L68) | 200 | 3 |
| identity | `POST /api/v1/auth/register/merchant` | [registerMerchant](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java#L76) | 201 | 1 |
| identity | `POST /api/v1/auth/refresh` | [refresh](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java#L102) | 200 | 1 |
| identity | `GET /api/v1/users/me` | [getMyProfile](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/UserController.java#L27) | 200 | 1 |
| identity | `PUT /api/v1/users/me` | [updateProfile](../../backend/src/main/java/com/jaldishop/backend/identity/web/controller/UserController.java#L35) | 200 | 1 |
| inventory | `GET /api/v1/inventory` | [getStoreInventory](../../backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java#L60) | 200 | 1 |
| inventory | `GET /api/v1/inventory/variants/{variantId}` | [getVariantInventory](../../backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java#L73) | 200 | 1 |
| inventory | `GET /api/v1/inventory/low-stock` | [getLowStockInventory](../../backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java#L87) | 200 | 1 |
| inventory | `PUT /api/v1/inventory/variants/{variantId}/stock` | [updateStock](../../backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java#L100) | 200 | 1 |
| inventory | `PATCH /api/v1/inventory/variants/{variantId}/threshold` | [updateThreshold](../../backend/src/main/java/com/jaldishop/backend/inventory/web/controller/MerchantInventoryController.java#L115) | 200 | 1 |
| media | `POST /api/v1/media/upload-signature` | [generateUploadSignature](../../backend/src/main/java/com/jaldishop/backend/media/web/controller/MediaController.java#L31) | 200 | 1 |
| notification | `GET /api/v1/notifications` | [listNotifications](../../backend/src/main/java/com/jaldishop/backend/notification/web/controller/NotificationController.java#L39) | 200 | 1 |
| notification | `GET /api/v1/notifications/unread/count` | [countUnread](../../backend/src/main/java/com/jaldishop/backend/notification/web/controller/NotificationController.java#L55) | 200 | 1 |
| notification | `PATCH /api/v1/notifications/{id}/read` | [markAsRead](../../backend/src/main/java/com/jaldishop/backend/notification/web/controller/NotificationController.java#L62) | 200 | 1 |
| ordering | `GET /api/v1/customer/orders` | [getMyOrders](../../backend/src/main/java/com/jaldishop/backend/ordering/web/controller/CustomerOrderController.java#L45) | 200 | 1 |
| ordering | `GET /api/v1/customer/orders/{orderNumber}` | [getOrderDetail](../../backend/src/main/java/com/jaldishop/backend/ordering/web/controller/CustomerOrderController.java#L54) | 200 | 1 |
| ordering | `PATCH /api/v1/customer/orders/{orderNumber}/cancel` | [cancelOrder](../../backend/src/main/java/com/jaldishop/backend/ordering/web/controller/CustomerOrderController.java#L64) | 200 | 1 |
| ordering | `GET /api/v1/merchants/stores/{storeId}/orders` | [listOrders](../../backend/src/main/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderController.java#L61) | 200 | 1 |
| ordering | `GET /api/v1/merchants/stores/{storeId}/orders/{orderId}` | [getOrderDetail](../../backend/src/main/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderController.java#L86) | 200 | 1 |
| ordering | `PATCH /api/v1/merchants/stores/{storeId}/orders/{orderId}/status` | [changeOrderStatus](../../backend/src/main/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderController.java#L102) | 200 | 1 |
| payment | `POST /api/v1/admin/payments/attempts/{attemptId}/reconcile-order` | [reconcile](../../backend/src/main/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationController.java#L24) | 200 | 1 |
| payment | `POST /api/v1/admin/payments/attempts/{attemptId}/investigate-order` | [investigate](../../backend/src/main/java/com/jaldishop/backend/payment/web/controller/AdminOrderInvestigationController.java#L29) | 200 | 1 |
| payment | `POST /api/v1/checkout/mercado-pago/preference` | [createPreference](../../backend/src/main/java/com/jaldishop/backend/payment/web/controller/MercadoPagoPreferenceController.java#L28) | 201 | 1 |
| payment | `POST /api/v1/checkout/mercado-pago/webhook` | [receiveWebhook](../../backend/src/main/java/com/jaldishop/backend/payment/web/controller/MercadoPagoWebhookController.java#L32) | 200 | 2 |
| review | `GET /api/v1/customer/products/{productId}/reviews/eligibility` | [checkEligibility](../../backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java#L46) | 200 | 1 |
| review | `POST /api/v1/customer/products/{productId}/reviews` | [createReview](../../backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java#L54) | 201 | 1 |
| review | `PUT /api/v1/customer/products/{productId}/reviews` | [updateReview](../../backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java#L63) | 200 | 1 |
| review | `DELETE /api/v1/customer/products/{productId}/reviews` | [deleteReview](../../backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java#L72) | 204 | 1 |
| review | `GET /api/v1/customer/reviews` | [getCustomerReviews](../../backend/src/main/java/com/jaldishop/backend/review/web/controller/CustomerReviewController.java#L80) | 200 | 1 |
| review | `GET /api/v1/products/{productId}/reviews` | [getProductReviews](../../backend/src/main/java/com/jaldishop/backend/review/web/controller/PublicProductReviewController.java#L23) | 200 | 1 |
| shared | `GET /api/v1/health` | [health](../../backend/src/main/java/com/jaldishop/backend/shared/web/HealthController.java#L12) | 200 | 1 |
| store | `GET /api/v1/admin/stores` | [listStores](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/AdminStoreController.java#L41) | 200 | 1 |
| store | `GET /api/v1/admin/stores/{id}` | [getStoreById](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/AdminStoreController.java#L54) | 200 | 1 |
| store | `PATCH /api/v1/admin/stores/{id}/suspend` | [suspendStore](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/AdminStoreController.java#L60) | 200 | 1 |
| store | `PATCH /api/v1/admin/stores/{id}/activate` | [activateStore](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/AdminStoreController.java#L66) | 200 | 1 |
| store | `GET /api/v1/merchant/customers` | [listCustomers](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/MerchantCustomerController.java#L42) | 200 | 1 |
| store | `GET /api/v1/store-categories` | [getAllActiveCategories](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreCategoryController.java#L22) | 200 | 1 |
| store | `POST /api/v1/stores` | [createStore](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java#L51) | 201 | 1 |
| store | `GET /api/v1/stores/me` | [getMyStore](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java#L86) | 200 | 1 |
| store | `PUT /api/v1/stores/me` | [updateStore](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java#L94) | 200 | 1 |
| store | `DELETE /api/v1/stores/me` | [closeMyStore](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java#L125) | 200 | 1 |
| store | `GET /api/v1/stores/slug/{slug}` | [getStoreBySlug](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java#L135) | 200 | 1 |
| store | `GET /api/v1/stores/search` | [searchPublicStores](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java#L143) | 200 | 1 |
| store | `GET /api/v1/stores/featured` | [getFeaturedStores](../../backend/src/main/java/com/jaldishop/backend/store/web/controller/StoreController.java#L158) | 200 | 1 |

## Requests sin endpoint equivalente (obsoleto o normalización pendiente)


## Duplicados de firma Postman

- `POST /api/v1/auth/login` (3): /00. Configuración y autenticación/02. Iniciar Sesión — Customer; /00. Configuración y autenticación/03. Iniciar Sesión — Merchant; /00. Configuración y autenticación/04. Iniciar Sesión — Admin
- `POST /api/v1/checkout/mercado-pago/webhook` (2): /04. Integraciones y Webhooks/01. Webhook Mercado Pago — Payment (IPN); /04. Integraciones y Webhooks/02. Webhook Mercado Pago — Order (referencia Orders API)

## Contraste estático Bruno externo

Solo método y URL; no se ejecutó ni importó.

### bruno_imported_test

86 firmas únicas extraídas, 86/97 coinciden con backend; 11 firmas REST no presentes; 0 firmas sin mapping equivalente; 0 archivos sin método/URL extraíbles. Diferencias por interpolación de rutas requieren revisión manual.

Firmas faltantes:

- `DELETE /api/v1/customer/products/{}/reviews`
- `GET /api/v1/inventory`
- `GET /api/v1/inventory/low-stock`
- `GET /api/v1/inventory/variants/{}`
- `PATCH /api/v1/customer/orders/{}/cancel`
- `PATCH /api/v1/inventory/variants/{}/threshold`
- `POST /api/v1/admin/payments/attempts/{}/investigate-order`
- `POST /api/v1/admin/payments/attempts/{}/reconcile-order`
- `POST /api/v1/checkout/mercado-pago/preference`
- `POST /api/v1/checkout/mercado-pago/webhook`
- `PUT /api/v1/inventory/variants/{}/stock`

Sin mapping equivalente:


### bruno_imported_phase_d

86 firmas únicas extraídas, 86/97 coinciden con backend; 11 firmas REST no presentes; 0 firmas sin mapping equivalente; 0 archivos sin método/URL extraíbles. Diferencias por interpolación de rutas requieren revisión manual.

Firmas faltantes:

- `DELETE /api/v1/customer/products/{}/reviews`
- `GET /api/v1/inventory`
- `GET /api/v1/inventory/low-stock`
- `GET /api/v1/inventory/variants/{}`
- `PATCH /api/v1/customer/orders/{}/cancel`
- `PATCH /api/v1/inventory/variants/{}/threshold`
- `POST /api/v1/admin/payments/attempts/{}/investigate-order`
- `POST /api/v1/admin/payments/attempts/{}/reconcile-order`
- `POST /api/v1/checkout/mercado-pago/preference`
- `POST /api/v1/checkout/mercado-pago/webhook`
- `PUT /api/v1/inventory/variants/{}/stock`

Sin mapping equivalente:

