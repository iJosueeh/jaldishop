# Evidencia de validaciones y persistencia

Índice exhaustivo de anotaciones DTO y guardas explícitas: no equivale a revisión semántica completa. Los paths y líneas corresponden al estado local auditado.

## address

### backend/src/main/java/com/jaldishop/backend/address/application/service/CreateCustomerAddressService.java

```java
39: throw new BusinessRuleException(                     "ADDRESS_LIMIT_EXCEEDED",                     "Has alcanzado el límite máximo de " + MAX_ADDRESSES_PER_CUSTOMER + " direcciones guardadas."
53: throw new BusinessRuleException(                     "INVALID_UBIGEO",                     "El código UBIGEO '" + request.ubigeoCode() + "' no corresponde al departamento '"
```

### backend/src/main/java/com/jaldishop/backend/address/application/service/UpdateCustomerAddressService.java

```java
46: throw new BusinessRuleException(                     "INVALID_UBIGEO",                     "El código UBIGEO '" + request.ubigeoCode() + "' no corresponde al departamento '"
```

### backend/src/main/java/com/jaldishop/backend/address/infrastructure/ubigeo/UbigeoCatalogService.java

```java
50: throw new IllegalStateException("Error al inicializar catálogo oficial de UBIGEO.", e);         }     }
```

### backend/src/main/java/com/jaldishop/backend/address/web/dto/CreateCustomerAddressRequest.java

```java
10: @NotBlank(message = "El alias es obligatorio.")         @Size(max = 50, message = "El alias no puede exceder los 50 caracteres.")         String alias,
11: @Size(max = 50, message = "El alias no puede exceder los 50 caracteres.")         String alias,
14: @NotBlank(message = "El nombre del destinatario es obligatorio.")         @Size(max = 200, message = "El nombre del destinatario no puede exceder los 200 caracteres.")         String recipientName,
15: @Size(max = 200, message = "El nombre del destinatario no puede exceder los 200 caracteres.")         String recipientName,
18: @NotBlank(message = "El teléfono de contacto es obligatorio.")         @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe ser un número celular peruano válido de 9 dígitos que comience con 9.")         String recipientPhone,
19: @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe ser un número celular peruano válido de 9 dígitos que comience con 9.")         String recipientPhone,
22: @NotBlank(message = "La dirección exacta es obligatoria.")         @Size(max = 255, message = "La dirección exacta no puede exceder los 255 caracteres.")         String streetAddress,
23: @Size(max = 255, message = "La dirección exacta no puede exceder los 255 caracteres.")         String streetAddress,
26: @NotBlank(message = "El departamento es obligatorio.")         @Size(max = 100, message = "El departamento no puede exceder los 100 caracteres.")         String department,
27: @Size(max = 100, message = "El departamento no puede exceder los 100 caracteres.")         String department,
30: @NotBlank(message = "La provincia es obligatoria.")         @Size(max = 100, message = "La provincia no puede exceder los 100 caracteres.")         String province,
31: @Size(max = 100, message = "La provincia no puede exceder los 100 caracteres.")         String province,
34: @NotBlank(message = "El distrito es obligatorio.")         @Size(max = 100, message = "El distrito no puede exceder los 100 caracteres.")         String district,
35: @Size(max = 100, message = "El distrito no puede exceder los 100 caracteres.")         String district,
38: @NotBlank(message = "El código UBIGEO es obligatorio.")         @Pattern(regexp = "^\\d{6}$", message = "El código UBIGEO debe constar de exactamente 6 dígitos numéricos.")         String ubigeoCode,
39: @Pattern(regexp = "^\\d{6}$", message = "El código UBIGEO debe constar de exactamente 6 dígitos numéricos.")         String ubigeoCode,
42: @Size(max = 300, message = "La referencia no puede exceder los 300 caracteres.")         String reference,
```

### backend/src/main/java/com/jaldishop/backend/address/web/dto/UpdateCustomerAddressRequest.java

```java
10: @NotBlank(message = "El alias es obligatorio.")         @Size(max = 50, message = "El alias no puede exceder los 50 caracteres.")         String alias,
11: @Size(max = 50, message = "El alias no puede exceder los 50 caracteres.")         String alias,
14: @NotBlank(message = "El nombre del destinatario es obligatorio.")         @Size(max = 200, message = "El nombre del destinatario no puede exceder los 200 caracteres.")         String recipientName,
15: @Size(max = 200, message = "El nombre del destinatario no puede exceder los 200 caracteres.")         String recipientName,
18: @NotBlank(message = "El teléfono de contacto es obligatorio.")         @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe ser un número celular peruano válido de 9 dígitos que comience con 9.")         String recipientPhone,
19: @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe ser un número celular peruano válido de 9 dígitos que comience con 9.")         String recipientPhone,
22: @NotBlank(message = "La dirección exacta es obligatoria.")         @Size(max = 255, message = "La dirección exacta no puede exceder los 255 caracteres.")         String streetAddress,
23: @Size(max = 255, message = "La dirección exacta no puede exceder los 255 caracteres.")         String streetAddress,
26: @NotBlank(message = "El departamento es obligatorio.")         @Size(max = 100, message = "El departamento no puede exceder los 100 caracteres.")         String department,
27: @Size(max = 100, message = "El departamento no puede exceder los 100 caracteres.")         String department,
30: @NotBlank(message = "La provincia es obligatoria.")         @Size(max = 100, message = "La provincia no puede exceder los 100 caracteres.")         String province,
31: @Size(max = 100, message = "La provincia no puede exceder los 100 caracteres.")         String province,
34: @NotBlank(message = "El distrito es obligatorio.")         @Size(max = 100, message = "El distrito no puede exceder los 100 caracteres.")         String district,
35: @Size(max = 100, message = "El distrito no puede exceder los 100 caracteres.")         String district,
38: @NotBlank(message = "El código UBIGEO es obligatorio.")         @Pattern(regexp = "^\\d{6}$", message = "El código UBIGEO debe constar de exactamente 6 dígitos numéricos.")         String ubigeoCode,
39: @Pattern(regexp = "^\\d{6}$", message = "El código UBIGEO debe constar de exactamente 6 dígitos numéricos.")         String ubigeoCode,
42: @Size(max = 300, message = "La referencia no puede exceder los 300 caracteres.")         String reference,
```

## capacity

### backend/src/main/java/com/jaldishop/backend/capacity/application/availability/service/GetEffectiveCapacityService.java

```java
59: throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "El identificador de la tienda es obligatorio.");         }         if (query.serviceDate() == null) {
62: throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "La fecha es obligatoria para consultar la capacidad efectiva.");         }         if (query.startTime() == null || query.endTime() == null) {
65: throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "Debe indicar la franja horaria (startTime y endTime).");         }         if (!query.startTime().isBefore(query.endTime())) {
68: throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "startTime debe ser anterior a endTime.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/availability/service/ListStoreCapacitySlotsService.java

```java
47: throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "El identificador de la tienda es obligatorio.");         }         if (serviceDate == null) {
50: throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "La fecha es obligatoria para consultar las franjas.");         }         LocalDate today = LocalDate.now(BUSINESS_ZONE);
54: throw new BusinessRuleException("INVALID_CAPACITY_QUERY",                     "No es posible consultar franjas de fechas pasadas.");         }
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/configuration/service/CreateCapacityConfigurationService.java

```java
31: throw new BusinessRuleException("CAPACITY_OVERLAP",                         "Ya existe una configuración activa con franja horaria solapada para este día.");             }
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/configuration/service/ToggleCapacityConfigurationStatusService.java

```java
40: throw new ResourceNotFoundException("CapacityConfiguration", configId);         }
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/configuration/service/ToggleCapacityExceptionStatusService.java

```java
41: throw new ResourceNotFoundException(                     "CAPACITY_EXCEPTION_NOT_FOUND",                     "La excepción de capacidad no pertenece a esta tienda.");
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/configuration/service/UpdateCapacityConfigurationService.java

```java
29: throw new ResourceNotFoundException("CapacityConfiguration", command.configId());         }
45: throw new BusinessRuleException("CAPACITY_OVERLAP",                         "Ya existe una configuración activa con franja horaria solapada para este día.");             }
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/configuration/service/UpdateCapacityExceptionService.java

```java
30: throw new ResourceNotFoundException(                     "CAPACITY_EXCEPTION_NOT_FOUND",                     "La excepción de capacidad no pertenece a esta tienda.");
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/reservation/service/CommitCapacityReservationService.java

```java
49: throw new ConflictException(                     "RESERVATION_PROTECTION_EXPIRED",                     "La protección de pago de la reserva de capacidad ya venció; la compra no puede confirmarse automáticamente."
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/reservation/service/CreateCapacityReservationService.java

```java
44: throw new ConflictException("CAPACITY_UNAVAILABLE",                     "No hay capacidad disponible para la franja horaria seleccionada.");         }
54: throw new ConflictException("ALREADY_RESERVED",                     "Ya existe una reserva activa para esta franja horaria.");         }
63: throw new ConflictException("CAPACITY_EXHAUSTED",                     "No quedan cupos disponibles para la franja horaria seleccionada.");         }
86: throw new ConflictException("CAPACITY_UNAVAILABLE",                 "No hay capacidad disponible para la franja horaria seleccionada.");     }
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/reservation/service/GetCapacityReservationService.java

```java
29: throw new ResourceNotFoundException("reserva de capacidad", id);         }
```

### backend/src/main/java/com/jaldishop/backend/capacity/application/reservation/service/ReleaseCapacityReservationService.java

```java
30: throw new ResourceNotFoundException("reserva de capacidad", id);         }
38: throw new ConflictException("RESERVATION_COMMITTED",                     "La reserva ya está comprometida y no puede liberarse.");         }
```

### backend/src/main/java/com/jaldishop/backend/capacity/domain/model/CapacityConfiguration.java

```java
36: throw new IllegalArgumentException("storeId no puede ser nulo.");         }         if (dayOfWeek < 0 || dayOfWeek > 6) {
39: throw new IllegalArgumentException("dayOfWeek debe estar entre 0 y 6.");         }         if (maxCapacity < 0) {
42: throw new IllegalArgumentException("maxCapacity no puede ser negativo.");         }         CapacityPeriod effectivePeriod = period != null ? period : CapacityPeriod.allDay();
74: throw new IllegalArgumentException("dayOfWeek debe estar entre 0 y 6.");         }         if (maxCapacity < 0) {
77: throw new IllegalArgumentException("maxCapacity no puede ser negativo.");         }
```

### backend/src/main/java/com/jaldishop/backend/capacity/domain/model/CapacityException.java

```java
39: throw new IllegalArgumentException("storeId no puede ser nulo.");         }         if (serviceDate == null) {
42: throw new IllegalArgumentException("serviceDate no puede ser nulo.");         }         if (exceptionCapacity < 0) {
45: throw new IllegalArgumentException("exceptionCapacity no puede ser negativo.");         }         CapacityPeriod effectivePeriod = period != null ? period : CapacityPeriod.allDay();
83: throw new IllegalArgumentException("serviceDate no puede ser nulo.");         }         if (exceptionCapacity < 0) {
86: throw new IllegalArgumentException("exceptionCapacity no puede ser negativo.");         }         this.serviceDate = serviceDate;
```

### backend/src/main/java/com/jaldishop/backend/capacity/domain/model/CapacityPeriod.java

```java
14: throw new IllegalArgumentException("startTime y endTime deben ser ambos nulos o ambos con valor.");         }         if (startTime != null && !startTime.isBefore(endTime)) {
17: throw new IllegalArgumentException("startTime debe ser anterior a endTime.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/capacity/domain/model/CapacityReservation.java

```java
45: throw new IllegalArgumentException("storeId no puede ser nulo.");         }         if (userId == null) {
48: throw new IllegalArgumentException("userId no puede ser nulo.");         }         if (serviceDate == null) {
51: throw new IllegalArgumentException("serviceDate no puede ser nulo.");         }         if (period == null || period.isAllDay()) {
54: throw new IllegalArgumentException("startTime y endTime son obligatorios para reservar la franja.");         }
66: throw new IllegalArgumentException("startTime y endTime son obligatorios para reservar la franja.");         }         return create(storeId, userId, serviceDate, CapacityPeriod.of(startTime, endTime));
110: throw new IllegalStateException("Una reserva comprometida no puede liberarse sin cancelar el pedido.");         }         if (status == CapacityReservationStatus.EXPIRED
114: throw new IllegalStateException("La reserva ya se encuentra en un estado terminal.");         }         this.status = CapacityReservationStatus.RELEASED;
135: throw new IllegalStateException("Solo una reserva activa puede protegerse por pago.");         }         if (isExpired(now)) {
138: throw new IllegalStateException("La reserva venció y no puede protegerse por pago.");         }         this.status = CapacityReservationStatus.PAYMENT_PROTECTED;
147: throw new IllegalStateException("Solo una reserva protegida por pago puede comprometerse.");         }         this.status = CapacityReservationStatus.COMMITTED;
```

### backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacityReservationController.java

```java
54: throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden consultar la disponibilidad.");         }
67: throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden reservar capacidad.");         }
84: throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden consultar reservas.");         }
96: throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden liberar reservas.");         }
```

### backend/src/main/java/com/jaldishop/backend/capacity/web/controller/CapacitySlotsController.java

```java
46: throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden consultar las franjas de capacidad.");         }
61: throw new IllegalArgumentException("El identificador de la tienda es obligatorio.");         }         try {
66: throw new IllegalArgumentException("El identificador de la tienda no es válido.");         }     }
72: throw new IllegalArgumentException("La fecha es obligatoria para consultar las franjas.");         }         try {
77: throw new IllegalArgumentException("La fecha debe tener formato YYYY-MM-DD.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/capacity/web/dto/CreateCapacityConfigurationRequest.java

```java
9: @Min(0) @Max(6) int dayOfWeek,         LocalTime startTime,         LocalTime endTime,
12: @Min(0) int maxCapacity ) {}
```

### backend/src/main/java/com/jaldishop/backend/capacity/web/dto/CreateCapacityExceptionRequest.java

```java
10: @NotNull LocalDate serviceDate,         LocalTime startTime,         LocalTime endTime,
13: @Min(0) int exceptionCapacity,         String reason ) {}
```

### backend/src/main/java/com/jaldishop/backend/capacity/web/dto/CreateCapacityReservationRequest.java

```java
10: @NotNull UUID storeId,         @NotNull LocalDate serviceDate,         @NotNull LocalTime startTime,
11: @NotNull LocalDate serviceDate,         @NotNull LocalTime startTime,         @NotNull LocalTime endTime
12: @NotNull LocalTime startTime,         @NotNull LocalTime endTime ) {}
13: @NotNull LocalTime endTime ) {}
```

### backend/src/main/java/com/jaldishop/backend/capacity/web/dto/UpdateCapacityConfigurationRequest.java

```java
9: @Min(0) @Max(6) int dayOfWeek,         LocalTime startTime,         LocalTime endTime,
12: @Min(0) int maxCapacity ) {}
```

### backend/src/main/java/com/jaldishop/backend/capacity/web/dto/UpdateCapacityExceptionRequest.java

```java
10: @NotNull LocalDate serviceDate,         LocalTime startTime,         LocalTime endTime,
13: @Min(0) int exceptionCapacity,         String reason ) {}
```

## cart

### backend/src/main/java/com/jaldishop/backend/cart/application/service/AddItemToCartService.java

```java
49: throw new IllegalArgumentException("La cantidad debe ser mayor a cero");         }
58: throw new ConflictException("VARIANT_INACTIVE", "La variante de producto no se encuentra disponible");         }
65: throw new ConflictException("VARIANT_STORE_MISMATCH", "El producto no pertenece a la tienda especificada");         }
69: throw new ConflictException("PRODUCT_INACTIVE", "El producto no se encuentra activo");         }
88: throw new IllegalArgumentException("El ID de la tienda es obligatorio");         }         storeRepository.findById(storeId)
```

### backend/src/main/java/com/jaldishop/backend/cart/application/service/ClearCartService.java

```java
51: throw new IllegalArgumentException("El ID de la tienda es obligatorio");         }         storeRepository.findById(storeId)
```

### backend/src/main/java/com/jaldishop/backend/cart/application/service/GetCartService.java

```java
44: throw new IllegalArgumentException("El ID de la tienda es obligatorio");         }         storeRepository.findById(storeId)
```

### backend/src/main/java/com/jaldishop/backend/cart/application/service/RemoveCartItemService.java

```java
47: throw new IllegalArgumentException("El ID de la tienda es obligatorio");         }         storeRepository.findById(storeId)
```

### backend/src/main/java/com/jaldishop/backend/cart/application/service/UpdateCartItemQuantityService.java

```java
41: throw new ResourceNotFoundException("El producto no se encuentra en el carrito");         }
51: throw new IllegalArgumentException("El ID de la tienda es obligatorio");         }         storeRepository.findById(storeId)
```

### backend/src/main/java/com/jaldishop/backend/cart/domain/model/Cart.java

```java
22: throw new IllegalArgumentException("El ID del carrito es obligatorio");         }         if (userId == null) {
25: throw new IllegalArgumentException("El ID del usuario es obligatorio");         }         if (storeId == null) {
28: throw new IllegalArgumentException("El ID de la tienda es obligatorio");         }
46: throw new IllegalArgumentException("El ID de la variante es obligatorio");         }         if (quantity <= 0) {
49: throw new IllegalArgumentException("La cantidad debe ser mayor a cero");         }
66: throw new IllegalArgumentException("El ID de la variante es obligatorio");         }
```

### backend/src/main/java/com/jaldishop/backend/cart/domain/model/CartItem.java

```java
22: throw new IllegalArgumentException("El ID del carrito es obligatorio");         }         if (variantId == null) {
25: throw new IllegalArgumentException("El ID de la variante es obligatorio");         }         if (quantity <= 0) {
28: throw new IllegalArgumentException("La cantidad debe ser mayor a cero");         }         if (referencePriceAmount == null || referencePriceAmount.compareTo(BigDecimal.ZERO) < 0) {
31: throw new IllegalArgumentException("El precio de referencia no puede ser negativo");         }
52: throw new IllegalArgumentException("La cantidad debe ser mayor a cero");         }         this.quantity = newQuantity;
60: throw new IllegalArgumentException("La cantidad a sumar debe ser mayor a cero");         }         this.quantity += additionalQuantity;
68: throw new IllegalArgumentException("El precio de referencia no puede ser negativo");         }         this.referencePriceAmount = priceAmount;
```

### backend/src/main/java/com/jaldishop/backend/cart/web/dto/AddItemToCartRequest.java

```java
8: @NotNull(message = "El ID de la tienda es obligatorio")         UUID storeId,
11: @NotNull(message = "El ID de la variante es obligatorio")         UUID variantId,
14: @Min(value = 1, message = "La cantidad debe ser al menos 1")         int quantity ) {}
```

### backend/src/main/java/com/jaldishop/backend/cart/web/dto/UpdateCartItemRequest.java

```java
8: @NotNull(message = "El ID de la tienda es obligatorio")         UUID storeId,
11: @Min(value = 0, message = "La cantidad no puede ser negativa")         int quantity ) {}
```

## catalog

### backend/src/main/java/com/jaldishop/backend/catalog/application/service/CreateCategoryService.java

```java
28: throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío");         }
33: throw new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría con este nombre en la tienda");         }
```

### backend/src/main/java/com/jaldishop/backend/catalog/application/service/CreateProductService.java

```java
32: throw new IllegalArgumentException("El nombre del producto no puede estar vacío");         }
45: throw new ConflictException("PRODUCT_SLUG_ALREADY_EXISTS", "Ya existe un producto con el slug '" + resolvedSlug + "' en esta tienda");         }
```

### backend/src/main/java/com/jaldishop/backend/catalog/application/service/CreateProductVariantService.java

```java
38: throw new IllegalArgumentException("El nombre de la presentación no puede estar vacío");         }
42: throw new IllegalArgumentException("El precio debe ser mayor a cero");         }
49: throw new ConflictException("SKU_ALREADY_EXISTS", "El SKU '" + trimmedSku + "' ya está en uso");             }         }
```

### backend/src/main/java/com/jaldishop/backend/catalog/application/service/GetProductVariantsService.java

```java
47: throw new IllegalArgumentException("La variante no pertenece al producto especificado");         }
```

### backend/src/main/java/com/jaldishop/backend/catalog/application/service/UpdateCategoryService.java

```java
33: throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío");         }
38: throw new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría con este nombre en la tienda");         }
```

### backend/src/main/java/com/jaldishop/backend/catalog/application/service/UpdateProductService.java

```java
36: throw new IllegalArgumentException("El nombre del producto no puede estar vacío");         }
48: throw new ConflictException("PRODUCT_SLUG_ALREADY_EXISTS", "Ya existe un producto con el slug '" + resolvedSlug + "' en esta tienda");         }
```

### backend/src/main/java/com/jaldishop/backend/catalog/application/service/UpdateProductVariantService.java

```java
39: throw new IllegalArgumentException("La variante no pertenece al producto especificado");         }
43: throw new IllegalArgumentException("El nombre de la presentación no puede estar vacío");         }
50: throw new ConflictException("SKU_ALREADY_EXISTS", "El SKU '" + trimmedSku + "' ya está en uso");             }         }
```

### backend/src/main/java/com/jaldishop/backend/catalog/application/util/SlugUtils.java

```java
16: throw new IllegalArgumentException("El texto para generar el slug no puede estar vacío");         }         String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
```

### backend/src/main/java/com/jaldishop/backend/catalog/domain/model/ProductImage.java

```java
17: throw new IllegalArgumentException("El ID de la imagen no puede ser nulo.");         }         if (productId == null) {
20: throw new IllegalArgumentException("El ID del producto no puede ser nulo.");         }         if (imageUrl == null || imageUrl.isBlank()) {
23: throw new IllegalArgumentException("La URL de la imagen no puede estar vacía.");         }         if (position < 0) {
26: throw new IllegalArgumentException("La posición debe ser mayor o igual a 0.");         }         this.id = id;
70: throw new IllegalArgumentException("La posición debe ser mayor o igual a 0.");         }         this.position = position;
77: throw new IllegalArgumentException("La URL de la imagen no puede estar vacía.");         }         this.imageUrl = imageUrl.trim();
```

### backend/src/main/java/com/jaldishop/backend/catalog/domain/model/ProductVariant.java

```java
30: throw new IllegalArgumentException("El precio debe ser mayor a cero");         }         this.id = id;
67: throw new IllegalArgumentException("El precio debe ser mayor a cero");         }         this.presentationName = presentationName;
```

### backend/src/main/java/com/jaldishop/backend/catalog/domain/model/VariantAttribute.java

```java
9: throw new IllegalArgumentException("El nombre del atributo no puede estar vacío");         }         if (value == null || value.isBlank()) {
12: throw new IllegalArgumentException("El valor del atributo no puede estar vacío");         }         this.name = name.trim();
```

### backend/src/main/java/com/jaldishop/backend/catalog/web/dto/CreateCategoryRequest.java

```java
7: @NotBlank(message = "El nombre de la categoría es obligatorio")         @Size(max = 120, message = "El nombre de la categoría no debe exceder 120 caracteres")         String name,
8: @Size(max = 120, message = "El nombre de la categoría no debe exceder 120 caracteres")         String name,
```

### backend/src/main/java/com/jaldishop/backend/catalog/web/dto/CreateProductRequest.java

```java
9: @NotNull(message = "El ID de la categoría es obligatorio")         UUID categoryId,
12: @NotBlank(message = "El nombre del producto es obligatorio")         @Size(max = 160, message = "El nombre del producto no debe exceder 160 caracteres")         String name,
13: @Size(max = 160, message = "El nombre del producto no debe exceder 160 caracteres")         String name,
16: @Size(max = 180, message = "El slug del producto no debe exceder 180 caracteres")         String slug,
```

### backend/src/main/java/com/jaldishop/backend/catalog/web/dto/CreateProductVariantRequest.java

```java
14: @NotBlank(message = "El nombre de presentación es obligatorio")         @Size(max = 120, message = "El nombre de presentación no debe exceder 120 caracteres")         String presentationName,
15: @Size(max = 120, message = "El nombre de presentación no debe exceder 120 caracteres")         String presentationName,
18: @Size(max = 100, message = "El SKU no debe exceder 100 caracteres")         String sku,
21: @NotNull(message = "El monto del precio es obligatorio")         @DecimalMin(value = "0.01", message = "El monto del precio debe ser mayor a cero")         BigDecimal priceAmount,
22: @DecimalMin(value = "0.01", message = "El monto del precio debe ser mayor a cero")         BigDecimal priceAmount,
25: @NotBlank(message = "La moneda del precio es obligatoria")         @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un código ISO de 3 letras en mayúsculas")         String priceCurrency,
26: @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un código ISO de 3 letras en mayúsculas")         String priceCurrency,
31: @Valid         List<VariantAttributeDto> attributes ) {}
```

### backend/src/main/java/com/jaldishop/backend/catalog/web/dto/UpdateCategoryRequest.java

```java
7: @NotBlank(message = "El nombre de la categoría es obligatorio")         @Size(max = 120, message = "El nombre de la categoría no debe exceder 120 caracteres")         String name,
8: @Size(max = 120, message = "El nombre de la categoría no debe exceder 120 caracteres")         String name,
```

### backend/src/main/java/com/jaldishop/backend/catalog/web/dto/UpdateProductRequest.java

```java
9: @NotNull(message = "El ID de la categoría es obligatorio")         UUID categoryId,
12: @NotBlank(message = "El nombre del producto es obligatorio")         @Size(max = 160, message = "El nombre del producto no debe exceder 160 caracteres")         String name,
13: @Size(max = 160, message = "El nombre del producto no debe exceder 160 caracteres")         String name,
16: @Size(max = 180, message = "El slug del producto no debe exceder 180 caracteres")         String slug,
```

### backend/src/main/java/com/jaldishop/backend/catalog/web/dto/UpdateProductVariantRequest.java

```java
14: @NotBlank(message = "El nombre de presentación es obligatorio")         @Size(max = 120, message = "El nombre de presentación no debe exceder 120 caracteres")         String presentationName,
15: @Size(max = 120, message = "El nombre de presentación no debe exceder 120 caracteres")         String presentationName,
18: @Size(max = 100, message = "El SKU no debe exceder 100 caracteres")         String sku,
21: @NotNull(message = "El monto del precio es obligatorio")         @DecimalMin(value = "0.01", message = "El monto del precio debe ser mayor a cero")         BigDecimal priceAmount,
22: @DecimalMin(value = "0.01", message = "El monto del precio debe ser mayor a cero")         BigDecimal priceAmount,
25: @NotBlank(message = "La moneda del precio es obligatoria")         @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un código ISO de 3 letras en mayúsculas")         String priceCurrency,
26: @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un código ISO de 3 letras en mayúsculas")         String priceCurrency,
33: @Valid         List<VariantAttributeDto> attributes ) {}
```

### backend/src/main/java/com/jaldishop/backend/catalog/web/dto/VariantAttributeDto.java

```java
6: @NotBlank(message = "El nombre del atributo es obligatorio")         String name,
9: @NotBlank(message = "El valor del atributo es obligatorio")         String value ) {}
```

## checkout

### backend/src/main/java/com/jaldishop/backend/checkout/application/command/ConfirmPurchaseCommand.java

```java
25: throw new IllegalArgumentException("El ID del pago es obligatorio para confirmar la compra.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/checkout/application/command/InitiateCheckoutCommand.java

```java
29: throw new IllegalArgumentException("El ID del usuario es obligatorio para iniciar el checkout.");         }         if (storeId == null) {
32: throw new IllegalArgumentException("El ID de la tienda es obligatorio para iniciar el checkout.");         }         if (serviceDate == null) {
35: throw new BusinessRuleException("SERVICE_DATE_REQUIRED", "La fecha de servicio es obligatoria.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/checkout/application/result/CheckoutItemSnapshotAssembler.java

```java
47: throw new ConflictException("VARIANT_INACTIVE",                         "La presentación '" + variant.getPresentationName() + "' no se encuentra disponible.");             }
55: throw new ConflictException("VARIANT_STORE_MISMATCH",                         "El producto '" + product.getName() + "' no pertenece a esta tienda.");             }
60: throw new ConflictException("PRODUCT_INACTIVE",                         "El producto '" + product.getName() + "' no se encuentra activo.");             }
```

### backend/src/main/java/com/jaldishop/backend/checkout/application/service/ConfirmPurchaseService.java

```java
220: throw new ConflictException(PaymentErrorCodes.RN_PAY_02, "La orden solo puede confirmarse con un pago aprobado.");         }         return payment;
229: throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");         }         return store;
238: throw new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío.");         }         return cart;
252: throw new ConflictException("INVALID_SNAPSHOT", "El snapshot de ítems en el pago está corrupto o malformado.");         }
256: throw new ConflictException("INVALID_SNAPSHOT", "El snapshot de ítems no contiene productos para confirmar la compra.");         }
262: throw new ConflictException("INVALID_SNAPSHOT", "Uno de los ítems en el snapshot carece de variantId.");             }             if (item.quantity() <= 0) {
265: throw new ConflictException("INVALID_SNAPSHOT", "Cantidad inválida en el snapshot para la variante: " + item.variantId());             }             if (item.unitPriceAmount() == null || item.unitPriceAmount().compareTo(BigDecimal.ZERO) < 0) {
268: throw new ConflictException("INVALID_SNAPSHOT", "Precio unitario inválido en el snapshot para la variante: " + item.variantId());             }             BigDecimal expectedItemSubtotal = item.unitPriceAmount().multiply(BigDecimal.valueOf(item.quantity()));
272: throw new ConflictException("INVALID_SNAPSHOT", "Subtotal inconsistente en el snapshot para la variante: " + item.variantId());             }             calculatedSubtotal = calculatedSubtotal.add(expectedItemSubtotal);
281: throw new ConflictException("INVALID_SNAPSHOT", "El subtotal del snapshot no coincide con el subtotal registrado en el pago.");         }
```

### backend/src/main/java/com/jaldishop/backend/checkout/application/service/InitiateCheckoutService.java

```java
57: throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");         }
66: throw new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío.");         }
```

### backend/src/main/java/com/jaldishop/backend/checkout/domain/service/CheckoutFulfillmentValidator.java

```java
16: throw new IllegalArgumentException("La tienda es obligatoria para validar el cumplimiento.");         }         if (fulfillmentType == null) {
19: throw new BusinessRuleException("FULFILLMENT_TYPE_REQUIRED", "El tipo de entrega (PICKUP o DELIVERY) es obligatorio.");         }
24: throw new ConflictException("DELIVERY_NOT_AVAILABLE", "La tienda no realiza envíos a domicilio.");             }             if (deliveryAddress == null || deliveryAddress.isBlank()) {
27: throw new BusinessRuleException("DELIVERY_ADDRESS_REQUIRED", "La dirección de entrega es obligatoria para envíos a domicilio.");             }         } else if (fulfillmentType == CheckoutFulfillmentType.PICKUP) {
31: throw new ConflictException("PICKUP_NOT_AVAILABLE", "La tienda no permite retiro en tienda.");             }         }
```

### backend/src/main/java/com/jaldishop/backend/checkout/web/dto/InitiateCheckoutRequest.java

```java
14: @NotNull(message = "El storeId es obligatorio.")         UUID storeId,
17: @NotNull(message = "El tipo de entrega (PICKUP o DELIVERY) es obligatorio.")         CheckoutFulfillmentType fulfillmentType,
20: @NotNull(message = "La fecha de entrega/retiro es obligatoria.")         @FutureOrPresent(message = "La fecha no puede ser anterior al día de hoy.")         LocalDate serviceDate,
21: @FutureOrPresent(message = "La fecha no puede ser anterior al día de hoy.")         LocalDate serviceDate,
28: @Size(max = 200, message = "El nombre del cliente no puede exceder los 200 caracteres.")         String customerName,
31: @Size(max = 30, message = "El teléfono no puede exceder los 30 caracteres.")         String customerPhone,
34: @Size(max = 254, message = "El correo electrónico no puede exceder los 254 caracteres.")         String customerEmail,
37: @Size(max = 500, message = "La dirección de entrega no puede exceder los 500 caracteres.")         String deliveryAddress,
40: @Size(max = 300, message = "La referencia de entrega no puede exceder los 300 caracteres.")         String deliveryReference,
47: @Size(max = 80, message = "El código de descuento no puede exceder los 80 caracteres.")         String discountCode ) {}
```

## favorite

## identity

### backend/src/main/java/com/jaldishop/backend/identity/application/service/AuthenticateUserService.java

```java
35: throw new BadCredentialsException("Credenciales inválidas.");         }
39: throw new BusinessRuleException("USER_INACTIVE", "El usuario se encuentra inactivo.");         }
```

### backend/src/main/java/com/jaldishop/backend/identity/application/service/ChangeUserStatusService.java

```java
31: throw new IllegalArgumentException("Estado no soportado para cambio de estado de usuario: " + command.targetStatus());         }
```

### backend/src/main/java/com/jaldishop/backend/identity/application/service/RegisterCustomerService.java

```java
36: throw new ConflictException("EMAIL_ALREADY_EXISTS", "El correo ya esta registrado.");         }
```

### backend/src/main/java/com/jaldishop/backend/identity/application/service/RegisterMerchantService.java

```java
55: throw new ConflictException(                         "MERCHANT_ALREADY_EXISTS",                         "El correo ya está registrado como comerciante. Inicia sesión para acceder a tu panel."
62: throw new BadCredentialsException(                         "Este correo ya esta registrado como cliente. La contraseña ingresada no coincide con tu cuenta."                 );
```

### backend/src/main/java/com/jaldishop/backend/identity/application/service/UpdateUserRolesService.java

```java
34: throw new IllegalArgumentException("El ID de usuario es obligatorio.");         }         if (roleNames == null || roleNames.isEmpty()) {
37: throw new IllegalArgumentException("El usuario debe tener al menos un rol asignado.");         }
44: throw new IllegalArgumentException("No puedes removerte tu propio rol de administrador.");         }
```

### backend/src/main/java/com/jaldishop/backend/identity/domain/model/Role.java

```java
12: throw new IllegalArgumentException("ID no puede ser nulo.");         }
16: throw new IllegalArgumentException("Nombre no puede ser nulo.");         }
```

### backend/src/main/java/com/jaldishop/backend/identity/domain/model/User.java

```java
44: throw new IllegalArgumentException("El usuario debe contener un rol minimo.");         }
102: throw new IllegalArgumentException(                     field + " no puede ser nulo o vacio."             );
110: throw new IllegalArgumentException("El rol no puede ser nulo.");         }         this.roles.add(role);
118: throw new IllegalArgumentException("El usuario debe contener al menos un rol.");         }         this.roles.clear();
138: throw new IllegalArgumentException("No puedes suspender tu propia cuenta de administrador.");         }         if (this.status == UserStatus.SUSPENDED) {
141: throw new ConflictException("USER_ALREADY_SUSPENDED", "El usuario ya se encuentra suspendido.");         }         this.status = UserStatus.SUSPENDED;
149: throw new ConflictException("USER_ALREADY_ACTIVE", "El usuario ya se encuentra activo.");         }         this.status = UserStatus.ACTIVE;
```

### backend/src/main/java/com/jaldishop/backend/identity/web/controller/AuthController.java

```java
107: throw new AccessDeniedException("Usuario no autenticado.");         }         AuthResult result = refreshTokenService.execute(principal.userId());
```

### backend/src/main/java/com/jaldishop/backend/identity/web/dto/LoginRequest.java

```java
7: @Email @NotBlank String email,         @NotBlank String password ) {}
8: @NotBlank String password ) {}
```

### backend/src/main/java/com/jaldishop/backend/identity/web/dto/RegisterMerchantRequest.java

```java
15: @Email @NotBlank String email,    @NotBlank @Size(min = 6) String password,    @NotBlank String firstName,
16: @NotBlank @Size(min = 6) String password,    @NotBlank String firstName,    @NotBlank String lastName,
17: @NotBlank String firstName,    @NotBlank String lastName,    @ValidPhone String phone,
18: @NotBlank String lastName,    @ValidPhone String phone,    @NotBlank @Size(min = 2, max = 160) String storeName,
19: @ValidPhone String phone,    @NotBlank @Size(min = 2, max = 160) String storeName,    String businessType,
20: @NotBlank @Size(min = 2, max = 160) String storeName,    String businessType,    @ValidPhone String storeContactPhone,
22: @ValidPhone String storeContactPhone,    String address,    String addressReference,
25: @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,    boolean pickupEnabled,
26: @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,    boolean pickupEnabled,    boolean deliveryEnabled,
```

### backend/src/main/java/com/jaldishop/backend/identity/web/dto/RegisterRequest.java

```java
9: @Email @NotBlank String email,         @NotBlank @Size(min = 6) String password,         @NotBlank String firstName,
10: @NotBlank @Size(min = 6) String password,         @NotBlank String firstName,         @NotBlank String lastName,
11: @NotBlank String firstName,         @NotBlank String lastName,         @ValidPhone String phone
12: @NotBlank String lastName,         @ValidPhone String phone ) {}
13: @ValidPhone String phone ) {}
```

### backend/src/main/java/com/jaldishop/backend/identity/web/dto/UpdateUserRequest.java

```java
8: @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String firstName,    @NotBlank(message = "El apellido es obligatorio") @Size(max = 100) String lastName,    @ValidPhone String phone
9: @NotBlank(message = "El apellido es obligatorio") @Size(max = 100) String lastName,    @ValidPhone String phone ) {}
10: @ValidPhone String phone ) {}
```

### backend/src/main/java/com/jaldishop/backend/identity/web/dto/UpdateUserRolesRequest.java

```java
9: @NotEmpty(message = "Debe proporcionar al menos un rol.")         Set<RoleName> roles ) {}
```

## inventory

### backend/src/main/java/com/jaldishop/backend/inventory/application/service/CommitInventoryReservationService.java

```java
80: throw new ConflictException(                         "INSUFFICIENT_STOCK",                         "Stock insuficiente para confirmar la compra de la variante: " + variantId
```

### backend/src/main/java/com/jaldishop/backend/inventory/application/service/ReserveInventoryService.java

```java
65: throw new ConflictException(                         "INVENTORY_NOT_CONFIGURED",                         "El inventario no está configurado para la variante con control de stock: " + variantId
85: throw new ConflictException(                         "INSUFFICIENT_STOCK",                         "Stock insuficiente para la variante: " + variantId +
```

### backend/src/main/java/com/jaldishop/backend/inventory/application/service/UpdateInventoryStockService.java

```java
50: throw new IllegalArgumentException("La cantidad de inventario no puede ser negativa.");         }
61: throw new BusinessRuleException(                     "INVENTORY_TRACKING_DISABLED",                     "La variante no tiene activado el seguimiento de inventario (tracksInventory=false)."
72: throw new ConflictException(                     "CANNOT_REDUCE_BELOW_RESERVED",                     "No se puede ajustar la cantidad física a " + newQuantity +
```

### backend/src/main/java/com/jaldishop/backend/inventory/application/service/UpdateInventoryThresholdService.java

```java
49: throw new IllegalArgumentException("El umbral de stock bajo no puede ser negativo.");         }
60: throw new BusinessRuleException(                     "INVENTORY_TRACKING_DISABLED",                     "La variante no tiene activado el seguimiento de inventario (tracksInventory=false)."
```

### backend/src/main/java/com/jaldishop/backend/inventory/domain/model/Inventory.java

```java
16: throw new IllegalArgumentException("variantId no puede ser nulo");         }         if (quantity < 0) {
19: throw new IllegalArgumentException("quantity no puede ser negativo");         }         if (lowStockThreshold != null && lowStockThreshold < 0) {
22: throw new IllegalArgumentException("lowStockThreshold no puede ser negativo");         }         this.variantId = variantId;
```

### backend/src/main/java/com/jaldishop/backend/inventory/domain/model/InventoryReservation.java

```java
24: throw new IllegalArgumentException("El ID de la reserva de inventario no puede ser nulo.");         }         if (storeId == null) {
27: throw new IllegalArgumentException("El ID de la tienda no puede ser nulo.");         }         if (capacityReservationId == null) {
30: throw new IllegalArgumentException("El ID de la reserva de capacidad no puede ser nulo.");         }         if (variantId == null) {
33: throw new IllegalArgumentException("El ID de la variante no puede ser nulo.");         }         if (quantity <= 0) {
36: throw new IllegalArgumentException("La cantidad reservada debe ser mayor a 0.");         }         if (expiresAt == null) {
39: throw new IllegalArgumentException("La fecha de expiración no puede ser nula.");         }         this.id = id;
69: throw new IllegalStateException("Solo se pueden comprometer reservas de inventario activas.");         }         this.status = InventoryReservationStatus.COMMITTED;
77: throw new IllegalStateException("Solo se pueden liberar reservas de inventario activas.");         }         this.status = InventoryReservationStatus.RELEASED;
```

### backend/src/main/java/com/jaldishop/backend/inventory/web/dto/UpdateStockRequest.java

```java
7: @NotNull(message = "La cantidad física es requerida")         @Min(value = 0, message = "La cantidad física no puede ser negativa")         Integer quantity
8: @Min(value = 0, message = "La cantidad física no puede ser negativa")         Integer quantity ) {
```

### backend/src/main/java/com/jaldishop/backend/inventory/web/dto/UpdateThresholdRequest.java

```java
6: @Min(value = 0, message = "El umbral de stock bajo no puede ser negativo")         Integer lowStockThreshold ) {
```

## media

### backend/src/main/java/com/jaldishop/backend/media/application/service/GenerateUploadSignatureService.java

```java
38: throw new ResourceNotFoundException("STORE_NOT_FOUND", "No se encontró una tienda asociada a este usuario.");         }
```

### backend/src/main/java/com/jaldishop/backend/media/infrastructure/cloudinary/CloudinaryMediaStorageService.java

```java
61: throw new RuntimeException("Error al eliminar recurso multimedia de Cloudinary: " + e.getMessage(), e);         }     }
```

### backend/src/main/java/com/jaldishop/backend/media/web/controller/MediaController.java

```java
51: throw new AccessDeniedException("No autenticado. Por favor inicia sesión nuevamente.");         }
58: throw new AccessDeniedException("Solo comerciantes o administradores pueden generar firmas de subida.");         }
```

### backend/src/main/java/com/jaldishop/backend/media/web/dto/GenerateSignatureRequest.java

```java
9: @NotNull(message = "El tipo de destino multimedia es obligatorio")         MediaTargetType targetType,         UUID storeId
```

## notification

### backend/src/main/java/com/jaldishop/backend/notification/application/service/MarkNotificationAsReadService.java

```java
32: throw new ResourceNotFoundException("Notification", notificationId);         }
```

### backend/src/main/java/com/jaldishop/backend/notification/domain/model/Notification.java

```java
41: throw new IllegalArgumentException("userId no puede ser nulo.");         }         if (type == null) {
44: throw new IllegalArgumentException("type no puede ser nulo.");         }         if (title == null || title.isBlank()) {
47: throw new IllegalArgumentException("title no puede ser nulo o vacío.");         }         if (message == null || message.isBlank()) {
50: throw new IllegalArgumentException("message no puede ser nulo o vacío.");         }         Instant now = Instant.now();
71: throw new BusinessRuleException("NOTIFICATION_ALREADY_READ", "La notificación ya fue marcada como leída.");         }         this.status = NotificationStatus.READ;
```

## ordering

### backend/src/main/java/com/jaldishop/backend/ordering/application/service/CancelCustomerOrderService.java

```java
40: throw new IllegalArgumentException("El identificador del cliente es obligatorio.");         }         if (orderNumber == null || orderNumber.isBlank()) {
43: throw new IllegalArgumentException("El número de pedido es obligatorio.");         }
50: throw new AccessDeniedException("No tiene permisos para cancelar un pedido ajeno.");         }
57: throw new AccessDeniedException("No tiene permisos para cancelar un pedido ajeno.");         }
65: throw new BusinessRuleException(                     "RN-ORD-09",                     "El pedido no se puede cancelar porque se encuentra en estado " + order.getStatus() +
```

### backend/src/main/java/com/jaldishop/backend/ordering/application/service/ChangeOrderStatusService.java

```java
44: throw new AccessDeniedException("El contexto de tienda (expectedStoreId) es obligatorio para autorizar la operación.");         }
51: throw new AccessDeniedException("No tiene permisos para modificar un pedido de otra tienda.");         }
```

### backend/src/main/java/com/jaldishop/backend/ordering/application/service/GetCustomerOrderDetailService.java

```java
29: throw new IllegalArgumentException("El identificador del cliente es obligatorio.");         }         if (orderNumber == null || orderNumber.isBlank()) {
32: throw new IllegalArgumentException("El número de pedido es obligatorio.");         }
```

### backend/src/main/java/com/jaldishop/backend/ordering/application/service/GetCustomerOrdersService.java

```java
31: throw new IllegalArgumentException("El identificador del cliente es obligatorio.");         }
```

### backend/src/main/java/com/jaldishop/backend/ordering/application/service/GetMerchantOrderDetailService.java

```java
24: throw new IllegalArgumentException("El identificador del pedido es obligatorio.");         }         if (storeId == null) {
27: throw new IllegalArgumentException("El identificador de la tienda es obligatorio.");         }
```

### backend/src/main/java/com/jaldishop/backend/ordering/application/service/ListMerchantOrdersService.java

```java
30: throw new IllegalArgumentException("El identificador de la tienda es obligatorio.");         }
```

### backend/src/main/java/com/jaldishop/backend/ordering/domain/model/Order.java

```java
105: throw new IllegalArgumentException("Los datos de la orden son obligatorios.");         }         if (orderNumber == null || orderNumber.isBlank()) {
108: throw new BusinessRuleException("RN-ORD-02", "El número de orden es obligatorio.");         }         if (data.userId() == null) {
111: throw new BusinessRuleException("RN-ORD-03", "El usuario es obligatorio para la orden.");         }         if (data.storeId() == null) {
114: throw new BusinessRuleException("RN-ORD-04", "La tienda es obligatoria para la orden.");         }         if (data.paymentId() == null) {
117: throw new BusinessRuleException(OrderErrorCodes.ORDER_PAYMENT_REQUIRED, "Una orden solo puede crearse con un pago asociado.");         }         if (data.capacityReservationId() == null) {
120: throw new BusinessRuleException("RN-CAP-01", "Una orden requiere una reserva de capacidad asociada.");         }         if (data.deliveryMode() == null) {
123: throw new BusinessRuleException("RN-DEL-01", "La modalidad de entrega es obligatoria.");         }         if (data.deliveryMode() == DeliveryMode.DELIVERY
127: throw new BusinessRuleException("RN-DEL-05", "La dirección de entrega es obligatoria para modalidad DELIVERY.");         }         if (data.items() == null || data.items().isEmpty()) {
130: throw new BusinessRuleException("RN-ORD-06", "Una orden debe contener al menos un ítem.");         }         validateServiceWindow(data.serviceStartTime(), data.serviceEndTime());
136: throw new BusinessRuleException("RN-ORD-08", "La moneda de la orden es obligatoria.");         }
168: throw new BusinessRuleException(                     "RN-ORD-07",                     String.format("Transición inválida de estado: no se puede pasar de %s a %s", this.status, newStatus)
174: throw new BusinessRuleException(                     "RN-DEL-04",                     "El estado OUT_FOR_DELIVERY solo está permitido para pedidos con modalidad DELIVERY"
190: throw new BusinessRuleException("RN-ORD-05", "La franja de servicio debe indicar inicio y fin juntos.");         }         if (!startTime.isBefore(endTime)) {
193: throw new BusinessRuleException("RN-ORD-05", "El inicio de la franja de servicio debe ser anterior al fin.");         }     }
204: throw new BusinessRuleException("RN-ORD-09", "Los montos de la orden no pueden ser negativos.");         }         if (discount.compareTo(subtotal) > 0) {
207: throw new BusinessRuleException("RN-ORD-10", "El descuento no puede superar el subtotal de productos.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/ordering/domain/model/OrderItem.java

```java
19: throw new IllegalArgumentException("El ID de la orden es obligatorio para crear el detalle.");         }         if (data == null) {
22: throw new IllegalArgumentException("Los datos del ítem son obligatorios.");         }         BigDecimal unitPrice = data.unitPriceAmount() != null ? data.unitPriceAmount() : BigDecimal.ZERO;
```

### backend/src/main/java/com/jaldishop/backend/ordering/domain/model/OrderStatusHistory.java

```java
20: throw new IllegalArgumentException("El ID de la orden es obligatorio para el historial de estado.");         }         if (status == null) {
23: throw new IllegalArgumentException("El estado es obligatorio para el historial.");         }         return new OrderStatusHistory(
```

### backend/src/main/java/com/jaldishop/backend/ordering/web/controller/CustomerOrderController.java

```java
78: throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden acceder a sus pedidos.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/ordering/web/controller/MerchantOrderController.java

```java
73: throw new IllegalArgumentException("El número de página no puede ser negativo");         }         if (size < 1 || size > 100) {
76: throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 100");         }         Pageable pageable = PageRequest.of(page, size);
118: throw new BadCredentialsException("Autenticación requerida");         }     }
```

### backend/src/main/java/com/jaldishop/backend/ordering/web/dto/CancelOrderRequest.java

```java
9: @Size(max = 255, message = "El motivo de cancelación no puede superar los 255 caracteres")         String reason ) {}
```

### backend/src/main/java/com/jaldishop/backend/ordering/web/dto/ChangeOrderStatusRequest.java

```java
10: @NotNull(message = "El nuevo estado es obligatorio")         OrderStatus status ) {}
```

### backend/src/main/java/com/jaldishop/backend/ordering/web/dto/CreateOrderRequest.java

```java
7: @ValidOrderDelivery public record CreateOrderRequest(    @NotNull(message = "La modalidad de entrega es obligatoria")
9: @NotNull(message = "La modalidad de entrega es obligatoria")    DeliveryMode deliveryMode,
```

## payment

### backend/src/main/java/com/jaldishop/backend/payment/application/gateway/MercadoPagoResourceValidator.java

```java
20: throw new SecurityException("Cuenta receptora ausente o no autorizada (collector_id=" + collectorId + ").");         }         if (properties.getExpectedLiveMode() != null && !properties.getExpectedLiveMode().equals(liveMode)) {
23: throw new SecurityException("Entorno remoto ausente o no autorizado (live_mode=" + liveMode + ").");         }     }
29: throw new ConflictException("ORDER_ID_MISMATCH", "Respuesta de orden ausente o contradictoria.");         }         if (order.payments() == null || order.payments().size() > 1) {
32: throw new ConflictException("AMBIGUOUS_ORDER_PAYMENTS", "Transacciones de orden inválidas o ambiguas.");         }         if (!order.payments().isEmpty() && (order.payments().getFirst() == null
37: throw new ConflictException("MISSING_REMOTE_PAYMENT_ID", "La transacción no tiene identidad remota.");         }         validateAccount(order.collectorId(), order.liveMode());
```

### backend/src/main/java/com/jaldishop/backend/payment/application/gateway/dto/CreateOrderCommand.java

```java
44: throw new IllegalArgumentException("externalReference no puede estar en blanco");         }         Objects.requireNonNull(totalAmount, "totalAmount no puede ser nulo");
48: throw new IllegalArgumentException("totalAmount debe ser mayor a cero");         }         if (items == null || items.isEmpty()) {
51: throw new IllegalArgumentException("Debe existir al menos un ítem en la orden");         }     }
67: throw new IllegalArgumentException("quantity debe ser mayor a cero");             }             if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
70: throw new IllegalArgumentException("unitPrice debe ser mayor a cero");             }         }
```

### backend/src/main/java/com/jaldishop/backend/payment/application/gateway/dto/CreatePreferenceCommand.java

```java
41: throw new IllegalArgumentException("externalReference no puede estar en blanco");         }         if (items == null || items.isEmpty()) {
44: throw new IllegalArgumentException("Debe existir al menos un ítem en la preferencia");         }     }
60: throw new IllegalArgumentException("quantity debe ser mayor a cero");             }             if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
63: throw new IllegalArgumentException("unitPrice debe ser mayor a cero");             }         }
```

### backend/src/main/java/com/jaldishop/backend/payment/application/gateway/dto/OrderQueryResult.java

```java
35: throw new IllegalArgumentException("orderId no puede estar en blanco");         }         if (payments == null) {
```

### backend/src/main/java/com/jaldishop/backend/payment/application/gateway/dto/OrderResult.java

```java
19: throw new IllegalArgumentException("orderId no puede estar en blanco");         }         if (checkoutUrl.isBlank()) {
22: throw new IllegalArgumentException("checkoutUrl no puede estar en blanco");         }     }
```

### backend/src/main/java/com/jaldishop/backend/payment/application/gateway/dto/PaymentQueryResult.java

```java
41: throw new IllegalArgumentException("providerPaymentId no puede estar en blanco");         }         if (status.isBlank()) {
44: throw new IllegalArgumentException("status no puede estar en blanco");         }     }
```

### backend/src/main/java/com/jaldishop/backend/payment/application/gateway/dto/PreferenceResult.java

```java
17: throw new IllegalArgumentException("preferenceId no puede estar en blanco");         }         if (initPoint.isBlank()) {
20: throw new IllegalArgumentException("initPoint no puede estar en blanco");         }     }
```

### backend/src/main/java/com/jaldishop/backend/payment/application/preference/CreatePaymentPreferenceService.java

```java
226: throw new ConflictException("RESERVATION_EXPIRED", "La reserva de capacidad se encuentra expirada.");         }
231: throw new ConflictException(                     "PAYMENT_PROTECTION_EXPIRING",                     "El tiempo restante de la reserva (" + remaining.toSeconds() + "s) es insuficiente para completar el pago de forma segura."
```

### backend/src/main/java/com/jaldishop/backend/payment/application/preference/PaymentPreferencePreparationService.java

```java
134: throw new ConflictException("RESERVATION_FORBIDDEN", "La reserva no pertenece al usuario autenticado.");         }
138: throw new ConflictException("RESERVATION_COMMITTED", "La reserva ya ha sido confirmada en un pedido previo.");         }         if (reservation.getStatus() == CapacityReservationStatus.RELEASED) {
141: throw new ConflictException("RESERVATION_RELEASED", "La reserva de capacidad fue cancelada.");         }         if (reservation.getStatus() == CapacityReservationStatus.EXPIRED || reservation.isExpired(now)) {
144: throw new ConflictException("RESERVATION_EXPIRED", "La reserva de capacidad se encuentra expirada.");         }
152: throw new ConflictException("INVENTORY_RESERVATION_EXPIRED", "La reserva de inventario para los productos ha expirado.");                 }                 if (ir.getStatus() == InventoryReservationStatus.RELEASED) {
155: throw new ConflictException("INVENTORY_RESERVATION_RELEASED", "La reserva de inventario fue liberada.");                 }             }
164: throw new ConflictException("STORE_INACTIVE", "La tienda no se encuentra disponible actualmente.");         }
176: throw new ConflictException("PAYMENT_ALREADY_APPROVED", "El pago para esta reserva ya fue aprobado.");             }
187: throw new ConflictException("PAYMENT_IN_PROGRESS", "La creación remota requiere revisión.");                 }             }
215: throw new ConflictException("PAYMENT_IN_PROGRESS",                             "Ya existe una solicitud de pago o creación de preferencia en curso para esta reserva.");                 }
227: throw new ConflictException("PAYMENT_IN_PROGRESS",                             "El resultado de la solicitud de pago es incierto y requiere revisión.");                 }
260: throw new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío.");         }
273: throw new IllegalStateException("Error al serializar el snapshot de ítems de checkout", e);         }
349: throw new ConflictException("PAYMENT_ATTEMPT_MISMATCH", "El intento no pertenece al pago.");         }         return attempt;
```

### backend/src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentIncidenceRecorder.java

```java
53: throw new IllegalStateException("Pago inexistente al registrar incidencia");         }
57: throw new IllegalStateException("La incidencia requiere un pago aprobado");         // Payment lock serializes duplicate notifications; refund and incidence commit atomically.         if (payment.getRefundStatus() != RefundStatus.NOT_REQUIRED) {
```

### backend/src/main/java/com/jaldishop/backend/payment/application/reconciliation/PaymentOrderReconciliationProcessor.java

```java
202: throw new ConflictException("PAYMENT_ATTEMPT_MISMATCH", "IDENTIFIER_CONTRADICTION");         persistenceService.validateReference(remoteOrder.externalReference(), payment.getId(), attempt);
```

### backend/src/main/java/com/jaldishop/backend/payment/application/webhook/PaymentWebhookPersistenceService.java

```java
72: throw new ConflictException("PAYMENT_ATTEMPT_MISMATCH", "REFERENCE_MISMATCH");     }
94: throw new ConflictException("PAYMENT_AMOUNT_MISMATCH",                     String.format("El monto notificado (%s) no coincide con el monto esperado (%s)",                             queryResult.transactionAmount(), payment.getAmount()));
102: throw new ConflictException("PAYMENT_CURRENCY_MISMATCH",                     String.format("La moneda notificada (%s) no coincide con la moneda esperada (%s)",                             queryResult.currency(), payment.getCurrency()));
329: if (incidenceRecorder == null) throw new IllegalStateException("Registrador de incidencias no disponible");         return incidenceRecorder.recordCommercialOrIndeterminateConflict(paymentId, conflict);     }
```

### backend/src/main/java/com/jaldishop/backend/payment/application/webhook/ProcessMercadoPagoWebhookService.java

```java
118: throw new IllegalArgumentException("Identificadores de evento inconsistentes entre parámetros de consulta y cuerpo JSON.");             }         }
125: throw new IllegalArgumentException("Identificador de evento no proporcionado en la notificación (falta data.id o id).");         }
138: throw new SecurityException("Firma criptográfica x-signature inválida o encabezados requeridos ausentes.");         }
178: throw new ConflictException("PAYMENT_ID_MISMATCH", "La respuesta corresponde a otro pago remoto.");          // H-04: Verificación de Cuenta Receptora (collector_id) si está configurada
248: throw new ConflictException("MISSING_EXTERNAL_REFERENCE",                     "El recurso consultado no posee external_reference válida vinculada a un pago local.");         }
```

### backend/src/main/java/com/jaldishop/backend/payment/domain/model/Payment.java

```java
46: throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");         }         if (!"PEN".equals(pricing.currency())) {
49: throw new IllegalArgumentException("La moneda permitida es estrictamente PEN");         }
233: throw new IllegalArgumentException("El monto a reembolsar debe ser mayor a cero");         }         if (amountToRefund.compareTo(getAmount()) > 0) {
236: throw new IllegalArgumentException("El monto a reembolsar no puede superar el monto original pagado");         }         this.refundStatus = RefundStatus.PENDING;
```

### backend/src/main/java/com/jaldishop/backend/payment/domain/model/PaymentAttempt.java

```java
55: throw new IllegalArgumentException("attemptNumber debe ser mayor a cero");         }
269: throw new IllegalArgumentException("providerOrderId no puede estar en blanco");         }         if (this.providerOrderId != null && !this.providerOrderId.equals(providerOrderId)) {
272: throw new IllegalStateException("Conflicting remote order identity");         }         if ("ORDERS".equals(checkoutMode)) {
276: throw new IllegalArgumentException("Verified creation requires checkout URL");             }             if (remoteOrderCreationState == RemoteOrderCreationState.REJECTED_CONFIRMED) {
279: throw new IllegalStateException("Creation already rejected");             }             remoteOrderCreationState = RemoteOrderCreationState.CREATED;
298: throw new IllegalStateException("Orders creation evidence only");         }         if (remoteOrderCreationState == RemoteOrderCreationState.CREATED
```

### backend/src/main/java/com/jaldishop/backend/payment/domain/model/PaymentRefundIntent.java

```java
72: throw new IllegalArgumentException("El monto a reembolsar debe ser mayor a cero");         }
```

### backend/src/main/java/com/jaldishop/backend/payment/infrastructure/client/mercadopago/CheckoutCreationRequestStore.java

```java
24: throw new IllegalArgumentException("Contexto de solicitud de checkout inválido");         }         int inserted = jdbc.update("""
33: throw new ConflictException("PAYMENT_IN_PROGRESS", "El intento requiere revisión antes de continuar.");         }         if ("ORDERS".equals(mode)) {
41: throw new ConflictException("PAYMENT_IN_PROGRESS", "El intento requiere revisión antes de continuar.");             }         }
```

### backend/src/main/java/com/jaldishop/backend/payment/infrastructure/client/mercadopago/MercadoPagoRestClientAdapter.java

```java
124: throw new MercadoPagoException(                         200,                         "MP_INVALID_RESPONSE",
142: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);         } catch (MercadoPagoException ex) {             throw ex;
148: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);             }             log.error("Error inesperado al comunicarse con Mercado Pago: {}", ex.getMessage());
151: throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar Mercado Pago", ex);         }     }
160: throw new IllegalArgumentException("providerPaymentId no puede ser nulo ni estar vacío");         }
174: throw new MercadoPagoException(                         200,                         "MP_INVALID_RESPONSE",
207: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);         } catch (MercadoPagoException ex) {             throw ex;
212: throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar Mercado Pago", ex);         }     }
249: throw new MercadoPagoException(                         200,                         "MP_INVALID_RESPONSE",
276: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar reembolso en Mercado Pago", ex);         } catch (MercadoPagoException ex) {             throw ex;
282: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar reembolso en Mercado Pago", ex);             }             log.error("Error inesperado al ejecutar reembolso en Mercado Pago: {}", ex.getMessage());
285: throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar reembolso en Mercado Pago", ex);         }     }
312: throw new MercadoPagoException(                         200,                         "MP_INVALID_RESPONSE",
366: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar reembolso de orden en Mercado Pago", ex);         } catch (MercadoPagoException ex) {             throw ex;
372: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar reembolso de orden en Mercado Pago", ex);             }             log.error("Error inesperado al ejecutar reembolso de orden en Mercado Pago: {}", ex.getMessage());
375: throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar reembolso de orden en Mercado Pago", ex);         }     }
419: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al consultar reembolsos en Mercado Pago", ex);         } catch (MercadoPagoException ex) {             throw ex;
424: throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al consultar reembolsos en Mercado Pago", ex);         }     }
455: throw new MercadoPagoException(                         200,                         "MP_INVALID_RESPONSE",
468: throw new MercadoPagoException(400, OrderCreationRejection.INVALID_TOTAL_AMOUNT,                         "Order creation rejected: invalid_total_amount");             }
478: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);         } catch (MercadoPagoException ex) {             throw ex;
484: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);             }             log.error("Error inesperado al comunicarse con Mercado Pago (crear orden): {}", ex.getMessage());
487: throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al invocar Mercado Pago", ex);         }     }
496: throw new IllegalArgumentException("orderId no puede ser nulo ni estar vacío");         }
510: throw new MercadoPagoException(                         200,                         "MP_INVALID_RESPONSE",
573: throw new MercadoPagoException(0, "MP_TIMEOUT", "Timeout o falla de red al invocar Mercado Pago", ex);         } catch (MercadoPagoException ex) {             throw ex;
578: throw new MercadoPagoException(500, "MP_UNEXPECTED_ERROR", "Error inesperado al consultar orden en Mercado Pago", ex);         }     }
584: throw new MercadoPagoException(                     401,                     "MP_MISSING_CREDENTIALS",
598: throw new MercadoPagoException(400, "MP_BAD_REQUEST", "Solicitud rechazada por Mercado Pago: " + sanitizeLog(responseBody), ex);         } else if (status == 401) {             throw new MercadoPagoException(401, "MP_UNAUTHORIZED", "Credenciales inválidas o no autorizadas en Mercado Pago", ex);
600: throw new MercadoPagoException(401, "MP_UNAUTHORIZED", "Credenciales inválidas o no autorizadas en Mercado Pago", ex);         } else if (status == 404) {             throw new MercadoPagoException(404, "MP_NOT_FOUND", "Recurso no encontrado en Mercado Pago", ex);
602: throw new MercadoPagoException(404, "MP_NOT_FOUND", "Recurso no encontrado en Mercado Pago", ex);         } else if (status == 429) {             throw new MercadoPagoException(429, "MP_RATE_LIMIT", "Límite de solicitudes alcanzado en Mercado Pago", ex);
604: throw new MercadoPagoException(429, "MP_RATE_LIMIT", "Límite de solicitudes alcanzado en Mercado Pago", ex);         } else {             throw new MercadoPagoException(status, "MP_CLIENT_ERROR", "Error de cliente devuelto por Mercado Pago: " + status, ex);
606: throw new MercadoPagoException(status, "MP_CLIENT_ERROR", "Error de cliente devuelto por Mercado Pago: " + status, ex);         }     }
613: throw new MercadoPagoException(status, "MP_SERVER_ERROR", "Error de servidor en Mercado Pago: HTTP " + status, ex);     }
```

### backend/src/main/java/com/jaldishop/backend/payment/web/dto/CreatePaymentPreferenceRequest.java

```java
12: @NotNull(message = "El ID de la reserva de capacidad es obligatorio")         UUID capacityReservationId,
15: @NotNull(message = "El tipo de entrega es obligatorio")         CheckoutFulfillmentType fulfillmentType,
18: @NotBlank(message = "El nombre del cliente es obligatorio")         String customerName,
21: @NotBlank(message = "El teléfono del cliente es obligatorio")         String customerPhone,
24: @NotBlank(message = "El email del cliente es obligatorio")         @Email(message = "El email debe ser válido")         String customerEmail,
25: @Email(message = "El email debe ser válido")         String customerEmail,
```

## review

### backend/src/main/java/com/jaldishop/backend/review/application/service/CreateProductReviewService.java

```java
50: throw new ConflictException("Ya has enviado una reseña para este producto. Si deseas cambiarla, actualízala.");         }
55: throw new BusinessRuleException("Solo puedes calificar productos que hayas adquirido en un pedido completado.");         }
```

### backend/src/main/java/com/jaldishop/backend/review/application/service/GetProductPublicReviewsService.java

```java
43: throw new ResourceNotFoundException("Producto", productId);         }
```

### backend/src/main/java/com/jaldishop/backend/review/domain/model/Review.java

```java
55: throw new IllegalArgumentException("La calificación debe estar entre 1 y 5 estrellas.");         }         this.rating = rating;
```

### backend/src/main/java/com/jaldishop/backend/review/web/dto/CreateReviewRequest.java

```java
9: @NotNull(message = "La calificación es obligatoria.")         @Min(value = 1, message = "La calificación mínima es 1 estrella.")         @Max(value = 5, message = "La calificación máxima es 5 estrellas.")
10: @Min(value = 1, message = "La calificación mínima es 1 estrella.")         @Max(value = 5, message = "La calificación máxima es 5 estrellas.")         Integer rating,
11: @Max(value = 5, message = "La calificación máxima es 5 estrellas.")         Integer rating,
14: @Size(max = 1000, message = "El comentario no puede exceder los 1000 caracteres.")         String comment ) {}
```

### backend/src/main/java/com/jaldishop/backend/review/web/dto/UpdateReviewRequest.java

```java
9: @NotNull(message = "La calificación es obligatoria.")         @Min(value = 1, message = "La calificación mínima es 1 estrella.")         @Max(value = 5, message = "La calificación máxima es 5 estrellas.")
10: @Min(value = 1, message = "La calificación mínima es 1 estrella.")         @Max(value = 5, message = "La calificación máxima es 5 estrellas.")         Integer rating,
11: @Max(value = 5, message = "La calificación máxima es 5 estrellas.")         Integer rating,
14: @Size(max = 1000, message = "El comentario no puede exceder los 1000 caracteres.")         String comment ) {}
```

## shared

### backend/src/main/java/com/jaldishop/backend/shared/domain/model/DeliveryAddressSnapshot.java

```java
15: throw new IllegalArgumentException("Latitud y longitud deben especificarse conjuntamente.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/shared/domain/model/Money.java

```java
92: throw new IllegalArgumentException(String.format(                     "No se pueden operar importes con distintas divisas: %s vs %s",                     this.currency, other.currency
```

## store

### backend/src/main/java/com/jaldishop/backend/store/application/command/CloseMyStoreCommand.java

```java
13: throw new IllegalArgumentException("El ID del comerciante no puede ser nulo.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/store/application/service/ChangeStoreStatusService.java

```java
31: throw new IllegalArgumentException("Estado no soportado para cambio de estado de tienda: " + command.targetStatus());         }
```

### backend/src/main/java/com/jaldishop/backend/store/application/service/CreateStoreService.java

```java
31: throw new ConflictException("MERCHANT_ALREADY_HAS_STORE", "El comerciante ya tiene una tienda registrada.");         }
36: throw new ConflictException("STORE_SLUG_ALREADY_EXISTS", "El slug de la tienda ya está en uso");         }
70: throw new IllegalArgumentException("El slug no puede ser nulo ni vacio.");         }         String decomposed = Normalizer.normalize(source.trim(), Normalizer.Form.NFD);
78: throw new IllegalArgumentException("El slug no puede ser nulo ni vacio.");         }         return resolved;
```

### backend/src/main/java/com/jaldishop/backend/store/application/service/GetStoreBySlugService.java

```java
24: throw new IllegalArgumentException("El slug de la tienda no puede ser nulo ni vacío.");         }
32: throw new ResourceNotFoundException("La tienda no se encuentra activa o disponible.");         }
```

### backend/src/main/java/com/jaldishop/backend/store/application/service/GetStoreCustomersService.java

```java
23: throw new IllegalArgumentException("El ID de la tienda no puede ser nulo.");         }         return storeCustomerRepository.findCustomersByStoreId(storeId, query);
```

### backend/src/main/java/com/jaldishop/backend/store/application/service/StoreContextService.java

```java
35: throw new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación.");         }     }
42: throw new AccessDeniedException("No tiene permisos para operar sobre una tienda ajena.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/store/domain/model/DeliveryConfiguration.java

```java
22: throw new IllegalArgumentException("La tarifa de entrega no puede ser negativa.");         }         if (taxRate != null) {
26: throw new IllegalArgumentException("La tasa de impuestos debe ser mayor o igual a 0 y menor o igual a 100.");             }         }
```

### backend/src/main/java/com/jaldishop/backend/store/domain/model/Store.java

```java
163: throw new IllegalArgumentException("El ID del comerciante no puede ser nulo.");         }
167: throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio.");         }
171: throw new IllegalArgumentException("El slug no puede ser nulo ni vacio.");         }
492: throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio.");         }
540: throw new ConflictException("STORE_ALREADY_ACTIVE", "La tienda ya se encuentra activa.");         }         this.status = StoreStatus.ACTIVE;
548: throw new ConflictException("STORE_ALREADY_SUSPENDED", "La tienda ya se encuentra suspendida.");         }         this.status = StoreStatus.SUSPENDED;
```

### backend/src/main/java/com/jaldishop/backend/store/domain/model/StoreCategory.java

```java
21: throw new IllegalArgumentException("El ID no puede ser nulo.");         }         if (name == null || name.isBlank()) {
24: throw new IllegalArgumentException("El nombre no puede ser nulo ni vacío.");         }         if (slug == null || slug.isBlank()) {
27: throw new IllegalArgumentException("El slug no puede ser nulo ni vacío.");         }         this.id = id;
53: throw new IllegalArgumentException("El nombre no puede ser nulo ni vacío.");         }         this.name = name.trim();
```

### backend/src/main/java/com/jaldishop/backend/store/domain/model/StoreLocation.java

```java
19: throw new IllegalArgumentException("Latitud y longitud deben ser ambos nulos o ambos especificados.");         }     }
```

### backend/src/main/java/com/jaldishop/backend/store/web/dto/CloseStoreRequest.java

```java
6: @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")         String reason ) {
```

### backend/src/main/java/com/jaldishop/backend/store/web/dto/CreateStoreRequest.java

```java
12: @NotBlank(message = "El nombre es obligatorio") @Size(max = 160) String name,         @Size(max = 180) String slug,         String description,
13: @Size(max = 180) String slug,         String description,         @ValidPhone String contactPhone,
15: @ValidPhone String contactPhone,         String address,         String addressReference,
18: @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,         @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,         boolean pickupEnabled,
19: @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,         boolean pickupEnabled,         boolean deliveryEnabled,
22: @DecimalMin("0.00") BigDecimal deliveryFeeAmount,         @Size(min = 3, max = 3) String deliveryFeeCurrency,         @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,
23: @Size(min = 3, max = 3) String deliveryFeeCurrency,         @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,         String logoUrl,
24: @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,         String logoUrl,         String bannerUrl,
29: @ValidPhone String whatsappNumber,         java.util.Set<java.util.UUID> categoryIds ) {
```

### backend/src/main/java/com/jaldishop/backend/store/web/dto/UpdateStoreRequest.java

```java
12: @NotBlank(message = "El nombre es obligatorio") @Size(max = 160) String name,         String description,         @ValidPhone String contactPhone,
14: @ValidPhone String contactPhone,         String address,         String addressReference,
17: @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,         @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,         boolean pickupEnabled,
18: @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,         boolean pickupEnabled,         boolean deliveryEnabled,
21: @DecimalMin("0.00") BigDecimal deliveryFeeAmount,         @Size(min = 3, max = 3) String deliveryFeeCurrency,         @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,
22: @Size(min = 3, max = 3) String deliveryFeeCurrency,         @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,         String logoUrl,
23: @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,         String logoUrl,         String bannerUrl,
28: @ValidPhone String whatsappNumber,         java.util.Set<java.util.UUID> categoryIds ) {
```

## Restricciones SQL (migraciones; aplicación en bases NO verificada)

### V10__fix_user_roles_id_default_and_store_sync.sql

```sql
24: IF v_merchant_role_id IS NOT NULL THEN
40: CREATE TRIGGER trg_sync_merchant_user_role_store
```

### V11__refine_store_ownership.sql

```sql
6: --   2. Rename associated UNIQUE constraint and Foreign Key.
14: -- 2. Rename UNIQUE constraint
17: -- 3. Rename Foreign Key
```

### V12__migrate_store_customers_to_user_roles_and_check_scope.sql

```sql
2: -- JaldiShop Migration V12: Migrate Store Customers to User Roles & Check Role Scope
3: -- Version: V12__migrate_store_customers_to_user_roles_and_check_scope.sql
8: --   4. Validate that no MERCHANT (role_id=2) has store_id NULL and no ADMIN (role_id=3) has store_id NOT NULL.
9: --   5. Add CHECK constraint on user_roles ensuring:
10: --        - CUSTOMER (role_id=1) can be global (store_id NULL) or contextual (store_id NOT NULL).
11: --        - MERCHANT (role_id=2) must be contextual (store_id NOT NULL).
46: -- 3. Strict Pre-Validation before applying CHECK constraint:
47: -- CUSTOMER (role_id = 1) can have store_id NULL (global) or store_id NOT NULL (contextual) -> both valid.
48: -- MERCHANT (role_id = 2) must have store_id NOT NULL.
56: SELECT COUNT(*) INTO v_invalid_admins       FROM user_roles WHERE role_id = 3 AND store_id IS NOT NULL;
70: CHECK (
72: OR (role_id = 2 AND store_id IS NOT NULL)
```

### V13__strengthen_inventory_reservations_multitenancy.sql

```sql
7: --   3. Apply NOT NULL on inventory_reservations.store_id.
51: -- 5. Enforce NOT NULL on store_id
52: ALTER TABLE inventory_reservations ALTER COLUMN store_id SET NOT NULL;
56: ADD CONSTRAINT uq_capacity_reservations_store_id UNIQUE (store_id, id);
64: FOREIGN KEY (store_id, capacity_reservation_id)
65: REFERENCES capacity_reservations(store_id, id) ON DELETE CASCADE;
69: FOREIGN KEY (store_id, variant_id)
70: REFERENCES product_variants(store_id, id) ON DELETE RESTRICT;
```

### V14__prevent_capacity_slot_overlaps.sql

```sql
11: --   5. Add EXCLUDE USING gist constraints preventing overlapping ACTIVE slots.
24: ELSIF p_start_time IS NOT NULL AND p_end_time IS NOT NULL THEN
72: -- 4. General Pre-Validation: Detect any remaining overlaps among ACTIVE slots before applying EXCLUDE
105: -- 5. Exclude constraint for capacity_configurations (store_id, day_of_week, time_range)
108: EXCLUDE USING gist (
115: -- 6. Exclude constraint for capacity_exceptions (store_id, service_date, time_range)
118: EXCLUDE USING gist (
```

### V15__create_customer_addresses_table.sql

```sql
7: id UUID NOT NULL,
8: user_id UUID NOT NULL,
9: alias VARCHAR(50) NOT NULL,
10: recipient_name VARCHAR(200) NOT NULL,
11: recipient_phone VARCHAR(30) NOT NULL,
12: street_address VARCHAR(255) NOT NULL,
13: department VARCHAR(100) NOT NULL,
14: province VARCHAR(100) NOT NULL,
15: district VARCHAR(100) NOT NULL,
16: ubigeo_code CHAR(6) NOT NULL,
20: is_default BOOLEAN NOT NULL DEFAULT FALSE,
21: created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
22: updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
25: CONSTRAINT fk_customer_addresses_user_id FOREIGN KEY (user_id)
26: REFERENCES users(id) ON DELETE CASCADE,
27: CONSTRAINT ck_customer_addresses_lat CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
28: CONSTRAINT ck_customer_addresses_lon CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
29: CONSTRAINT ck_customer_addresses_ubigeo CHECK (ubigeo_code ~ '^[0-9]{6}$')
35: CREATE UNIQUE INDEX uq_customer_addresses_user_default
```

### V16__add_deduplication_key_to_notifications.sql

```sql
10: ADD CONSTRAINT uq_notifications_deduplication_key UNIQUE (deduplication_key);
```

### V17__expand_payment_persistence_and_attempts.sql

```sql
2: -- Description: Preparar la persistencia de payments y payment_attempts para Checkout Pro (Mercado Pago).
3: -- 1. Ampliar restricción de métodos de pago en payment_attempts para soportar billeteras, transferencias y pasarelas de Checkout Pro.
16: CHECK (payment_method IN ('CARD', 'ACCOUNT_MONEY', 'WALLET_PURCHASE', 'TICKET', 'BANK_TRANSFER', 'YAPE', 'OTHER'));
18: -- Identificadores de sesión de Checkout Pro generados por Mercado Pago
24: WHERE preference_id IS NOT NULL;
27: -- 2. PAYMENTS: DESPACHO, SNAPSHOT DE PRECIOS Y PRODUCTOS PARA CHECKOUT ASÍNCRONO
42: CHECK (fulfillment_type IS NULL OR fulfillment_type IN ('PICKUP', 'DELIVERY'));
45: CHECK (delivery_latitude IS NULL OR (delivery_latitude >= -90 AND delivery_latitude <= 90));
48: CHECK (delivery_longitude IS NULL OR (delivery_longitude >= -180 AND delivery_longitude <= 180));
58: CHECK (products_subtotal_amount IS NULL OR products_subtotal_amount >= 0);
61: CHECK (discount_amount IS NULL OR discount_amount >= 0);
64: CHECK (delivery_fee_amount IS NULL OR delivery_fee_amount >= 0);
67: CHECK (included_tax_amount IS NULL OR included_tax_amount >= 0);
69: -- Snapshot de ítems congelados al momento de iniciar checkout (JSONB para no depender del carrito mutable)
```

### V18__create_payment_incidences_table.sql

```sql
8: id UUID NOT NULL,
9: payment_id UUID NOT NULL,
10: process_type VARCHAR(30) NOT NULL,
11: category VARCHAR(30) NOT NULL,
12: code VARCHAR(80) NOT NULL,
13: message TEXT NOT NULL,
14: attempts_count INT NOT NULL DEFAULT 1,
15: max_attempts INT NOT NULL DEFAULT 5,
17: status VARCHAR(30) NOT NULL,
18: created_at TIMESTAMPTZ NOT NULL,
19: updated_at TIMESTAMPTZ NOT NULL,
22: CONSTRAINT fk_payment_incidences_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
23: CONSTRAINT ck_payment_incidences_process_type CHECK (process_type IN ('ORDER_CONFIRMATION', 'REFUND_EXECUTION')),
24: CONSTRAINT ck_payment_incidences_category CHECK (category IN ('COMMERCIAL', 'TECHNICAL', 'OPERATIONAL')),
25: CONSTRAINT ck_payment_incidences_status CHECK (status IN ('OPEN', 'RESOLVED', 'MANUAL_REVIEW_REQUIRED')),
26: CONSTRAINT ck_payment_incidences_attempts CHECK (attempts_count >= 1 AND attempts_count <= max_attempts + 1)
32: CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_incidences_active
```

### V19__create_payment_refunds_schema.sql

```sql
16: id UUID NOT NULL,
17: payment_id UUID NOT NULL,
18: provider_payment_id VARCHAR(100) NOT NULL,
19: amount NUMERIC(12,2) NOT NULL,
20: currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
21: idempotency_key VARCHAR(100) NOT NULL,
22: status VARCHAR(30) NOT NULL,
27: created_at TIMESTAMPTZ NOT NULL,
28: updated_at TIMESTAMPTZ NOT NULL,
31: CONSTRAINT fk_payment_refund_intents_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE RESTRICT,
32: CONSTRAINT uq_payment_refund_intents_payment_id UNIQUE (payment_id),
33: CONSTRAINT uq_payment_refund_intents_idempotency_key UNIQUE (idempotency_key),
34: CONSTRAINT ck_payment_refund_intents_amount CHECK (amount > 0),
35: CONSTRAINT ck_payment_refund_intents_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'UNKNOWN'))
51: id UUID NOT NULL,
52: refund_intent_id UUID NOT NULL,
53: attempt_number INT NOT NULL,
54: request_method VARCHAR(10) NOT NULL,
55: status VARCHAR(30) NOT NULL,
60: executed_at TIMESTAMPTZ NOT NULL,
62: CONSTRAINT fk_payment_refund_execution_refund_intent FOREIGN KEY (refund_intent_id) REFERENCES payment_refund_intents(id) ON DELETE CASCADE,
63: CONSTRAINT ck_payment_refund_execution_method CHECK (request_method IN ('POST', 'GET')),
64: CONSTRAINT ck_payment_refund_execution_status CHECK (status IN ('SUCCESSFUL', 'FAILED', 'UNKNOWN'))
```

### V1__initial_schema.sql

```sql
12: id UUID NOT NULL,
13: email VARCHAR(254) NOT NULL,
14: password_encoded VARCHAR(255) NOT NULL,
15: first_name VARCHAR(100) NOT NULL,
16: last_name VARCHAR(100) NOT NULL,
18: status VARCHAR(30) NOT NULL,
19: created_at TIMESTAMPTZ NOT NULL,
20: updated_at TIMESTAMPTZ NOT NULL,
22: CONSTRAINT uq_users_email UNIQUE (email),
23: CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
32: id SMALLINT NOT NULL,
33: name VARCHAR(20) NOT NULL,
35: CONSTRAINT uq_roles_name UNIQUE (name),
36: CONSTRAINT ck_roles_name CHECK (name IN ('CUSTOMER', 'MERCHANT', 'ADMIN'))
43: user_id UUID NOT NULL,
44: role_id SMALLINT NOT NULL,
46: CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
47: CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
56: id UUID NOT NULL,
57: merchant_user_id UUID NOT NULL,
58: name VARCHAR(160) NOT NULL,
59: slug VARCHAR(180) NOT NULL,
66: pickup_enabled BOOLEAN NOT NULL,
67: delivery_enabled BOOLEAN NOT NULL,
70: tax_applies BOOLEAN NOT NULL,
72: status VARCHAR(30) NOT NULL,
73: created_at TIMESTAMPTZ NOT NULL,
74: updated_at TIMESTAMPTZ NOT NULL,
76: CONSTRAINT uq_stores_merchant_user_id UNIQUE (merchant_user_id),
77: CONSTRAINT uq_stores_slug UNIQUE (slug),
78: CONSTRAINT fk_stores_merchant_user_id FOREIGN KEY (merchant_user_id) REFERENCES users(id) ON DELETE RESTRICT,
79: CONSTRAINT ck_stores_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED')),
80: CONSTRAINT ck_stores_latitude CHECK (latitude BETWEEN -90 AND 90),
81: CONSTRAINT ck_stores_longitude CHECK (longitude BETWEEN -180 AND 180),
82: CONSTRAINT ck_stores_delivery_fee CHECK (delivery_fee_amount IS NULL OR delivery_fee_amount >= 0),
83: CONSTRAINT ck_stores_tax_rate CHECK (tax_rate IS NULL OR (tax_rate > 0 AND tax_rate <= 100))
90: id UUID NOT NULL,
91: store_id UUID NOT NULL,
92: name VARCHAR(120) NOT NULL,
94: status VARCHAR(30) NOT NULL,
95: created_at TIMESTAMPTZ NOT NULL,
96: updated_at TIMESTAMPTZ NOT NULL,
98: CONSTRAINT fk_categories_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
99: CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
103: CREATE UNIQUE INDEX uq_idx_categories_store_name ON categories (store_id, lower(trim(name))) WHERE status = 'ACTIVE';
109: id UUID NOT NULL,
110: store_id UUID NOT NULL,
111: category_id UUID NOT NULL,
112: name VARCHAR(160) NOT NULL,
113: slug VARCHAR(180) NOT NULL,
116: status VARCHAR(30) NOT NULL,
117: created_at TIMESTAMPTZ NOT NULL,
118: updated_at TIMESTAMPTZ NOT NULL,
120: CONSTRAINT fk_products_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
121: CONSTRAINT fk_products_category_id FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
122: CONSTRAINT uq_products_store_slug UNIQUE (store_id, slug),
123: CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
133: id UUID NOT NULL,
134: product_id UUID NOT NULL,
135: presentation_name VARCHAR(120) NOT NULL,
137: price_amount NUMERIC(12,2) NOT NULL,
138: price_currency CHAR(3) NOT NULL,
139: tracks_inventory BOOLEAN NOT NULL,
140: status VARCHAR(30) NOT NULL,
141: created_at TIMESTAMPTZ NOT NULL,
142: updated_at TIMESTAMPTZ NOT NULL,
144: CONSTRAINT fk_product_variants_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
145: CONSTRAINT ck_product_variants_price CHECK (price_amount > 0),
146: CONSTRAINT ck_product_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
150: CREATE UNIQUE INDEX uq_idx_product_variants_sku ON product_variants (sku) WHERE sku IS NOT NULL;
156: variant_id UUID NOT NULL,
157: name VARCHAR(120) NOT NULL,
158: value TEXT NOT NULL,
160: CONSTRAINT fk_variant_attributes_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
167: variant_id UUID NOT NULL,
168: quantity INTEGER NOT NULL,
170: updated_at TIMESTAMPTZ NOT NULL,
172: CONSTRAINT fk_inventories_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
173: CONSTRAINT ck_inventories_quantity CHECK (quantity >= 0),
174: CONSTRAINT ck_inventories_low_stock CHECK (low_stock_threshold IS NULL OR low_stock_threshold >= 0)
181: id UUID NOT NULL,
182: user_id UUID NOT NULL,
183: store_id UUID NOT NULL,
184: created_at TIMESTAMPTZ NOT NULL,
185: updated_at TIMESTAMPTZ NOT NULL,
187: CONSTRAINT fk_carts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
188: CONSTRAINT fk_carts_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
189: CONSTRAINT uq_carts_user_store UNIQUE (user_id, store_id)
196: cart_id UUID NOT NULL,
197: variant_id UUID NOT NULL,
198: quantity INTEGER NOT NULL,
199: reference_price_amount NUMERIC(12,2) NOT NULL,
200: reference_price_currency CHAR(3) NOT NULL,
201: created_at TIMESTAMPTZ NOT NULL,
202: updated_at TIMESTAMPTZ NOT NULL,
204: CONSTRAINT fk_cart_items_cart_id FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
205: CONSTRAINT fk_cart_items_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
206: CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0),
207: CONSTRAINT ck_cart_items_reference_price CHECK (reference_price_amount >= 0)
214: id UUID NOT NULL,
215: store_id UUID NOT NULL,
216: name VARCHAR(160) NOT NULL,
217: type VARCHAR(30) NOT NULL,
218: modality VARCHAR(30) NOT NULL,
219: value NUMERIC(12,2) NOT NULL,
224: status VARCHAR(30) NOT NULL,
225: created_at TIMESTAMPTZ NOT NULL,
226: updated_at TIMESTAMPTZ NOT NULL,
228: CONSTRAINT fk_discounts_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
230: CONSTRAINT ck_discounts_type CHECK (type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
231: CONSTRAINT ck_discounts_modality CHECK (modality IN ('AUTOMATIC', 'CODE')),
232: CONSTRAINT ck_discounts_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
233: CONSTRAINT ck_discounts_value CHECK (value > 0),
234: CONSTRAINT ck_discounts_minimum_purchase CHECK (minimum_purchase_amount IS NULL OR minimum_purchase_amount >= 0),
235: CONSTRAINT ck_discounts_percentage CHECK (type != 'PERCENTAGE' OR value <= 100),
236: CONSTRAINT ck_discounts_code_modality CHECK ((modality = 'CODE' AND code IS NOT NULL) OR (modality = 'AUTOMATIC' AND code IS NULL)),
237: CONSTRAINT ck_discounts_dates CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
241: CREATE UNIQUE INDEX uq_idx_discounts_store_code ON discounts (store_id, code) WHERE code IS NOT NULL;
247: id UUID NOT NULL,
248: store_id UUID NOT NULL,
249: day_of_week INTEGER NOT NULL,
252: max_capacity INTEGER NOT NULL,
253: status VARCHAR(30) NOT NULL,
254: created_at TIMESTAMPTZ NOT NULL,
255: updated_at TIMESTAMPTZ NOT NULL,
257: CONSTRAINT fk_capacity_configurations_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
258: CONSTRAINT ck_capacity_configurations_day CHECK (day_of_week BETWEEN 0 AND 6),
259: CONSTRAINT ck_capacity_configurations_max_capacity CHECK (max_capacity >= 0),
260: CONSTRAINT ck_capacity_configurations_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
261: CONSTRAINT ck_capacity_configurations_time_null CHECK ((start_time IS NULL) = (end_time IS NULL)),
262: CONSTRAINT ck_capacity_configurations_time_order CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time)
271: id UUID NOT NULL,
272: store_id UUID NOT NULL,
273: service_date DATE NOT NULL,
276: exception_capacity INTEGER NOT NULL,
278: status VARCHAR(30) NOT NULL,
279: created_at TIMESTAMPTZ NOT NULL,
280: updated_at TIMESTAMPTZ NOT NULL,
282: CONSTRAINT fk_capacity_exceptions_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
283: CONSTRAINT ck_capacity_exceptions_capacity CHECK (exception_capacity >= 0),
284: CONSTRAINT ck_capacity_exceptions_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
285: CONSTRAINT ck_capacity_exceptions_time_null CHECK ((start_time IS NULL) = (end_time IS NULL)),
286: CONSTRAINT ck_capacity_exceptions_time_order CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time)
295: id UUID NOT NULL,
296: store_id UUID NOT NULL,
297: user_id UUID NOT NULL,
298: service_date DATE NOT NULL,
301: status VARCHAR(30) NOT NULL,
302: expires_at TIMESTAMPTZ NOT NULL,
304: created_at TIMESTAMPTZ NOT NULL,
305: updated_at TIMESTAMPTZ NOT NULL,
307: CONSTRAINT fk_capacity_reservations_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
308: CONSTRAINT fk_capacity_reservations_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
309: CONSTRAINT ck_capacity_reservations_status CHECK (status IN ('ACTIVE', 'PAYMENT_PROTECTED', 'COMMITTED', 'EXPIRED', 'RELEASED'))
320: id UUID NOT NULL,
321: capacity_reservation_id UUID NOT NULL,
322: amount NUMERIC(12,2) NOT NULL,
323: currency CHAR(3) NOT NULL,
324: status VARCHAR(30) NOT NULL,
325: refund_status VARCHAR(30) NOT NULL,
330: created_at TIMESTAMPTZ NOT NULL,
331: updated_at TIMESTAMPTZ NOT NULL,
333: CONSTRAINT uq_payments_capacity_reservation_id UNIQUE (capacity_reservation_id),
334: CONSTRAINT fk_payments_capacity_reservation_id FOREIGN KEY (capacity_reservation_id) REFERENCES capacity_reservations(id) ON DELETE RESTRICT,
335: CONSTRAINT ck_payments_amount CHECK (amount > 0),
336: CONSTRAINT ck_payments_currency CHECK (currency = 'PEN'),
337: CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'PROCESSING', 'APPROVED', 'FAILED')),
338: CONSTRAINT ck_payments_refund_status CHECK (refund_status IN ('NOT_REQUIRED', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
339: CONSTRAINT ck_payments_refund_amount CHECK (refund_amount IS NULL OR refund_amount >= 0),
340: CONSTRAINT ck_payments_refund_limit CHECK (refund_amount IS NULL OR refund_amount <= amount)
347: id UUID NOT NULL,
348: payment_id UUID NOT NULL,
349: attempt_number SMALLINT NOT NULL,
350: provider VARCHAR(30) NOT NULL,
351: payment_method VARCHAR(30) NOT NULL,
352: idempotency_key VARCHAR(64) NOT NULL,
356: status VARCHAR(30) NOT NULL,
359: created_at TIMESTAMPTZ NOT NULL,
361: updated_at TIMESTAMPTZ NOT NULL,
363: CONSTRAINT fk_payment_attempts_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
364: CONSTRAINT uq_payment_attempts_payment_number UNIQUE (payment_id, attempt_number),
365: CONSTRAINT uq_payment_attempts_provider_idempotency UNIQUE (provider, idempotency_key),
367: CONSTRAINT ck_payment_attempts_number CHECK (attempt_number > 0),
368: CONSTRAINT ck_payment_attempts_provider CHECK (provider = 'MERCADO_PAGO'),
369: CONSTRAINT ck_payment_attempts_payment_method CHECK (payment_method = 'CARD'),
370: CONSTRAINT ck_payment_attempts_status CHECK (status IN ('CREATED', 'PROCESSING', 'APPROVED', 'REJECTED', 'ERROR'))
374: CREATE UNIQUE INDEX uq_idx_payment_attempts_provider_payment_id ON payment_attempts (provider, provider_payment_id) WHERE provider_payment_id IS NOT NULL;
380: id UUID NOT NULL,
381: order_number VARCHAR(50) NOT NULL,
382: user_id UUID NOT NULL,
383: store_id UUID NOT NULL,
384: payment_id UUID NOT NULL,
385: capacity_reservation_id UUID NOT NULL,
386: status VARCHAR(30) NOT NULL,
387: delivery_mode VARCHAR(30) NOT NULL,
388: service_date DATE NOT NULL,
391: customer_name VARCHAR(200) NOT NULL,
392: customer_phone VARCHAR(30) NOT NULL,
393: customer_email VARCHAR(254) NOT NULL,
398: currency CHAR(3) NOT NULL,
399: products_subtotal_amount NUMERIC(12,2) NOT NULL,
400: discount_amount NUMERIC(12,2) NOT NULL,
402: delivery_fee_amount NUMERIC(12,2) NOT NULL,
403: included_tax_amount NUMERIC(12,2) NOT NULL,
404: total_amount NUMERIC(12,2) NOT NULL,
405: confirmed_at TIMESTAMPTZ NOT NULL,
406: updated_at TIMESTAMPTZ NOT NULL,
408: CONSTRAINT uq_orders_order_number UNIQUE (order_number),
409: CONSTRAINT uq_orders_payment_id UNIQUE (payment_id),
410: CONSTRAINT uq_orders_capacity_reservation_id UNIQUE (capacity_reservation_id),
411: CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
412: CONSTRAINT fk_orders_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT,
413: CONSTRAINT fk_orders_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE RESTRICT,
414: CONSTRAINT fk_orders_capacity_reservation_id FOREIGN KEY (capacity_reservation_id) REFERENCES capacity_reservations(id) ON DELETE RESTRICT,
415: CONSTRAINT ck_orders_status CHECK (status IN ('CONFIRMED', 'IN_PREPARATION', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED')),
416: CONSTRAINT ck_orders_delivery_mode CHECK (delivery_mode IN ('PICKUP', 'DELIVERY')),
417: CONSTRAINT ck_orders_currency CHECK (currency = 'PEN'),
418: CONSTRAINT ck_orders_products_subtotal CHECK (products_subtotal_amount >= 0),
419: CONSTRAINT ck_orders_discount CHECK (discount_amount >= 0),
420: CONSTRAINT ck_orders_discount_limit CHECK (discount_amount <= products_subtotal_amount),
421: CONSTRAINT ck_orders_delivery_fee CHECK (delivery_fee_amount >= 0),
422: CONSTRAINT ck_orders_tax CHECK (included_tax_amount >= 0),
423: CONSTRAINT ck_orders_total CHECK (total_amount >= 0),
424: CONSTRAINT ck_orders_delivery_latitude CHECK (delivery_latitude IS NULL OR delivery_latitude BETWEEN -90 AND 90),
425: CONSTRAINT ck_orders_delivery_longitude CHECK (delivery_longitude IS NULL OR delivery_longitude BETWEEN -180 AND 180),
426: CONSTRAINT ck_orders_delivery_address CHECK ((delivery_mode = 'DELIVERY' AND delivery_address IS NOT NULL) OR delivery_mode = 'PICKUP'),
427: CONSTRAINT ck_orders_service_time CHECK (service_start_time IS NULL OR service_end_time IS NULL OR service_start_time < service_end_time)
439: id UUID NOT NULL,
440: order_id UUID NOT NULL,
441: variant_id UUID NOT NULL,
442: product_name VARCHAR(160) NOT NULL,
443: variant_name VARCHAR(120) NOT NULL,
445: quantity INTEGER NOT NULL,
446: unit_price_amount NUMERIC(12,2) NOT NULL,
447: subtotal_amount NUMERIC(12,2) NOT NULL,
449: CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
450: CONSTRAINT fk_order_items_variant_id FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT,
451: CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
452: CONSTRAINT ck_order_items_unit_price CHECK (unit_price_amount >= 0),
453: CONSTRAINT ck_order_items_subtotal CHECK (subtotal_amount >= 0)
463: id UUID NOT NULL,
464: order_id UUID NOT NULL,
466: status VARCHAR(30) NOT NULL,
468: changed_at TIMESTAMPTZ NOT NULL,
470: CONSTRAINT fk_order_status_history_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
471: CONSTRAINT fk_order_status_history_changed_by_user_id FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL
480: user_id UUID NOT NULL,
481: product_id UUID NOT NULL,
482: created_at TIMESTAMPTZ NOT NULL,
484: CONSTRAINT fk_favorites_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
485: CONSTRAINT fk_favorites_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
492: id UUID NOT NULL,
493: user_id UUID NOT NULL,
494: product_id UUID NOT NULL,
495: rating INTEGER NOT NULL,
497: status VARCHAR(30) NOT NULL,
498: created_at TIMESTAMPTZ NOT NULL,
499: updated_at TIMESTAMPTZ NOT NULL,
501: CONSTRAINT uq_reviews_user_product UNIQUE (user_id, product_id),
502: CONSTRAINT fk_reviews_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
503: CONSTRAINT fk_reviews_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
504: CONSTRAINT ck_reviews_rating CHECK (rating >= 1 AND rating <= 5),
505: CONSTRAINT ck_reviews_status CHECK (status IN ('PUBLISHED', 'HIDDEN'))
514: id UUID NOT NULL,
515: user_id UUID NOT NULL,
516: type VARCHAR(30) NOT NULL,
517: title VARCHAR(180) NOT NULL,
518: message TEXT NOT NULL,
519: status VARCHAR(30) NOT NULL,
520: created_at TIMESTAMPTZ NOT NULL,
523: CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
524: CONSTRAINT ck_notifications_type CHECK (type IN ('NEW_ORDER', 'ORDER_STATUS_CHANGED', 'LOW_STOCK', 'SYSTEM')),
525: CONSTRAINT ck_notifications_status CHECK (status IN ('UNREAD', 'READ')),
526: CONSTRAINT ck_notifications_read_check CHECK ((status = 'UNREAD' AND read_at IS NULL) OR (status = 'READ' AND read_at IS NOT NULL))
```

### V20__add_provider_order_id_to_payment_attempts.sql

```sql
3: -- preservando preference_id para compatibilidad histórica con Preferences API.
9: WHERE provider_order_id IS NOT NULL;
```

### V21__add_unique_constraint_to_provider_order_id_and_checkout_mode.sql

```sql
1: -- Version: V21__add_unique_constraint_to_provider_order_id_and_checkout_mode.sql
2: -- Description: Añadir columna checkout_mode en payment_attempts para persistir explícitamente
3: -- la modalidad (PREFERENCES u ORDERS), y reemplazar el índice simple de provider_order_id
4: -- por un índice UNIQUE parcial, garantizando cardinalidad 1:1 estricta por intento.
6: -- 1. Añadir columna checkout_mode con valor por defecto 'PREFERENCES' para preservar compatibilidad con intentos históricos
7: ALTER TABLE payment_attempts ADD COLUMN IF NOT EXISTS checkout_mode VARCHAR(20) NOT NULL DEFAULT 'PREFERENCES';
12: -- 3. Crear índice UNIQUE condicional parcial para provider_order_id
13: CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_attempts_provider_order_id
15: WHERE provider_order_id IS NOT NULL;
```

### V22__add_orders_fields_to_refund_intents.sql

```sql
2: -- Description: Soporte bimodal para reembolsos en Orders API y Preferences API en payment_refund_intents.
5: ADD COLUMN IF NOT EXISTS checkout_mode VARCHAR(20) NOT NULL DEFAULT 'PREFERENCES',
7: ALTER COLUMN provider_payment_id DROP NOT NULL;
```

### V23__add_reconciliation_lease_to_payment_attempts.sql

```sql
7: ON payment_attempts(checkout_mode, status, created_at)
8: WHERE checkout_mode = 'ORDERS' AND status IN ('CREATED', 'PROCESSING') AND provider_order_id IS NOT NULL;
```

### V24__create_checkout_creation_requests.sql

```sql
2: UNIQUE (id, checkout_mode, idempotency_key);
4: CREATE TABLE checkout_creation_requests (
6: checkout_mode VARCHAR(20) NOT NULL CHECK (checkout_mode IN ('PREFERENCES', 'ORDERS')),
7: idempotency_key VARCHAR(64) NOT NULL,
8: format_version SMALLINT NOT NULL CHECK (format_version = 1),
9: request_body TEXT NOT NULL CHECK (jsonb_typeof(request_body::jsonb) = 'object'),
10: created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
11: CONSTRAINT fk_checkout_creation_context FOREIGN KEY (payment_attempt_id, checkout_mode, idempotency_key)
12: REFERENCES payment_attempts(id, checkout_mode, idempotency_key) ON DELETE CASCADE
15: CREATE FUNCTION prevent_checkout_creation_request_update() RETURNS trigger AS $$
17: RAISE EXCEPTION 'checkout creation requests are immutable';
21: CREATE TRIGGER checkout_creation_requests_immutable
22: BEFORE UPDATE ON checkout_creation_requests
23: FOR EACH ROW EXECUTE FUNCTION prevent_checkout_creation_request_update();
```

### V25__add_remote_order_creation_evidence.sql

```sql
3: UPDATE payment_attempts SET remote_order_creation_state = 'UNKNOWN' WHERE checkout_mode = 'ORDERS';
4: ALTER TABLE payment_attempts ADD CONSTRAINT ck_remote_order_creation_state CHECK (
5: (checkout_mode = 'ORDERS' AND remote_order_creation_state IS NOT NULL
7: OR (checkout_mode <> 'ORDERS' AND remote_order_creation_state IS NULL)
9: ALTER TABLE payment_attempts ADD CONSTRAINT ck_created_order_evidence CHECK (
11: OR (NULLIF(BTRIM(provider_order_id), '') IS NOT NULL AND NULLIF(BTRIM(init_point), '') IS NOT NULL)
17: payment_attempt_id UUID NOT NULL REFERENCES payment_attempts(id) ON DELETE CASCADE,
19: new_state VARCHAR(30) NOT NULL,
20: evidence_code VARCHAR(50) NOT NULL,
21: recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
25: SELECT id, 'UNKNOWN', 'HISTORICAL_UNVERIFIED' FROM payment_attempts WHERE checkout_mode = 'ORDERS';
30: IF NEW.checkout_mode = 'ORDERS' AND NEW.remote_order_creation_state IS NULL THEN
45: CREATE TRIGGER guard_remote_order_creation_state BEFORE INSERT OR UPDATE ON payment_attempts
52: IF NEW.remote_order_creation_state IS NOT NULL
66: CREATE TRIGGER audit_remote_order_creation_state AFTER INSERT OR UPDATE ON payment_attempts
74: CREATE TRIGGER remote_order_creation_events_immutable BEFORE UPDATE ON remote_order_creation_events
```

### V2__seed_roles.sql

```sql
29: password_encoded = EXCLUDED.password_encoded,
46: password_encoded = EXCLUDED.password_encoded,
63: password_encoded = EXCLUDED.password_encoded,
```

### V3__add_store_customers.sql

```sql
8: store_id UUID NOT NULL,
9: user_id UUID NOT NULL,
10: created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
12: CONSTRAINT fk_store_customers_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
13: CONSTRAINT fk_store_customers_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
```

### V4__seed_complete_demo_data.sql

```sql
28: ) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';
37: ) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';
46: ) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';
55: ) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';
64: ) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';
73: ) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';
82: ) ON CONFLICT (email) DO UPDATE SET password_encoded = EXCLUDED.password_encoded, status = 'ACTIVE';
133: merchant_user_id = EXCLUDED.merchant_user_id,
134: name = EXCLUDED.name,
135: slug = EXCLUDED.slug,
136: description = EXCLUDED.description,
161: merchant_user_id = EXCLUDED.merchant_user_id,
162: name = EXCLUDED.name,
163: slug = EXCLUDED.slug,
164: description = EXCLUDED.description,
201: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
206: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
211: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
217: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
222: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
227: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
246: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
261: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
276: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
291: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
307: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
322: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
337: ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, status = 'ACTIVE';
344: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
348: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
352: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
356: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
360: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
364: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
368: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
372: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
376: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
380: ON CONFLICT (id) DO UPDATE SET price_amount = EXCLUDED.price_amount, status = 'ACTIVE';
385: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf301', 'Tamaño', 'Entera (8-10 porciones)') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
386: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf302', 'Tamaño', 'Porción Individual') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
387: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf303', 'Porción', 'Individual') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
388: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf305', 'Presentación', 'Caja x6 Unidades') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
389: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf306', 'Presentación', 'Caja x12 Unidades') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
390: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf307', 'Tamaño', '12 oz') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
391: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf308', 'Tamaño', '16 oz') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
392: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf309', 'Tamaño', '16 oz') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
393: INSERT INTO variant_attributes (variant_id, name, value) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf310', 'Relleno', 'Jamón Inglés & Queso Gouda') ON CONFLICT (variant_id, name) DO UPDATE SET value = EXCLUDED.value;
398: INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf302', 15, 5, NOW()) ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, low_stock_threshold = EXCLUDED.low_stock_threshold, updated_at = EXCLUDED.updated_at;
399: INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf303', 2, 5, NOW()) ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, low_stock_threshold = EXCLUDED.low_stock_threshold, updated_at = EXCLUDED.updated_at;
400: INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf304', 8, 4, NOW()) ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, low_stock_threshold = EXCLUDED.low_stock_threshold, updated_at = EXCLUDED.updated_at;
401: INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf305', 25, 6, NOW()) ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, low_stock_threshold = EXCLUDED.low_stock_threshold, updated_at = EXCLUDED.updated_at;
402: INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf306', 10, 4, NOW()) ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, low_stock_threshold = EXCLUDED.low_stock_threshold, updated_at = EXCLUDED.updated_at;
403: INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at) VALUES ('80acf0ba-5a79-4888-b00d-b5aef8fcf310', 18, 5, NOW()) ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, low_stock_threshold = EXCLUDED.low_stock_threshold, updated_at = EXCLUDED.updated_at;
441: ON CONFLICT (id) DO UPDATE SET max_capacity = EXCLUDED.max_capacity, status = 'ACTIVE';
446: ON CONFLICT (id) DO UPDATE SET max_capacity = EXCLUDED.max_capacity, status = 'ACTIVE';
451: ON CONFLICT (id) DO UPDATE SET max_capacity = EXCLUDED.max_capacity, status = 'ACTIVE';
456: ON CONFLICT (id) DO UPDATE SET max_capacity = EXCLUDED.max_capacity, status = 'ACTIVE';
461: ON CONFLICT (id) DO UPDATE SET max_capacity = EXCLUDED.max_capacity, status = 'ACTIVE';
466: ON CONFLICT (id) DO UPDATE SET max_capacity = EXCLUDED.max_capacity, status = 'ACTIVE';
474: ON CONFLICT (id) DO UPDATE SET exception_capacity = EXCLUDED.exception_capacity, status = 'ACTIVE';
479: ON CONFLICT (id) DO UPDATE SET exception_capacity = EXCLUDED.exception_capacity, status = 'ACTIVE';
843: ON CONFLICT (cart_id, variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, reference_price_amount = EXCLUDED.reference_price_amount, updated_at = EXCLUDED.updated_at;
847: ON CONFLICT (cart_id, variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, reference_price_amount = EXCLUDED.reference_price_amount, updated_at = EXCLUDED.updated_at;
```

### V6__set_default_currency_and_normalize_phones.sql

```sql
25: -- 4. Ajustar CHECK constraint de tax_rate para permitir valores entre 0.00 y 100.00 inclusive
27: ALTER TABLE stores ADD CONSTRAINT ck_stores_tax_rate CHECK (tax_rate IS NULL OR (tax_rate >= 0 AND tax_rate <= 100));
```

### V7__strengthen_relational_integrity_and_multitenancy.sql

```sql
7: --   3. Contextual User Roles (User <-> Role <-> Store) with partial unique indexes.
13: --   9. Inventory Reservations for checkout stock holds.
22: id UUID NOT NULL,
23: name VARCHAR(120) NOT NULL,
24: slug VARCHAR(140) NOT NULL,
26: status VARCHAR(30) NOT NULL,
27: created_at TIMESTAMPTZ NOT NULL,
28: updated_at TIMESTAMPTZ NOT NULL,
30: CONSTRAINT uq_store_categories_slug UNIQUE (slug),
31: CONSTRAINT ck_store_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
35: store_id UUID NOT NULL,
36: store_category_id UUID NOT NULL,
38: CONSTRAINT fk_store_cat_assign_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
39: CONSTRAINT fk_store_cat_assign_category FOREIGN KEY (store_category_id) REFERENCES store_categories(id) ON DELETE CASCADE
55: ADD CONSTRAINT ck_stores_coordinates_null CHECK ((latitude IS NULL) = (longitude IS NULL));
62: ALTER TABLE user_roles ALTER COLUMN id SET NOT NULL;
77: ADD CONSTRAINT fk_user_roles_store_id FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE;
79: CREATE UNIQUE INDEX uq_idx_user_roles_global ON user_roles (user_id, role_id) WHERE store_id IS NULL;
80: CREATE UNIQUE INDEX uq_idx_user_roles_store ON user_roles (user_id, role_id, store_id) WHERE store_id IS NOT NULL;
81: CREATE INDEX idx_user_roles_store_id ON user_roles (store_id) WHERE store_id IS NOT NULL;
86: ALTER TABLE categories ADD CONSTRAINT uq_categories_store_id_id UNIQUE (store_id, id);
90: ADD CONSTRAINT fk_products_category_store FOREIGN KEY (store_id, category_id) REFERENCES categories(store_id, id) ON DELETE RESTRICT;
95: ALTER TABLE products ADD CONSTRAINT uq_products_store_id_id UNIQUE (store_id, id);
105: ALTER TABLE product_variants ALTER COLUMN store_id SET NOT NULL;
109: ADD CONSTRAINT fk_product_variants_product_store FOREIGN KEY (store_id, product_id) REFERENCES products(store_id, id) ON DELETE CASCADE;
112: CREATE UNIQUE INDEX uq_idx_product_variants_store_sku ON product_variants (store_id, sku) WHERE sku IS NOT NULL;
114: ALTER TABLE product_variants ADD CONSTRAINT uq_product_variants_store_id_id UNIQUE (store_id, id);
121: id UUID NOT NULL,
122: product_id UUID NOT NULL,
123: image_url TEXT NOT NULL,
124: position INTEGER NOT NULL DEFAULT 0,
125: is_primary BOOLEAN NOT NULL DEFAULT false,
126: created_at TIMESTAMPTZ NOT NULL,
128: CONSTRAINT fk_product_images_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
129: CONSTRAINT ck_product_images_position CHECK (position >= 0)
132: CREATE UNIQUE INDEX uq_idx_product_images_primary ON product_images (product_id) WHERE is_primary = true;
139: WHERE p.image_url IS NOT NULL AND TRIM(p.image_url) != ''
145: ALTER TABLE carts ADD CONSTRAINT uq_carts_store_id_id UNIQUE (store_id, id);
155: ALTER TABLE cart_items ALTER COLUMN store_id SET NOT NULL;
161: ADD CONSTRAINT fk_cart_items_cart_store FOREIGN KEY (store_id, cart_id) REFERENCES carts(store_id, id) ON DELETE CASCADE;
164: ADD CONSTRAINT fk_cart_items_variant_store FOREIGN KEY (store_id, variant_id) REFERENCES product_variants(store_id, id) ON DELETE CASCADE;
172: ADD CONSTRAINT ck_capacity_reservations_time_null CHECK ((start_time IS NULL) = (end_time IS NULL)),
173: ADD CONSTRAINT ck_capacity_reservations_time_order CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time);
175: ALTER TABLE capacity_reservations ADD CONSTRAINT uq_capacity_reservations_store_user_id UNIQUE (store_id, user_id, id);
179: ADD CONSTRAINT ck_orders_service_time_null CHECK ((service_start_time IS NULL) = (service_end_time IS NULL)),
180: ADD CONSTRAINT ck_orders_service_time_order CHECK (service_start_time IS NULL OR service_end_time IS NULL OR service_start_time < service_end_time);
183: -- 9. INVENTORY RESERVATIONS (Holds temporales de stock para checkout)
186: id UUID NOT NULL,
187: capacity_reservation_id UUID NOT NULL,
188: variant_id UUID NOT NULL,
189: quantity INTEGER NOT NULL,
190: status VARCHAR(30) NOT NULL,
191: expires_at TIMESTAMPTZ NOT NULL,
192: created_at TIMESTAMPTZ NOT NULL,
193: updated_at TIMESTAMPTZ NOT NULL,
195: CONSTRAINT fk_inventory_reservations_capacity_reservation FOREIGN KEY (capacity_reservation_id) REFERENCES capacity_reservations(id) ON DELETE CASCADE,
196: CONSTRAINT fk_inventory_reservations_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE RESTRICT,
197: CONSTRAINT uq_inventory_reservations_reservation_variant UNIQUE (capacity_reservation_id, variant_id),
198: CONSTRAINT ck_inventory_reservations_quantity CHECK (quantity > 0),
199: CONSTRAINT ck_inventory_reservations_status CHECK (status IN ('ACTIVE', 'COMMITTED', 'EXPIRED', 'RELEASED'))
209: ALTER TABLE payments ADD CONSTRAINT uq_payments_reservation_id UNIQUE (capacity_reservation_id, id);
211: ALTER TABLE orders ADD CONSTRAINT uq_orders_store_id_id UNIQUE (store_id, id);
215: ADD CONSTRAINT fk_orders_capacity_reservation_store_user FOREIGN KEY (store_id, user_id, capacity_reservation_id) REFERENCES capacity_reservations(store_id, user_id, id) ON DELETE RESTRICT;
219: ADD CONSTRAINT fk_orders_payment_capacity_reservation FOREIGN KEY (capacity_reservation_id, payment_id) REFERENCES payments(capacity_reservation_id, id) ON DELETE RESTRICT;
232: ALTER TABLE order_items ALTER COLUMN store_id SET NOT NULL;
238: ADD CONSTRAINT fk_order_items_order_store FOREIGN KEY (store_id, order_id) REFERENCES orders(store_id, id) ON DELETE CASCADE;
241: ADD CONSTRAINT fk_order_items_variant_store FOREIGN KEY (store_id, variant_id) REFERENCES product_variants(store_id, id) ON DELETE RESTRICT;
```

### V8__seed_store_categories_and_branding.sql

```sql
24: name = EXCLUDED.name,
25: description = EXCLUDED.description,
26: status = EXCLUDED.status,
65: WHERE p.image_url IS NOT NULL
```
