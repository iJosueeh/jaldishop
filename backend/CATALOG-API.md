# Contrato de API REST - Módulo Catalog (BE-12)

Base Path: /api/v1/merchants/stores/{storeId}

---

## 1. Categorías

### 1.1. Crear Categoría
* Método: POST
* Ruta: /api/v1/merchants/stores/{storeId}/categories
* Body: {"name": "Postres", "description": "Postres artesanales"}
* Respuesta Exitosa (201 Created): Retorna objeto CategoryResponse con id, storeId, status ACTIVE y timestamps.

### 1.2. Listar Categorías de Tienda
* Método: GET
* Ruta: /api/v1/merchants/stores/{storeId}/categories
* Respuesta Exitosa (200 OK): Retorna lista de CategoryResponse.

### 1.3. Obtener Categoría por ID
* Método: GET
* Ruta: /api/v1/merchants/stores/{storeId}/categories/{categoryId}
* Respuesta Exitosa (200 OK): Retorna objeto CategoryResponse.

### 1.4. Actualizar Categoría
* Método: PUT
* Ruta: /api/v1/merchants/stores/{storeId}/categories/{categoryId}
* Body: {"name": "Pasteles y Tartas", "description": "Variedad dulce", "status": "ACTIVE"}
* Respuesta Exitosa (200 OK): Retorna objeto CategoryResponse modificado.

---

## 2. Productos

### 2.1. Crear Producto
* Método: POST
* Ruta: /api/v1/merchants/stores/{storeId}/products
* Body: {"categoryId": "uuid", "name": "Torta Tres Leches", "slug": "torta-tres-leches", "description": "Bañada en leche", "imageUrl": "url"}
* Respuesta Exitosa (201 Created): Retorna ProductResponse con status ACTIVE.

### 2.2. Listar Productos
* Método: GET
* Ruta: /api/v1/merchants/stores/{storeId}/products
* Filtro opcional: ?categoryId={uuid}
* Respuesta Exitosa (200 OK): Retorna lista de ProductResponse.

### 2.3. Obtener Producto por ID
* Método: GET
* Ruta: /api/v1/merchants/stores/{storeId}/products/{productId}
* Respuesta Exitosa (200 OK): Retorna objeto ProductResponse.

### 2.4. Actualizar Producto
* Método: PUT
* Ruta: /api/v1/merchants/stores/{storeId}/products/{productId}
* Body: {"categoryId": "uuid", "name": "Torta Modificada", "slug": "torta-modificada", "description": "Nueva", "status": "ACTIVE"}
* Respuesta Exitosa (200 OK): Retorna ProductResponse actualizado.

---

## 3. Variantes y Presentaciones

### 3.1. Crear Variante
* Método: POST
* Ruta: /api/v1/merchants/stores/{storeId}/products/{productId}/variants
* Body: {"presentationName": "Molde Completo", "sku": "TORTA-3L", "priceAmount": 45.00, "priceCurrency": "PEN", "tracksInventory": true, "attributes": [{"name": "Tamano", "value": "Familiar"}]}
* Respuesta Exitosa (201 Created): Retorna ProductVariantResponse con id generado.

### 3.2. Listar Variantes de un Producto
* Método: GET
* Ruta: /api/v1/merchants/stores/{storeId}/products/{productId}/variants
* Respuesta Exitosa (200 OK): Retorna lista de ProductVariantResponse.

### 3.3. Obtener Variante por ID
* Método: GET
* Ruta: /api/v1/merchants/stores/{storeId}/products/{productId}/variants/{variantId}
* Respuesta Exitosa (200 OK): Retorna objeto ProductVariantResponse.

### 3.4. Actualizar Variante
* Método: PUT
* Ruta: /api/v1/merchants/stores/{storeId}/products/{productId}/variants/{variantId}
* Body: {"presentationName": "Molde Mediano", "sku": "TORTA-3L-M", "priceAmount": 35.00, "priceCurrency": "PEN", "tracksInventory": true, "status": "ACTIVE", "attributes": []}
* Respuesta Exitosa (200 OK): Retorna ProductVariantResponse actualizado.