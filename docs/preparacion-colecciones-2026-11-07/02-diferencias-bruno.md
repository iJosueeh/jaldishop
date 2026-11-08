# Comparación de copias y decisión canónica

Postman previo: 100 requests, 97 firmas únicas. Se conserva cada request original (incluidos tres logins y dos variantes webhook) como caso VALIDO y se añaden negativos. Se limpiaron variables versionadas y cuerpos de autenticación; el respaldo privado conserva el estado previo. No se leyó ningún environment.

Fuente de rutas/contrato: inventario y backend auditado. Fuente canónica de requests mantenida: postman/jaldishop.postman_collection.json. Bruno externo queda pendiente: no hay evidencia que autorice elegir una copia como canónica. No se creó ni sobrescribió una colección Bruno.

La comparación usa firmas normalizadas de método/ruta; lista diferencias de cuerpos, headers, autenticación y variables, sin publicar valores. Diferencia no significa defecto: puede ser fixture, importación o contrato antiguo. Se compara contra Postman anterior, antes de generar escenarios. Para exportar Bruno después de elegir ubicación se deben trasladar también los guards, aserciones y extracción de variables; importar solo HTTP perdería controles.

## bruno_imported_test

Ubicación: `/home/ijosueeh/.gemini/antigravity-cli/brain/078c7ee2-cbd6-4c2b-8c34-b7876eba0906/scratch/bruno_imported_test`.

Requests HTTP: 86; firmas únicas: 86; bruno.json: True.

Faltantes frente a Postman: 11; adicionales: 0; duplicados: 0.

| Firma | Estado / diferencias |
|---|---|
| `DELETE /api/v1/capacity/reservations/{}` | Auth distinta/heredada; Headers distintos |
| `DELETE /api/v1/cart` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId / Bruno baseUrl |
| `DELETE /api/v1/cart/items/{}` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId,variantId / Bruno baseUrl,variantId |
| `DELETE /api/v1/customer/addresses/{}` | Auth distinta/heredada; Headers distintos |
| `DELETE /api/v1/customer/favorites/products/{}` | Auth distinta/heredada; Headers distintos |
| `DELETE /api/v1/customer/products/{}/reviews` | Falta en Bruno |
| `DELETE /api/v1/stores/me` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `GET /api/v1/admin/stores` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/admin/stores/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/admin/users` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/admin/users/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/capacity-configurations` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/capacity-exceptions` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/capacity/availability` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime,storeId / Bruno baseUrl |
| `GET /api/v1/capacity/effective` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime / Bruno baseUrl |
| `GET /api/v1/capacity/reservations/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/capacity/slots` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,serviceDate,storeId / Bruno baseUrl |
| `GET /api/v1/cart` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId / Bruno baseUrl |
| `GET /api/v1/customer/addresses` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/favorites/products` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/favorites/products/{}/status` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/orders` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/orders/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/products/{}/reviews/eligibility` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/reviews` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/health` | Headers distintos |
| `GET /api/v1/inventory` | Falta en Bruno |
| `GET /api/v1/inventory/low-stock` | Falta en Bruno |
| `GET /api/v1/inventory/variants/{}` | Falta en Bruno |
| `GET /api/v1/merchant/customers` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/categories` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/categories/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/orders` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/orders/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/products` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/products/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/products/{}/variants` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/products/{}/variants/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/notifications` | Auth distinta/heredada; Headers distintos; Variables distintas: PM authToken,baseUrl / Bruno baseUrl |
| `GET /api/v1/notifications/unread/count` | Auth distinta/heredada; Headers distintos; Variables distintas: PM authToken,baseUrl / Bruno baseUrl |
| `GET /api/v1/products/{}/reviews` | Headers distintos |
| `GET /api/v1/store-categories` | Headers distintos |
| `GET /api/v1/stores/featured` | Headers distintos |
| `GET /api/v1/stores/me` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/stores/search` | Headers distintos; Variables distintas: PM baseUrl,storeSearchQuery / Bruno baseUrl |
| `GET /api/v1/stores/slug/{}` | Headers distintos |
| `GET /api/v1/stores/slug/{}/categories` | Headers distintos |
| `GET /api/v1/stores/slug/{}/products` | Headers distintos |
| `GET /api/v1/stores/{}/categories` | Headers distintos |
| `GET /api/v1/stores/{}/products` | Headers distintos |
| `GET /api/v1/users/me` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/stores/{}/activate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/stores/{}/suspend` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/users/{}/activate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/users/{}/roles` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/users/{}/suspend` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/capacity-configurations/{}/activate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/capacity-configurations/{}/deactivate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/capacity-exceptions/{}/activate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/capacity-exceptions/{}/deactivate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/customer/addresses/{}/default` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/customer/orders/{}/cancel` | Falta en Bruno |
| `PATCH /api/v1/inventory/variants/{}/threshold` | Falta en Bruno |
| `PATCH /api/v1/merchants/stores/{}/orders/{}/status` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/notifications/{}/read` | Auth distinta/heredada; Headers distintos; Variables distintas: PM authToken,baseUrl,notificationId / Bruno baseUrl,notificationId |
| `POST /api/v1/admin/payments/attempts/{}/investigate-order` | Falta en Bruno |
| `POST /api/v1/admin/payments/attempts/{}/reconcile-order` | Falta en Bruno |
| `POST /api/v1/auth/login` | Cuerpo distinto; Headers distintos; Variables distintas: PM authPassword,authToken,baseUrl,userId / Bruno baseUrl |
| `POST /api/v1/auth/refresh` | Auth distinta/heredada; Headers distintos; Variables distintas: PM authToken,baseUrl / Bruno baseUrl |
| `POST /api/v1/auth/register` | Cuerpo distinto; Headers distintos |
| `POST /api/v1/auth/register/merchant` | Cuerpo distinto; Headers distintos |
| `POST /api/v1/capacity-configurations` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `POST /api/v1/capacity-exceptions` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime / Bruno baseUrl |
| `POST /api/v1/capacity/reservations` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime,storeId / Bruno baseUrl |
| `POST /api/v1/cart/items` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId,variantId / Bruno baseUrl |
| `POST /api/v1/checkout` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime,storeId / Bruno baseUrl |
| `POST /api/v1/checkout/mercado-pago/preference` | Falta en Bruno |
| `POST /api/v1/checkout/mercado-pago/webhook` | Falta en Bruno |
| `POST /api/v1/customer/addresses` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `POST /api/v1/customer/favorites/products/{}` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/customer/products/{}/reviews` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `POST /api/v1/media/upload-signature` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `POST /api/v1/merchants/stores/{}/categories` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `POST /api/v1/merchants/stores/{}/products` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `POST /api/v1/merchants/stores/{}/products/{}/variants` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `POST /api/v1/stores` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/capacity-configurations/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/capacity-exceptions/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,capacityExceptionId,endTime,serviceDate,startTime / Bruno baseUrl,capacityExceptionId |
| `PUT /api/v1/cart/items/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId,variantId / Bruno baseUrl,variantId |
| `PUT /api/v1/customer/addresses/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/customer/products/{}/reviews` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/inventory/variants/{}/stock` | Falta en Bruno |
| `PUT /api/v1/merchants/stores/{}/categories/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/merchants/stores/{}/products/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/merchants/stores/{}/products/{}/variants/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/stores/me` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/users/me` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
## bruno_imported_phase_d

Ubicación: `/home/ijosueeh/.gemini/antigravity-cli/brain/078c7ee2-cbd6-4c2b-8c34-b7876eba0906/scratch/bruno_imported_phase_d`.

Requests HTTP: 89; firmas únicas: 86; bruno.json: False.

Faltantes frente a Postman: 11; adicionales: 0; duplicados: 3.

| Firma | Estado / diferencias |
|---|---|
| `DELETE /api/v1/capacity/reservations/{}` | Auth distinta/heredada; Headers distintos |
| `DELETE /api/v1/cart` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId / Bruno baseUrl |
| `DELETE /api/v1/cart/items/{}` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId,variantId / Bruno baseUrl,variantId |
| `DELETE /api/v1/customer/addresses/{}` | Auth distinta/heredada; Headers distintos |
| `DELETE /api/v1/customer/favorites/products/{}` | Auth distinta/heredada; Headers distintos |
| `DELETE /api/v1/customer/products/{}/reviews` | Falta en Bruno |
| `DELETE /api/v1/stores/me` | Cuerpo distinto; Auth distinta/heredada; Headers distintos |
| `GET /api/v1/admin/stores` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/admin/stores/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/admin/users` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/admin/users/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/capacity-configurations` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/capacity-exceptions` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/capacity/availability` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime,storeId / Bruno baseUrl |
| `GET /api/v1/capacity/effective` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime / Bruno baseUrl |
| `GET /api/v1/capacity/reservations/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/capacity/slots` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,serviceDate,storeId / Bruno baseUrl |
| `GET /api/v1/cart` | Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId / Bruno baseUrl |
| `GET /api/v1/customer/addresses` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/favorites/products` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/favorites/products/{}/status` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/orders` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/orders/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/products/{}/reviews/eligibility` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/customer/reviews` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/health` | Headers distintos |
| `GET /api/v1/inventory` | Falta en Bruno |
| `GET /api/v1/inventory/low-stock` | Falta en Bruno |
| `GET /api/v1/inventory/variants/{}` | Falta en Bruno |
| `GET /api/v1/merchant/customers` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/categories` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/categories/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/orders` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/orders/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/products` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/products/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/products/{}/variants` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/merchants/stores/{}/products/{}/variants/{}` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/notifications` | Auth distinta/heredada; Headers distintos; Variables distintas: PM authToken,baseUrl / Bruno baseUrl |
| `GET /api/v1/notifications/unread/count` | Auth distinta/heredada; Headers distintos; Variables distintas: PM authToken,baseUrl / Bruno baseUrl |
| `GET /api/v1/products/{}/reviews` | Headers distintos |
| `GET /api/v1/store-categories` | Headers distintos |
| `GET /api/v1/stores/featured` | Headers distintos |
| `GET /api/v1/stores/me` | Auth distinta/heredada; Headers distintos |
| `GET /api/v1/stores/search` | Headers distintos; Variables distintas: PM baseUrl,storeSearchQuery / Bruno baseUrl |
| `GET /api/v1/stores/slug/{}` | Headers distintos |
| `GET /api/v1/stores/slug/{}/categories` | Headers distintos |
| `GET /api/v1/stores/slug/{}/products` | Headers distintos |
| `GET /api/v1/stores/{}/categories` | Headers distintos |
| `GET /api/v1/stores/{}/products` | Headers distintos |
| `GET /api/v1/users/me` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/stores/{}/activate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/stores/{}/suspend` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/users/{}/activate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/users/{}/roles` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/admin/users/{}/suspend` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/capacity-configurations/{}/activate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/capacity-configurations/{}/deactivate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/capacity-exceptions/{}/activate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/capacity-exceptions/{}/deactivate` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/customer/addresses/{}/default` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/customer/orders/{}/cancel` | Falta en Bruno |
| `PATCH /api/v1/inventory/variants/{}/threshold` | Falta en Bruno |
| `PATCH /api/v1/merchants/stores/{}/orders/{}/status` | Auth distinta/heredada; Headers distintos |
| `PATCH /api/v1/notifications/{}/read` | Auth distinta/heredada; Headers distintos; Variables distintas: PM authToken,baseUrl,notificationId / Bruno baseUrl,notificationId |
| `POST /api/v1/admin/payments/attempts/{}/investigate-order` | Falta en Bruno |
| `POST /api/v1/admin/payments/attempts/{}/reconcile-order` | Falta en Bruno |
| `POST /api/v1/auth/login` | Headers distintos; Variables distintas: PM authPassword,authToken,baseUrl,userId / Bruno authPassword,baseUrl |
| `POST /api/v1/auth/refresh` | Auth distinta/heredada; Headers distintos; Variables distintas: PM authToken,baseUrl / Bruno baseUrl |
| `POST /api/v1/auth/register` | Headers distintos |
| `POST /api/v1/auth/register/merchant` | Headers distintos |
| `POST /api/v1/capacity-configurations` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/capacity-exceptions` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime / Bruno baseUrl |
| `POST /api/v1/capacity/reservations` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime,storeId / Bruno baseUrl |
| `POST /api/v1/cart/items` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId,variantId / Bruno baseUrl |
| `POST /api/v1/checkout` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,endTime,serviceDate,startTime,storeId / Bruno baseUrl |
| `POST /api/v1/checkout/mercado-pago/preference` | Falta en Bruno |
| `POST /api/v1/checkout/mercado-pago/webhook` | Falta en Bruno |
| `POST /api/v1/customer/addresses` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/customer/favorites/products/{}` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/customer/products/{}/reviews` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/media/upload-signature` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/merchants/stores/{}/categories` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/merchants/stores/{}/products` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/merchants/stores/{}/products/{}/variants` | Auth distinta/heredada; Headers distintos |
| `POST /api/v1/stores` | Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/capacity-configurations/{}` | Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/capacity-exceptions/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,capacityExceptionId,endTime,serviceDate,startTime / Bruno baseUrl,capacityExceptionId |
| `PUT /api/v1/cart/items/{}` | Cuerpo distinto; Auth distinta/heredada; Headers distintos; Variables distintas: PM baseUrl,storeId,variantId / Bruno baseUrl,variantId |
| `PUT /api/v1/customer/addresses/{}` | Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/customer/products/{}/reviews` | Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/inventory/variants/{}/stock` | Falta en Bruno |
| `PUT /api/v1/merchants/stores/{}/categories/{}` | Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/merchants/stores/{}/products/{}` | Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/merchants/stores/{}/products/{}/variants/{}` | Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/stores/me` | Auth distinta/heredada; Headers distintos |
| `PUT /api/v1/users/me` | Auth distinta/heredada; Headers distintos |
