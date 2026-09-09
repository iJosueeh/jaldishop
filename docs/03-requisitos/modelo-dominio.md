# Modelo de Dominio

### JaldiShop — Conceptos y Responsabilidades del Dominio v1.5

[![Estado](https://img.shields.io/badge/Estado-En%20Revisión-orange?style=for-the-badge&logo=checkmarx&logoColor=white)](./modelo-dominio.md)
[![Versión](https://img.shields.io/badge/Versión-v1.5-blue?style=for-the-badge)](./modelo-dominio.md)
[![Fase](https://img.shields.io/badge/Fase-Sprint_02-orange?style=for-the-badge)](../06-scrum/sprint-02.md)

---

`📍 Docs` > `03-Requisitos` > **Modelo de Dominio**  
[⬅ Alcance del MVP](./alcance-mvp.md) | [🏠 Índice General](../../README.md) | [Reglas de Negocio ➡](./reglas-negocio.md)

---

## 1. Propósito

> 📌 **Nota:** Este documento identifica los principales conceptos del dominio de **JaldiShop** y define sus responsabilidades dentro del negocio. El modelo se mantiene independiente de decisiones técnicas como frameworks, base de datos, endpoints, DTO, repositorios o mecanismos de comunicación.

## 2. Principios del Dominio

> 💡 **Conceptos Diferenciados:**  
> JaldiShop diferencia cuatro conceptos durante el proceso de compra:

| Concepto | Definición |
|---|---|
| **Carrito** | Intención de compra |
| **Reserva de capacidad** | Retención temporal de un cupo operativo |
| **Pago** | Proceso financiero mediante el cual se intenta completar la compra |
| **Pedido** | Compra confirmada |

Por tanto:

```text
Carrito ≠ ReservaCapacidad ≠ Pago ≠ Pedido
```

El **inventario** y la **capacidad operativa** también representan restricciones diferentes:

| Restricción | Descripción |
|---|---|
| **Inventario** | Cuánto stock existe de una presentación vendible |
| **Capacidad** | Cuántos pedidos puede atender la tienda en un periodo determinado |

------------------------------------------------------------------------

## 3. Entidades Principales y Responsabilidades

### 3.1 Usuario

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar a una persona registrada en JaldiShop, manteniendo su identidad, estado y los roles mediante los cuales participa en la plataforma.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único del usuario |
| nombres | Nombres del usuario |
| apellidos | Apellidos del usuario |
| correo | Correo electrónico (único para autenticación) |
| telefono | Teléfono de contacto para pedidos |
| credencialAcceso | Concepto de autenticación |
| roles | Colección de roles (CLIENTE, COMERCIANTE, ADMIN) |
| estado | ACTIVO o SUSPENDIDO |
| fechaRegistro | Fecha de creación de la cuenta |
| fechaActualizacion | Última modificación |

**Reglas:**

- El correo identifica la cuenta para autenticación y no puede repetirse.
- Un Usuario puede tener múltiples roles simultáneamente.
- Roles iniciales: CUSTOMER, MERCHANT, ADMIN.
- El teléfono sirve como medio de contacto para los pedidos.
- Puede permitirse registro sin teléfono, pero debe existir antes de completar un checkout cuando sea necesario.
- `nombreCompleto` es calculado a partir de nombres + apellidos, no debe persisterse como dato independiente.
- Tienda, Pedidos, Favoritos, Reseñas y Notificaciones son relaciones, no atributos internos del aggregate.

---

### 3.2 Tienda

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar a la MYPE que opera dentro de JaldiShop, manteniendo su información comercial, estado y configuración general de operación.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único de la tienda |
| nombreComercial | Nombre comercial de la tienda |
| slug | Identificador URL único dentro de la plataforma |
| descripcion | Descripción de la tienda |
| telefonoContacto | Teléfono de contacto comercial |
| ubicacion | UbicacionTienda (Value Object) |
| estado | ACTIVA, INACTIVA, SUSPENDIDA, CERRADA |
| configuracionEntrega | ConfiguracionEntrega (Value Object) |
| configuracionTributaria | ConfiguracionTributaria (Value Object) |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Reglas:**

- Cada tienda pertenece a exactamente un comerciante responsable en el MVP.
- Un comerciante puede tener como máximo una tienda.
- El slug debe ser único dentro de la plataforma y se genera automáticamente a partir del nombre comercial.
- SUSPENDIDA representa restricción administrativa/plataforma.
- INACTIVA/CERRADA representan estados comerciales.
- Categorías, productos, pedidos y reservas son relaciones, no contenido interno del aggregate.

**Value Object: UbicacionTienda**

| Atributo | Descripción |
|---|---|
| direccion | Dirección de la tienda |
| referencia | Referencia opcional |
| latitud | Coordenada opcional |
| longitud | Coordenada opcional |

> 💡 Las coordenadas son opcionales porque la integración con Maps es un plus y no requisito central.

---

### 3.3 Categoría

**Clasificación:** Aggregate Root

**Responsabilidad:** Organizar los productos ofrecidos por una tienda y permitir al comerciante gestionar su clasificación comercial.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único de la categoría |
| nombre | Nombre de la categoría |
| descripcion | Descripción opcional |
| estado | ACTIVA o INACTIVA |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Reglas:**

- Cada Categoria pertenece a una Tienda.
- Cada Producto pertenece exactamente a una Categoria en el MVP.
- No pueden existir dos categorías ACTIVAS con el mismo nombre normalizado dentro de una misma Tienda.
- El mismo nombre de categoría puede existir en tiendas diferentes.
- No existen subcategorías en el MVP.
- `cantidadProductos` no es atributo persistido; es calculado.
- Solo puede eliminarse si no existe historial o registros relacionados; en caso contrario debe desactivarse.

---

### 3.4 Producto

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar el concepto comercial que una tienda ofrece a sus clientes, manteniendo su información general y disponibilidad comercial.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único del producto |
| nombre | Nombre del producto |
| slug | Identificador URL único dentro de la tienda |
| descripcion | Descripción del producto |
| imagenPrincipal | Imagen principal opcional |
| estado | ACTIVO o INACTIVO |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Reglas:**

- Cada Producto pertenece exactamente a una Tienda.
- Cada Producto pertenece exactamente a una Categoría.
- La Categoría asignada debe pertenecer a la misma Tienda del Producto.
- El slug se genera automáticamente a partir del nombre y debe ser único dentro de su Tienda.
- Producto NO almacena precio como fuente de verdad.
- Producto NO controla directamente inventario.
- AGOTADO no es EstadoProducto: deriva de Inventario.
- SATURADO no es EstadoProducto: deriva de Capacidad.
- Debe contener al menos una VarianteProducto.
- Si un producto no presenta opciones visibles, debe existir una variante por defecto/oculta.
- La imagen principal pertenece al Producto en el MVP. No implementar múltiples imágenes ni galería avanzada.

**Contiene internamente:**

#### VarianteProducto

**Clasificación:** Entidad interna

**Responsabilidad:** Representar la unidad/presentación realmente vendible de un producto.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador interno | Identificador de la variante |
| nombrePresentacion | Nombre de la presentación |
| atributos | Colección de AtributoVariante |
| precio | Dinero (precio de venta, mayor que cero) |
| sku | Identificador comercial opcional |
| controlaInventario | Booleano que indica si requiere control de stock |
| estado | ACTIVA o INACTIVA |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Reglas:**

- Una VarianteProducto pertenece exactamente a un Producto.
- El precio pertenece a VarianteProducto y utiliza el Value Object Dinero.
- El precio debe ser mayor que cero.
- SKU es opcional. Cuando existe, debe ser único dentro de la Tienda.
- `controlaInventario` determina si la variante necesita Inventario.
- Una Variante puede tener cero o múltiples AtributoVariante.
- AGOTADA no es EstadoVariante.
- Dentro de una misma VarianteProducto no puede repetirse el nombre de un atributo.
- Si un Producto solo tiene una variante estándar, el frontend puede omitir el selector de variantes. No agregar `isDefault` únicamente para resolver este comportamiento.

**AtributoVariante — Value Object**

| Atributo | Descripción |
|---|---|
| nombre | Nombre del atributo (ej: Talla) |
| valor | Valor del atributo (ej: M) |

**Reglas:**

- AtributoVariante es objeto interno de VarianteProducto.
- Representa nombre + valor.
- Una variante puede no tener atributos.
- No utilizar un modelo EAV complejo en el MVP.
- No convertir cada tipo de atributo en una entidad independiente.

---

### 3.5 Inventario

**Clasificación:** Aggregate Root

**Responsabilidad:** Mantener la cantidad disponible de una variante que utiliza control de existencias, permitiendo validar disponibilidad y evitar stock negativo.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único del inventario |
| stockActual | Cantidad disponible actual (nunca negativa) |
| umbralStockBajo | Umbral para notificación de stock bajo (opcional, tampoco puede ser negativo) |
| fechaActualizacion | Última modificación |

**Reglas:**

- Inventario pertenece a VarianteProducto, no a Producto.
- Una Variante puede tener como máximo un Inventario asociado.
- Solo existe Inventario para variantes con `controlaInventario = true`.
- La cantidad nunca puede ser negativa.
- El umbral de stock bajo, cuando exista, tampoco puede ser negativo.
- No existe ReservaInventario en el MVP.
- El Carrito NO reserva inventario.
- No existen `reservedQuantity`, `committedQuantity` ni conceptos equivalentes.
- No implementar kardex avanzado ni movimientos históricos de inventario.
- No implementar almacenes, lotes, proveedores ni transferencias.
- Los ajustes manuales del comerciante modifican directamente `stockActual`.
- Si `controlaInventario` se deshabilita, no debe eliminarse físicamente la información de Inventario existente.
- Si posteriormente se vuelve a habilitar, el inventario existente puede reutilizarse.
- Cuando el stock alcanza o queda por debajo del umbral, puede generarse una Notificacion de tipo STOCK_BAJO.

**Control de inventario:**

- Si una Variante tiene control de inventario habilitado, el stock participa en las validaciones de compra.
- Si el control está deshabilitado, el stock no determina la disponibilidad comercial.

**Estrategia de concurrencia — Inventario:**

> ✅ **Decisión cerrada:** El descuento de existencias se realiza mediante actualización condicional atómica.

```
UPDATE inventario
SET cantidad = cantidad - cantidadSolicitada
WHERE variante = varianteSolicitada
  AND cantidad >= cantidadSolicitada
```

- Si la operación no modifica ningún registro, se interpreta como stock insuficiente.
- No utilizar optimistic locking/@Version como estrategia principal.
- No utilizar pessimistic locking como estrategia principal.
- Cuando un Pedido contiene varias variantes, todos los descuentos deben formar parte de una misma transacción.
- Si falla el descuento de una variante, los descuentos anteriores deben revertirse.
- La restauración por cancelación debe ser idempotente.

---

### 3.6 Carrito

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar la intención de compra activa de un cliente dentro de una tienda, manteniendo las variantes seleccionadas y sus cantidades antes de iniciar la confirmación de compra.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único del carrito |
| estado | ACTIVO o FINALIZADO |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Reglas:**

- Carrito representa únicamente intención de compra.
- Pertenece exactamente a un Usuario y una Tienda.
- Un usuario puede tener como máximo un carrito activo por Tienda.
- Un carrito contiene productos únicamente de una misma Tienda.
- No necesita un ciclo de estados complejo para el MVP. Si existe, se considera activo.
- Después de una compra confirmada puede vaciarse/eliminarse porque Pedido conserva la historia.
- Carrito NO reserva Inventario.
- Carrito NO reserva Capacidad.
- Carrito NO congela precios.
- Los valores comerciales deben revalidarse durante checkout.

**Conceptualmente:**
> 💡 **Carrito = información comercial viva. Pedido = fotografía histórica definitiva.**

**Contiene internamente:**

#### ItemCarrito

**Clasificación:** Entidad interna

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador interno | Identificador del item |
| cantidad | Cantidad seleccionada (>= 1) |
| precioReferencial | Dinero - precio mostrado al agregar (no garantiza precio final) |
| fechaAgregado | Fecha en que se agregó |
| fechaActualizacion | Última modificación |

**Reglas:**

- ItemCarrito referencia exactamente una VarianteProducto.
- Mantiene la cantidad seleccionada. Cantidad debe ser mayor que cero.
- Una misma Variante no puede aparecer repetida dentro del mismo Carrito.
- Si la cantidad llega a cero, el Item debe eliminarse.
- No almacenar stock dentro de ItemCarrito.
- No utilizar el precio almacenado en ItemCarrito como fuente contractual.
- El precio actual procede de VarianteProducto.
- Si precio, stock, producto, variante o tienda cambian después de agregar el Item, se debe revalidar durante checkout.
- `nombreProducto`, `nombreVariante` e `imagen` no se almacenan como snapshot en ItemCarrito.

---

### 3.7 Descuento

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar una promoción comercial creada por una tienda que permite reducir el valor de una compra según una condición básica, vigencia y modalidad definida.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único del descuento |
| nombre | Nombre de la promoción |
| descripcion | Descripción opcional |
| tipo | PORCENTAJE o MONTO_FIJO |
| modalidad | AUTOMATICO o CODIGO |
| valor | Monto o porcentaje del descuento (mayor que cero) |
| codigoDescuento | CodigoDescuento (solo si modalidad = CODIGO) |
| fechaInicio | Fecha de inicio de vigencia |
| fechaFin | Fecha de fin opcional |
| estado | Estado del descuento |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Reglas:**

- Una Tienda puede definir múltiples descuentos.
- El valor debe ser mayor que cero.
- Si es porcentaje, no puede superar 100%.
- El código solo corresponde a modalidad CODIGO.
- Los códigos son únicos dentro de una Tienda, no globalmente.
- Puede existir una compra mínima.
- Puede existir una ventana de vigencia.
- Una compra utiliza como máximo un descuento.
- No implementar descuentos acumulables, stacking de promociones ni múltiples descuentos simultáneos.

**Prioridad de descuento:**

> ✅ **Regla cerrada:** Cuando existe un código de descuento válido y también existe un descuento automático aplicable, el descuento mediante código tiene prioridad.

> ✅ **Regla cerrada:** Una Tienda puede tener como máximo un descuento automático vigente/aplicable simultáneamente.

**Value Object: CodigoDescuento**

| Atributo | Descripción |
|---|---|
| valor | Código de promoción |

------------------------------------------------------------------------

## 4. Capacidad Operativa

### 4.1 ConfiguracionCapacidad

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar las condiciones operativas base mediante las cuales una tienda establece cuántos pedidos puede atender dentro de un determinado periodo.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único |
| diaSemana | Día de la semana al que aplica |
| periodoCapacidad | PeriodoCapacidad (Value Object) |
| capacidadMaxima | Cantidad máxima de pedidos/cupos (>= 0) |
| estado | Estado de la configuración |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Reglas:**

- Pertenece a una Tienda.
- Define capacidad base recurrente.
- Si horaInicio y horaFin están ausentes, representa jornada completa.
- Si se utiliza franja, ambas horas deben existir.
- horaInicio debe ser menor que horaFin.
- `capacidadMaxima` expresa cantidad de pedidos/cupos. MVP: **1 pedido = 1 cupo**.
- No representa productos, kg, horas ni unidades de inventario.
- Una Tienda puede tener varias configuraciones por días y franjas.
- Para una misma Tienda y día no pueden existir configuraciones activas con periodos solapados.
- Para simplificar, configuraciones recurrentes (lunes-viernes) se representan mediante configuraciones individuales por día.

---

### 4.2 PeriodoCapacidad

**Clasificación:** Value Object

**Responsabilidad:** Representar exclusivamente una franja dentro de una jornada.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| horaInicio | Hora de inicio opcional |
| horaFin | Hora de fin opcional |

**Interpretación:**

| Combinación | Significado |
|---|---|
| ambas ausentes | Jornada completa |
| ambas presentes | Franja horaria específica |

> ⚠️ La información de calendario NO pertenece al VO:
> - ConfiguracionCapacidad define diaSemana
> - ExcepcionCapacidad define fecha concreta
> - ReservaCapacidad define fechaOperativa concreta
>
> No crear TipoCapacidad DIARIA/HORARIA en el MVP porque puede inferirse de la presencia de horas.

---

### 4.3 ExcepcionCapacidad

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar una modificación temporal de la capacidad operativa habitual de una tienda para una fecha concreta.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único |
| fecha | Fecha de la excepción |
| periodoCapacidad | PeriodoCapacidad (Value Object) |
| capacidadExcepcional | Capacidad para ese periodo (puede ser 0 = cierre) |
| motivo | Motivo opcional |
| estado | Estado de la excepción |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Reglas:**

- Pertenece a una Tienda.
- Aplica a una fecha concreta y opcionalmente a una franja.
- Una excepción REEMPLAZA la capacidad base correspondiente; NO se suma.
- Ejemplo: capacidad base 10 y excepción 4 → capacidad efectiva 4.
- `capacidadExcepcional` puede ser 0 para representar cierre/no disponibilidad.
- La capacidad excepcional nunca puede ser negativa.
- Para una misma Tienda y fecha no pueden existir excepciones activas con periodos solapados.

---

### 4.4 ReservaCapacidad

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar la retención temporal de un cupo operativo durante el proceso de compra, evitando que dicho cupo sea utilizado simultáneamente por otra compra mientras la reserva permanezca válida.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único |
| fechaOperativa | Fecha en que el negocio utilizará esa capacidad |
| periodoCapacidad | PeriodoCapacidad (Value Object) |
| estado | EstadoReservaCapacidad |
| fechaCreacion | Fecha de creación |
| expiraEn | Fecha/hora hasta cuándo la reserva inicial permanece válida |
| proteccionPagoExpiraEn | Fecha/hora límite de protección de pago (opcional) |

**Estados:**

| Estado | Descripción |
|---|---|
| ACTIVA | Hold temporal creado durante checkout. Consume un cupo mientras siga temporalmente vigente. Hold normal: 10 minutos para iniciar el pago. |
| PROTEGIDA_PAGO | El cliente inició válidamente el proceso de pago. Continúa consumiendo el cupo. Protección máxima adicional: 10 minutos. |
| COMPROMETIDA | La compra fue confirmada correctamente y existe Pedido. Consume capacidad definitivamente mientras corresponda operativamente. |
| EXPIRADA | El hold inicial venció sin iniciar pago. No consume capacidad. |
| LIBERADA | La reserva dejó de consumir capacidad por cancelación, fallo u otra liberación válida. No consume capacidad. |

**Reglas:**

- Cada ReservaCapacidad representa exactamente **1 cupo** en el MVP.
- No almacenar `cantidadCupos` porque siempre es 1.
- Una reserva ACTIVA dura máximo 10 minutos.
- Si se inicia un pago válido antes de expirar: ACTIVA → PROTEGIDA_PAGO.
- La protección de pago dura máximo 10 minutos adicionales.
- `proteccionPagoExpiraEn` registra dicho límite.
- Una reserva solo se vuelve COMPROMETIDA después de que ConfirmPurchaseUseCase finalice correctamente.

**Transiciones válidas:**

```
ACTIVA → PROTEGIDA_PAGO
ACTIVA → EXPIRADA
ACTIVA → LIBERADA
PROTEGIDA_PAGO → COMPROMETIDA
PROTEGIDA_PAGO → LIBERADA
COMPROMETIDA → LIBERADA (solo cuando el Pedido se cancela antes de EN_PREPARACION)
```

**Flujo correcto de confirmación:**

> ⚠️ Pago aprobado NO implica por sí mismo ReservaCapacidad = COMPROMETIDA.

```
Pago aprobado
→ ejecutar confirmación de compra
→ crear Pedido
→ aplicar efectos correspondientes
→ marcar ReservaCapacidad como COMPROMETIDA
```

**La Reserva debe conservar una fotografía del periodo operativo reservado. No debe depender exclusivamente de que la ConfiguracionCapacidad original permanezca sin cambios.**

---

### 4.5 Cálculo de Capacidad Disponible

**CapacidadDisponible NO es una entidad.**

**Conceptualmente:**

```
Capacidad disponible = capacidad efectiva - capacidad actualmente consumida
```

**Capacidad efectiva:**
- excepción aplicable, si existe
- de lo contrario configuración base

**Consumen capacidad:**
- ACTIVA mientras no haya expirado
- PROTEGIDA_PAGO mientras no haya expirado su protección
- COMPROMETIDA

**No consumen:**
- EXPIRADA
- LIBERADA

> ⚠️ La disponibilidad no puede depender exclusivamente de que un scheduler haya actualizado físicamente los estados.

**Ejemplo:** Si una reserva continúa almacenada como ACTIVA pero `expiresAt` ya pasó, conceptualmente no debe seguir consumiendo capacidad. Un job periódico puede limpiar/actualizar estados posteriormente, pero NO constituye la fuente de verdad para calcular disponibilidad.

---

### 4.6 Estrategia de Concurrencia — Capacidad

> ✅ **Decisión cerrada:** Bloqueo pesimista durante la creación de ReservaCapacidad.

**Flujo conceptual:**

1. Iniciar transacción.
2. Identificar configuración/excepción aplicable al periodo.
3. Bloquear el registro de capacidad correspondiente.
4. Calcular capacidad efectiva.
5. Contar reservas consumidoras temporalmente válidas.
6. Verificar que utilizadas < capacidad efectiva.
7. Crear ReservaCapacidad ACTIVA.
8. Confirmar transacción.

Una solicitud concurrente para el mismo periodo debe esperar y posteriormente recalcular disponibilidad.

Esto impide que dos checkout consuman simultáneamente el último cupo.

> 📌 Los detalles técnicos de implementación (@Lock, queries concretas) se definirán en arquitectura/diseño técnico.

---

## 5. Pago

### 5.1 Pago

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar el proceso financiero lógico asociado a una intención de compra protegida por una ReservaCapacidad.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único |
| monto | Dinero - monto a cobrar |
| estadoPago | EstadoPago |
| estadoReembolso | EstadoReembolso |
| montoReembolsado | Dinero - monto reembolsado (>= 0) |
| referenciaReembolso | Referencia de reembolso (opcional) |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |
| fechaAprobacion | Fecha de aprobación (opcional) |
| fechaReembolso | Fecha de reembolso (opcional) |

**Relaciones conceptuales:**

- Una ReservaCapacidad puede tener como máximo un Pago lógico asociado.
- Un Pago pertenece al proceso protegido por una ReservaCapacidad.
- Pago y ReservaCapacidad son Aggregate Roots independientes.
- Un Pago puede terminar asociado a 0..1 Pedido.
- Un Pedido confirmado debe estar respaldado por exactamente un Pago aprobado.

**Reglas:**

- Pago puede existir antes que Pedido.
- El monto se determina luego de la revalidación del checkout.
- Una vez iniciado el proceso de pago, el monto no debe modificarse arbitrariamente.
- Un Pago puede contener múltiples IntentoPago.
- Todos los IntentoPago del mismo Pago intentan cobrar el mismo monto.
- Un mismo Pago puede tener como máximo una aprobación financiera válida.
- Pago APROBADO NO implica automáticamente que exista Pedido.
- Un mismo Pago aprobado puede generar como máximo un Pedido.
- Un Pago rechazado o fallido no genera Pedido.
- Los datos sensibles del medio de pago nunca se almacenan dentro del dominio de JaldiShop.

---

### 5.2 EstadoPago

**Clasificación:** Enum

**Estados definitivos del MVP:**

| Estado | Descripción |
|---|---|
| PENDIENTE | El Pago fue creado pero todavía no se encuentra ejecutando un intento activo. |
| PROCESANDO | Existe un intento de pago en procesamiento. |
| APROBADO | El proveedor confirmó una aprobación financiera válida. |
| FALLIDO | El proceso financiero terminó definitivamente sin obtener una aprobación válida. |

> ⚠️ Un IntentoPago rechazado NO implica automáticamente Pago = FALLIDO si todavía puede ejecutarse otro intento dentro de la ventana permitida.

---

### 5.3 IntentoPago

**Clasificación:** Entidad interna

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador interno | Identificador del intento |
| numeroIntento | Número de intento para preservar orden histórico |
| metodoPago | MetodoPago utilizado |
| proveedorPago | ProveedorPago (MERCADO_PAGO, CULQI) |
| referenciaPagoExterno | ReferenciaPagoExterno (opcional) |
| estado | EstadoIntentoPago |
| mensajeError | Mensaje de error (opcional) |
| fechaInicio | Fecha de inicio del intento |
| fechaFinalizacion | Fecha de finalización (opcional) |

**Reglas:**

- Pago 1 → 1..N IntentoPago.
- Un Pago puede registrar varios intentos.
- Un intento no tiene ciclo de vida independiente de Pago.
- Los reintentos NO extienden la protección máxima de ReservaCapacidad.
- Todos los intentos están sujetos al mismo límite temporal de protección del checkout.

---

### 5.4 EstadoIntentoPago

**Clasificación:** Enum

| Estado | Descripción |
|---|---|
| INICIADO | Intento iniciado |
| PROCESANDO | Operación en procesamiento con el proveedor |
| APROBADO | El proveedor aceptó el cobro |
| RECHAZADO | El proveedor respondió correctamente que el cobro no fue aceptado |
| ERROR | No fue posible completar correctamente la operación técnica o de comunicación |

> ⚠️ No reutilizar EstadoPago como EstadoIntentoPago aunque algunos valores se parezcan.

---

### 5.5 MetodoPago

**Clasificación:** Enum

**Responsabilidad:** Representar el medio utilizado por el cliente para realizar un pago.

> 📌 **Nota:** MetodoPago pertenece principalmente a IntentoPago, no como fuente de verdad obligatoria dentro de Pago.

---

### 5.6 ProveedorPago

**Clasificación:** Enum

**Responsabilidad:** Representar el proveedor de pagos utilizado.

| Valor | Descripción |
|---|---|
| MERCADO_PAGO | Proveedor Mercado Pago |
| CULQI | Proveedor Culqi |

> 💡 El MVP puede implementar inicialmente solo Mercado Pago. No acoplar el dominio a nombres internos del SDK del proveedor.

---

### 5.7 ReferenciaPagoExterno

**Clasificación:** Value Object

**Responsabilidad:** Representar la referencia externa necesaria para identificar, reconciliar y verificar la operación sin acoplar el dominio a campos específicos del proveedor.

**Reglas:**

- Puede utilizarse para verificación, compensación e idempotencia.
- NO almacenar: número de tarjeta, CVV, fecha de vencimiento, PIN, token sensible, JSON completo del proveedor ni datos equivalentes sensibles.

---

### 5.8 EstadoReembolso

**Clasificación:** Enum

**Dimensión diferente de EstadoPago.**

| Estado | Descripción |
|---|---|
| NO_REQUERIDO | El pago no requiere reembolso |
| PENDIENTE | Reembolso pendiente de procesamiento |
| PROCESANDO | Reembolso en procesamiento |
| COMPLETADO | Reembolso completado |
| FALLIDO | El proceso de reembolso falló |

**Ejemplo válido:**

```
Pago:
  estadoPago = APROBADO
  estadoReembolso = COMPLETADO
```

Esto conserva el hecho histórico de que el Pago sí fue aprobado. No cambiar falsamente APROBADO → FALLIDO solo porque posteriormente se realizó una devolución.

---

### 5.9 Protección Temporal del Pago

**Regla de dominio:**

ReservaCapacidad.PROTEGIDA_PAGO representa máximo 10 minutos adicionales de protección de capacidad.

Esto NO significa que una petición HTTP al proveedor pueda durar 10 minutos. El timeout técnico exacto del SDK/HTTP pertenece a infraestructura.

> ⚠️ Ningún IntentoPago ni reintento puede prolongar la protección de capacidad más allá de `paymentProtectionExpiresAt` de la ReservaCapacidad. Los reintentos no reinician ni extienden esa ventana.

---

### 5.10 Pago Aprobado y Confirmación

**Flujo correcto:**

```
Proveedor aprueba Pago
        ↓
Pago = APROBADO
        ↓
ConfirmPurchaseUseCase
        ↓
validar ReservaCapacidad
validar idempotencia
descontar Inventario
crear Pedido
comprometer ReservaCapacidad
finalizar Carrito
        ↓
Compra confirmada
```

**Si ConfirmPurchase falla después de una aprobación externa:**

- no crear un Pedido inconsistente
- iniciar compensación/reembolso
- EstadoPago permanece APROBADO
- EstadoReembolso pasa a PENDIENTE y luego PROCESANDO/COMPLETADO/FALLIDO
- liberar ReservaCapacidad cuando corresponda

> ⚠️ JaldiShop NO utiliza una transacción distribuida con el proveedor externo. La consistencia se obtiene mediante: idempotencia, transacción local y compensación/reembolso.

---

## 6. Pedido

### 6.1 Pedido

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar una compra confirmada y preservar su fotografía histórica.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único |
| numeroPedido | Identificador comercial visible y único |
| estado | EstadoPedido |
| modalidadEntrega | ModalidadEntrega (RECOJO o DELIVERY) |
| fechaAtencion | Snapshot de fecha acordada |
| periodoCapacidad | Snapshot de PeriodoCapacidad acordado |
| datosCliente | DatosClientePedido (Value Object) |
| resumenMonetario | ResumenMonetario (Value Object) |
| direccionEntrega | DireccionEntrega (opcional, solo para DELIVERY) |
| fechaConfirmacion | Fecha de confirmación del pedido |
| fechaActualizacion | Última modificación |

**Relaciones conceptuales:**

- Pedido pertenece exactamente a un Usuario cliente.
- Pedido pertenece exactamente a una Tienda.
- Pedido está respaldado por exactamente un Pago.
- Pedido está respaldado por exactamente una ReservaCapacidad.
- Un Pago puede originar 0..1 Pedido.
- Una ReservaCapacidad puede originar 0..1 Pedido.
- Una misma ReservaCapacidad no puede producir múltiples Pedidos.

**Reglas:**

- Pedido solo nace luego de ConfirmPurchase exitoso.
- Estado inicial: CONFIRMADO.
- `numeroPedido` es único y visible para cliente/comerciante, diferente al identificador interno. El formato exacto se definirá posteriormente en diseño/implementación.
- Pedido conserva snapshots y no depende de modificaciones posteriores del catálogo, usuario, descuento o configuración de capacidad.

---

### 6.2 EstadoPedido

**Clasificación:** Enum

**Estados:**

| Estado | Descripción |
|---|---|
| CONFIRMADO | Pedido confirmado |
| EN_PREPARACION | En preparación |
| LISTO | Listo para recojo/entrega |
| EN_ENTREGA | En camino (solo DELIVERY) |
| COMPLETADO | Completado (terminal) |
| CANCELADO | Cancelado (terminal alternativo) |

**Flujo RECOJO:**

```
CONFIRMADO → EN_PREPARACION → LISTO → COMPLETADO
```

**Flujo DELIVERY:**

```
CONFIRMADO → EN_PREPARACION → LISTO → EN_ENTREGA → COMPLETADO
```

**Reglas:**

- EN_ENTREGA solo es válido para DELIVERY.
- COMPLETADO es terminal. Un Pedido COMPLETADO no puede posteriormente pasar a CANCELADO.
- CANCELADO es terminal. Un Pedido CANCELADO no reanuda el flujo normal.

---

### 6.3 Fecha y Periodo Comprometido

**Regla:**

Pedido debe conservar `fechaAtencion` y `PeriodoCapacidad` como snapshot del compromiso operativo realizado.

**Ejemplo:**

```
fechaAtencion = 2026-09-10
PeriodoCapacidad = 18:00 - 20:00
```

Cambios posteriores de ConfiguracionCapacidad o ExcepcionCapacidad no deben modificar la fecha/periodo comprometido del Pedido.

---

### 6.4 DatosClientePedido

**Clasificación:** Value Object

**Responsabilidad:** Snapshot de los datos de contacto utilizados al confirmar la compra.

**Atributos:**

| Atributo | Descripción |
|---|---|
| nombreCompleto | Nombre completo del cliente |
| telefono | Teléfono de contacto |
| correo | Correo electrónico |

**Reglas:**

- Pedido continúa relacionado con Usuario para identificar qué cuenta realizó la compra.
- Si el Usuario cambia su teléfono/correo posteriormente, el Pedido histórico no cambia.

---

### 6.5 DetallePedido

**Clasificación:** Entidad interna

**Responsabilidad:** Conservar la información comercial de cada variante adquirida en el momento de la confirmación.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador interno | Identificador del detalle |
| referenciaVariante | Referencia conceptual a VarianteProducto (trazabilidad) |
| nombreProducto | Snapshot del nombre del producto |
| nombreVariante | Snapshot del nombre de la variante |
| atributosVariante | Snapshot de atributos |
| cantidad | Cantidad adquirida (>= 1) |
| precioUnitario | Dinero - precio por unidad definitivo |
| subtotal | Dinero - subtotal histórico de la línea |

**Reglas:**

- Pedido contiene 1..N DetallePedido. Un Pedido confirmado no puede estar vacío.
- `precioUnitario` es definitivo dentro del Pedido.
- `subtotal` puede conservarse como snapshot aunque sea calculable.
- nombreProducto, nombreVariante y atributos son fotografías históricas.
- Cambios posteriores de Producto/Variante no modifican DetallePedido.
- La referencia a VarianteProducto sirve para trazabilidad, pero la visualización histórica no debe depender de datos actuales.
- Un snapshot de atributos puede persistirse de forma técnica sin cambiar la naturaleza conceptual de DetallePedido.

---

### 6.6 ResumenMonetario

**Clasificación:** Value Object

**Responsabilidad:** Snapshot económico del Pedido.

**Atributos:**

| Atributo | Descripción |
|---|---|
| subtotalProductos | Dinero (>= 0) |
| descuentoAplicado | Dinero (>= 0) |
| codigoDescuento | Código utilizado (opcional) |
| costoDelivery | Dinero (>= 0) |
| igvIncluido | Dinero (>= 0) |
| total | Dinero (>= 0) |

**Reglas:**

- descuentoAplicado no puede superar subtotalProductos en el MVP.
- El descuento no se aplica al costo de delivery salvo cambio futuro explícito.
- codigoDescuento es opcional; descuentos automáticos pueden dejarlo vacío.

**Fórmula conceptual:**

```
total = subtotalProductos - descuentoAplicado + costoDelivery
```

> ⚠️ igvIncluido NO se suma nuevamente al total. Los precios registrados son finales y el IGV, cuando corresponde, ya está incluido.

---

### 6.7 DireccionEntrega y Modalidad

**ModalidadEntrega:**

| Valor | Descripción |
|---|---|
| RECOJO | Cliente recoje en tienda |
| DELIVERY | Envío a dirección del cliente |

**Reglas:**

- DELIVERY: DireccionEntrega obligatoria.
- RECOJO: DireccionEntrega no requerida. Se utiliza la ubicación actual/configurada de la Tienda para el punto de recojo en el MVP.

**DireccionEntrega — Value Object histórico:**

| Atributo | Descripción |
|---|---|
| direccion | Dirección de entrega |
| referencia | Referencia opcional |
| latitud | Coordenada opcional |
| longitud | Coordenada opcional |

> 💡 Las coordenadas son opcionales. Maps continúa siendo una mejora opcional. No crear Entity Entrega ni DireccionUsuario.

---

### 6.8 HistorialEstadoPedido

**Clasificación:** Entidad interna

**Responsabilidad:** Conservar los cambios relevantes en el estado de atención de un pedido y el momento en que ocurrieron.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador interno | Identificador del registro |
| estado | EstadoPedido |
| fechaCambio | Fecha del cambio |
| usuarioResponsable | Usuario que realizó el cambio (opcional) |
| motivo | Motivo del cambio (opcional) |

**Reglas:**

- Al crear Pedido debe existir inmediatamente un primer historial CONFIRMADO.
- Todo cambio efectivo del EstadoPedido genera un nuevo registro de historial.
- `usuarioResponsable` es opcional cuando el cambio proviene del sistema.
- El motivo puede utilizarse principalmente para cancelaciones u otros cambios relevantes. No duplicar motivoCancelacion directamente en Pedido.

**Conceptualmente:**

- `Pedido.estadoActual` responde rápidamente la situación actual.
- `HistorialEstadoPedido` preserva la evolución completa.

> 💡 SeguimientoPedido es información derivada del estado actual + historial.

---

## 7. Entrega

### 7.1 ModalidadEntrega

**Clasificación:** Enum

**Responsabilidad:** Representar la forma mediante la cual el cliente recibirá una compra confirmada.

> 📌 **Modalidades del MVP:** Para el MVP: `RECOJO` y `DELIVERY`

---

### 7.2 ConfiguracionEntrega

**Clasificación:** Value Object

**Responsabilidad:** Representar las modalidades de entrega habilitadas por una tienda y sus condiciones económicas básicas.

> ⚠️ **Fuera del MVP:** No se considera en esta versión optimización de rutas, seguimiento GPS, gestión de repartidores ni cálculo avanzado por distancia.

---

## 8. Experiencia del Usuario

### 8.1 Favorito

**Clasificación:** Aggregate Root pequeño

**Responsabilidad:** Representar que un Usuario ha marcado un Producto como favorito.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único del favorito |
| fechaCreacion | Fecha en que se creó el favorito |

**Relaciones:**

| Relación | Cardinalidad |
|---|---|
| Favorito → Usuario | exactamente **1** |
| Favorito → Producto | exactamente **1** |
| Usuario → Favorito | **0..N** |
| Producto → Favorito | **0..N** |

**Invariantes:**

- La combinación Usuario + Producto debe ser única.
- Un Usuario no puede marcar dos veces como favorito el mismo Producto.
- Favorito referencia Producto, NO VarianteProducto.
- Favorito no pertenece internamente al aggregate Usuario.
- Favorito no pertenece internamente al aggregate Producto.
- No requiere EstadoFavorito en el MVP.
- Cuando el usuario elimina un favorito, desaparece la relación.
- Si un Producto se desactiva, sus Favoritos no tienen que eliminarse automáticamente.
- Un producto inactivo puede mostrarse posteriormente como no disponible.

**No almacenar dentro de Favorito:** nombreProducto, imagenProducto, precio, stock, estadoProducto, tienda. Estos datos pertenecen a otros conceptos y se consultan cuando sean necesarios.

---

### 8.2 Reseña

**Clasificación:** Aggregate Root pequeño

**Responsabilidad:** Representar la valoración realizada por un cliente sobre un Producto que realmente compró.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único de la reseña |
| calificacion | Valoración de 1 a 5 |
| comentario | Comentario opcional |
| estado | PUBLICADA o OCULTA |
| fechaCreacion | Fecha de creación |
| fechaActualizacion | Última modificación |

**Relaciones:**

| Relación | Cardinalidad |
|---|---|
| Reseña → Usuario | exactamente **1** |
| Reseña → Producto | exactamente **1** |
| Usuario → Reseña | **0..N** |
| Producto → Reseña | **0..N** |

**Reglas:**

- `calificacion` debe encontrarse entre 1 y 5.
- `comentario` es opcional.
- Un Usuario puede tener como máximo una Reseña por Producto.
- Si desea modificar su valoración, debe actualizar la Reseña existente.
- Una Reseña PUBLICADA participa en la reputación visible del Producto.
- Una Reseña OCULTA no debe mostrarse públicamente.
- OCULTA permite preservar el historial cuando la plataforma necesita moderar sin eliminar.
- La desactivación o modificación de un Producto no elimina ni modifica automáticamente las Reseñas históricas.

**Elegibilidad para reseñar:**

> ✅ **Regla aprobada:** Un Usuario solo puede crear una Reseña cuando exista al menos un Pedido COMPLETADO realizado por ese Usuario que contenga dicho Producto.

- No basta con agregar al Carrito.
- No basta con iniciar un Pago.
- No basta con tener un Pedido CONFIRMADO.
- No puede reseñarse mientras el pedido esté EN_PREPARACION, LISTO o EN_ENTREGA.
- El Pedido debe estar COMPLETADO.
- No es necesario crear una relación estructural obligatoria Reseña → Pedido.
- La elegibilidad puede verificarse consultando los pedidos completados del Usuario.

---

### 8.3 Notificacion

**Clasificación:** Aggregate Root

**Responsabilidad:** Representar información persistente destinada a un Usuario sobre eventos relevantes ocurridos dentro de JaldiShop.

**Atributos conceptuales:**

| Atributo | Descripción |
|---|---|
| identificador | Identificador único de la notificación |
| tipo | TipoNotificacion |
| titulo | Título de la notificación |
| mensaje | Contenido del mensaje |
| estado | NO_LEIDA o LEIDA |
| fechaCreacion | Fecha de creación |
| fechaLectura | Fecha en que fue leída (opcional) |

**Relaciones:**

| Relación | Cardinalidad |
|---|---|
| Notificacion → Usuario | exactamente **1** (destinatario) |
| Usuario → Notificacion | **0..N** |

**Reglas:**

- Una Notificacion nueva comienza como NO_LEIDA.
- Cuando el Usuario la consulta/marca como leída puede pasar a LEIDA.
- `fechaLectura` es opcional mientras permanezca NO_LEIDA. Cuando está LEIDA debe existir `fechaLectura`.
- Las notificaciones deben persistir aunque el Usuario no se encuentre conectado.
- El fallo de entrega en tiempo real NO elimina la Notificacion persistida.

**No introducir estados técnicos:** ENVIADA, ENTREGADA, ERROR_WEBSOCKET, FALLIDA_TRANSPORTE. Estos pertenecen a infraestructura, no al dominio.

**Notificacion y WebSocket:**

```
ocurre un evento relevante
        ↓
se crea Notificacion
        ↓
se persiste
        ↓
puede intentarse entrega en tiempo real
        ↓
si Usuario está conectado:
    puede recibirla inmediatamente
si Usuario no está conectado:
    permanece disponible como NO_LEIDA
```

La persistencia de la Notificacion no debe depender del éxito de WebSocket.

**Tipos de Notificacion:**

| Tipo | Interpretación |
|---|---|
| NUEVO_PEDIDO | Dirigida al comerciante cuando se confirma una nueva compra |
| CAMBIO_ESTADO_PEDIDO | Informa al cliente de cambios relevantes en el ciclo del Pedido |
| STOCK_BAJO | Informa al comerciante cuando Inventario alcanza el umbral configurado |
| SISTEMA | Mensajes relevantes propios de la plataforma |

---

## 9. Value Objects Identificados

| Value Object | Responsabilidad |
|---|---|
| **Dinero** | Representar un importe monetario junto con su moneda |
| **AtributoVariante** | Representar una característica y su valor dentro de una variante |
| **PeriodoCapacidad** | Representar una franja horaria dentro de una jornada |
| **ResumenMonetario** | Representar la composición económica de una compra |
| **DireccionEntrega** | Representar el destino utilizado para un delivery |
| **ConfiguracionEntrega** | Representar las condiciones básicas de entrega de una tienda |
| **ConfiguracionTributaria** | Representar las condiciones tributarias básicas aplicables a los precios de una tienda |
| **CodigoDescuento** | Representar un código de promoción cuando la modalidad lo requiera |
| **UbicacionTienda** | Representar la ubicación física de una tienda |
| **DatosClientePedido** | Snapshot de datos de contacto del cliente en un pedido |
| **ReferenciaPagoExterno** | Referencia para relacionar con operaciones del proveedor de pagos |

---

### Dinero

**Atributos:**

| Atributo | Descripción |
|---|---|
| monto | Cantidad monetaria |
| moneda | Moneda (MVP: PEN) |

> 💡 Para la primera versión JaldiShop operará principalmente con soles peruanos, sin acoplar conceptualmente el dominio de forma permanente a una única moneda.

---

### AtributoVariante

**Atributos:**

| Atributo | Descripción |
|---|---|
| nombre | Nombre del atributo (ej: Talla) |
| valor | Valor del atributo (ej: M) |

> 💡 Dentro de una misma VarianteProducto no puede repetirse el nombre de un atributo.

---

### ConfiguracionTributaria

**Responsabilidad:** Representar las condiciones tributarias básicas utilizadas por una tienda para interpretar sus precios.

> 💡 **Precios con IGV Incluido:** Los precios registrados en JaldiShop representan el precio final de venta. Cuando corresponda aplicar IGV, este se considera incluido en dicho precio.

> ⚠️ **Nota:** JaldiShop no agrega automáticamente IGV sobre el precio mostrado ni pretende implementar un sistema contable o tributario completo.

---

### PeriodoCapacidad

**Atributos:**

| Atributo | Descripción |
|---|---|
| horaInicio | Hora de inicio opcional |
| horaFin | Hora de fin opcional |

> 📌 Ambas ausentes = jornada completa. Ambas presentes = franja horaria.

---

### ReferenciaPagoExterno

**Responsabilidad:** Value Object o concepto de valor que permita relacionar el intento con la operación del proveedor sin acoplar el dominio a campos específicos de Mercado Pago/Culqi.

---

## 10. Enumeraciones Identificadas

| Enum | Propósito |
|---|---|
| **Rol** | Distinguir CUSTOMER, MERCHANT y ADMIN |
| **EstadoUsuario** | Representar el estado operativo de un usuario (ACTIVO, SUSPENDIDO) |
| **EstadoTienda** | Representar el estado operativo de una tienda (ACTIVA, INACTIVA, SUSPENDIDA, CERRADA) |
| **EstadoCategoria** | Representar la disponibilidad de una categoría (ACTIVA, INACTIVA) |
| **EstadoProducto** | Representar la disponibilidad comercial de un producto (ACTIVO, INACTIVO) |
| **EstadoVariante** | Representar la disponibilidad comercial de una variante (ACTIVA, INACTIVA) |
| **EstadoPedido** | Representar la etapa actual de atención de un pedido (CONFIRMADO, EN_PREPARACION, LISTO, EN_ENTREGA, COMPLETADO, CANCELADO) |
| **EstadoPago** | Representar la situación actual de un pago (PENDIENTE, PROCESANDO, APROBADO, FALLIDO) |
| **EstadoIntentoPago** | Representar el estado de un intento (INICIADO, PROCESANDO, APROBADO, RECHAZADO, ERROR) |
| **EstadoReembolso** | Representar el estado de reembolso (NO_REQUERIDO, PENDIENTE, PROCESANDO, COMPLETADO, FALLIDO) |
| **EstadoReservaCapacidad** | Representar el estado de una reserva temporal (ACTIVA, PROTEGIDA_PAGO, COMPROMETIDA, EXPIRADA, LIBERADA) |
| **ModalidadEntrega** | Distinguir RECOJO y DELIVERY |
| **MetodoPago** | Representar el medio utilizado para realizar un pago |
| **ProveedorPago** | Representar el proveedor de pagos (MERCADO_PAGO, CULQI) |
| **TipoDescuento** | Distinguir PORCENTAJE y MONTO_FIJO |
| **ModalidadDescuento** | Distinguir AUTOMATICO y CODIGO |
| **EstadoNotificacion** | Distinguir NO_LEIDA y LEIDA |
| **TipoNotificacion** | Clasificar el evento que origina una notificación |
| **EstadoReseña** | Representar si una reseña está PUBLICADA u OCULTA |

> 📌 **Nota:** Los valores definitivos podrán ajustarse durante la revisión del equipo sin modificar la responsabilidad de las entidades.

---

## 11. Conceptos que No Constituyen Entidades

| Concepto | Tratamiento |
|---|---|
| **HistorialPedidos** | Consulta de los pedidos existentes de un usuario |
| **SeguimientoPedido** | Información derivada del estado e historial del pedido |
| **Busqueda** | Funcionalidad de consulta del catálogo |
| **Filtro** | Criterio utilizado durante una búsqueda |
| **Checkout** | Proceso de confirmación de compra |
| **CapacidadDisponible** | Valor calculado |
| **RepetirPedido** | Funcionalidad que reconstruye un carrito desde un pedido anterior |
| **WebSocket** | Mecanismo técnico de comunicación en tiempo real |
| **STOMP** | Protocolo de transporte, no concepto de dominio |
| **Maps** | Integración externa opcional |
| **GPS tracking** | Infraestructura de seguimiento, no dominio |
| **ReputacionProducto** | Valor calculado a partir de Reseñas PUBLICADAS |
| **CostoDelivery** | Importe representado mediante `Dinero` |
| **Impuesto** | Regla/configuración tributaria, no entidad independiente |
| **MetodoPagoGuardado** | Fuera del alcance actual |
| **DireccionUsuario** | Fuera del alcance actual |
| **Entrega** | No requiere ciclo de vida independiente en el MVP |
| **TipoProducto** | Concepto no definido en el MVP |

---

## 12. Reglas e Invariantes Relevantes

> 📌 **Nota:** Las siguientes reglas gobiernan el comportamiento del dominio y deben ser respetadas por cualquier implementación.

| # | Regla |
|---|---|
| 1 | Un pedido solo existe después de una compra confirmada correctamente |
| 2 | El carrito no reserva inventario ni capacidad |
| 3 | El inventario nunca puede resultar negativo |
| 4 | Una variante sin control de inventario no depende del stock para determinar su disponibilidad |
| 5 | Cada pedido consume un cupo de capacidad en el MVP |
| 6 | Una reserva dispone normalmente de 10 minutos para iniciar el pago |
| 7 | Una reserva expirada o liberada no puede confirmar automáticamente una compra |
| 8 | La capacidad disponible descuenta capacidad reservada y comprometida de la capacidad efectiva |
| 9 | Una excepción de capacidad reemplaza la capacidad base del periodo correspondiente |
| 10 | Un pago rechazado no crea un pedido |
| 11 | Un mismo pago aprobado no puede generar múltiples pedidos ni aplicar repetidamente sus efectos |
| 12 | La confirmación debe mantener consistentes pedido, inventario, capacidad y pago |
| 13 | Los datos históricos de un pedido no cambian por modificaciones posteriores del catálogo |
| 14 | Los precios registrados representan el precio final de venta; cuando corresponda IGV, se considera incluido |
| 15 | Una compra puede utilizar como máximo un descuento en el MVP |
| 16 | Un producto solo puede ser reseñado por quien lo haya adquirido en un pedido COMPLETADO |
| 17 | Un cliente no puede registrar repetidamente el mismo producto como favorito |
| 18 | Una entidad solo puede eliminarse físicamente cuando no posea registros o historial asociado; en caso contrario debe conservarse mediante el estado que corresponda |
| 19 | El correo del usuario debe ser único en la plataforma |
| 20 | El slug de tienda debe ser único dentro de la plataforma |
| 21 | El slug de producto debe ser único dentro de su tienda |
| 22 | No deben existir dos categorías activas con el mismo nombre dentro de una misma tienda |
| 23 | Un carrito solo puede contener items de variantes de una misma tienda |
| 24 | ItemCarrito con cantidad 0 debe eliminarse |
| 25 | La misma VarianteProducto no puede aparecer como múltiples items del mismo Carrito |
| 26 | El descuento aplicado al carrito no queda garantizado hasta checkout |
| 27 | El precio referencial del ItemCarrito no garantiza el precio final |
| 28 | Un pedido tiene exactamente un pago aprobado |
| 29 | Un pedido tiene exactamente una reserva de capacidad comprometida |
| 30 | El mismo pago aprobado no puede generar múltiples pedidos |
| 31 | El precio de VarianteProducto debe ser mayor que cero |
| 32 | SKU, cuando existe, debe ser único dentro de la Tienda |
| 33 | No pueden existir dos categorías ACTIVAS con el mismo nombre normalizado dentro de una misma Tienda |
| 34 | El descuento por código tiene prioridad sobre el descuento automático si ambos son aplicables |
| 35 | Una Tienda puede tener como máximo un descuento automático vigente simultáneamente |
| 36 | Un descuento porcentaje no puede superar 100% |
| 37 | horaInicio debe ser menor que horaFin en PeriodoCapacidad |
| 38 | No pueden existir configuraciones de capacidad activas con periodos solapados para la misma Tienda/día |
| 39 | No pueden existir excepciones de capacidad activas con periodos solapados para la misma Tienda y fecha |
| 40 | La capacidad excepcional no puede ser negativa |
| 41 | El descuento de inventario debe ser atómico y condicional; si no hay stock suficiente, no se modifica |
| 42 | La restauración de inventario por cancelación debe ser idempotente |
| 43 | La liberación de capacidad por cancelación debe ser idempotente |
| 44 | Un Pago puede tener como máximo una aprobación financiera válida |
| 45 | Un IntentoPago rechazado no implica automáticamente Pago = FALLIDO si todavía pueden ejecutarse más intentos |
| 46 | Los reintentos de pago no extienden la protección de capacidad más allá de proteccionPagoExpiraEn |
| 47 | Pago APROBADO no crea automáticamente un Pedido |
| 48 | Un Pedido debe estar respaldado por exactamente un Pago aprobado |
| 49 | Un Pedido debe estar respaldado por exactamente una ReservaCapacidad comprometida |
| 50 | montoReembolsado >= 0 y <= monto del Pago |
| 51 | Un Pedido confirmado no puede estar vacío; debe contener al menos un DetallePedido |
| 52 | cantidad en DetallePedido debe ser > 0 |
| 53 | subtotalProductos, descuentoAplicado, costoDelivery, igvIncluido y total en ResumenMonetario deben ser >= 0 |
| 54 | descuentoAplicado no puede superar subtotalProductos en el MVP |
| 55 | ConfirmPurchase puede reintentarse sin duplicar efectos (idempotencia) |
| 56 | Cancelar un Pedido puede reintentarse sin duplicar restauración de stock, capacidad o reembolso |

---

## 13. Cancelación: Capacidad e Inventario

> 📌 **Regla aprobada:** La cancelación de pedidos tiene efectos diferenciados según el estado en que se encuentre el pedido.

### Cancelación antes de EN_PREPARACION

Si un Pedido es cancelado mientras permanece en **CONFIRMADO**, antes de iniciar preparación:

1. La ReservaCapacidad COMPROMETIDA pasa a **LIBERADA**.
2. El inventario descontado al confirmar la compra debe **restaurarse**.
3. El proceso de reembolso se maneja mediante `RefundStatus` del Pago según corresponda.

### Cancelación desde EN_PREPARACION en adelante

Una vez que el Pedido entra en **EN_PREPARACION** o un estado posterior:

- **NO** se restaura automáticamente el inventario.
- **NO** se libera automáticamente la capacidad.
- La capacidad operativa pudo haber sido consumida internamente por el negocio.
- Cualquier compensación excepcional quedaría fuera de la regla automática del MVP.

### Invariantes de cancelación

- Si un Pedido todavía está CONFIRMADO y una cancelación válida devuelve el cupo, ReservaCapacidad pasa de COMPROMETIDA a LIBERADA.
- La liberación/restauración debe ser **idempotente**: una misma cancelación no puede devolver existencias varias veces.
- El mecanismo técnico de concurrencia/idempotencia se definirá en arquitectura/diseño técnico.

---

## 14. Fuera del Modelo de Dominio MVP

> ⚠️ **Límites del MVP:** Quedan fuera del modelo funcional actual:

- Reserva de inventario (el inventario se descuenta directamente, no se reserva anticipadamente)
- Kardex avanzado y movimientos históricos de inventario
- Múltiples almacenes
- Lotes
- Proveedores
- Transferencias de inventario
- Capacidad ponderada por producto
- Capacidad por múltiples recursos o estaciones
- Múltiples imágenes de producto y galería avanzada
- EAV complejo de atributos (cada tipo de atributo no es una entidad independiente)
- Motor avanzado de promociones y descuentos acumulables
- Stacking de promociones
- Múltiples descuentos simultáneos
- Métodos de pago almacenados
- Reserva anticipada de inventario
- Seguimiento GPS
- Repartidores
- Optimización de rutas
- Cobertura geográfica avanzada
- Facturación electrónica completa
- Respuestas e imágenes en reseñas
- Moderación avanzada de reseñas
- Historial de búsquedas
- Subcategorías
- Capacidad slots como entidad independiente (salvo que posteriormente se demuestre necesario)

---

## 15. Relaciones del Dominio

### 15.1 Usuario y Tienda

| Relación | Descripción |
|---|---|
| Usuario → Tienda | Un Usuario puede administrar **0..1** Tienda en el MVP. |
| Tienda → Usuario | Cada Tienda tiene exactamente **un** Usuario responsable con rol COMERCIANTE. |

> 💡 Un Usuario puede existir sin Tienda. Usuario y Tienda son entidades independientes.

---

### 15.2 Tienda, Categoría, Producto y VarianteProducto

```
Tienda 1——0..N Categoria
Categoria 1——0..N Producto
Producto 1——1..N VarianteProducto
```

| Relación | Cardinalidad |
|---|---|
| Tienda → Categoria | **0..N** Categorias por Tienda |
| Categoria → Tienda | exactamente **1** Tienda |
| Categoria → Producto | **0..N** Productos por Categoria |
| Producto → Categoria | exactamente **1** Categoria en el MVP |
| Producto → VarianteProducto | **1..N** Variantes (al menos una) |
| VarianteProducto → Producto | exactamente **1** Producto |

> ⚠️ Todo producto vendible debe disponer al menos de una variante, incluso si esta es una variante estándar no visible para el cliente.

---

### 15.3 VarianteProducto e Inventario

| Relación | Descripción |
|---|---|
| VarianteProducto → Inventario | **0..1** Inventario (no todas las variantes requieren control de stock) |
| Inventario → VarianteProducto | exactamente **1** VarianteProducto |

> 💡 La disponibilidad comercial, la disponibilidad por inventario y la disponibilidad por capacidad son conceptos diferentes.

---

### 15.4 Usuario, Tienda y Carrito

```
Usuario 1——0..N Carrito
Carrito N——1 Tienda
```

| Relación | Cardinalidad |
|---|---|
| Usuario → Carrito | **0..N** Carritos a lo largo del tiempo |
| Carrito → Usuario | exactamente **1** Usuario |
| Carrito → Tienda | exactamente **1** Tienda |

> ⚠️ Un Usuario puede tener como máximo **un** Carrito activo por Tienda. Un Carrito nunca puede contener productos de distintas tiendas.

---

### 15.5 Carrito e ItemCarrito

| Relación | Descripción |
|---|---|
| Carrito → ItemCarrito | **0..N** ItemCarrito |
| ItemCarrito → VarianteProducto | exactamente **1** VarianteProducto |

**Invariantes:**
- Una misma VarianteProducto no puede aparecer en varios items del mismo Carrito.
- La cantidad de un ItemCarrito debe ser mayor que cero.

> 💡 Los precios mostrados en Carrito son referenciales y deben revalidarse durante checkout. El Carrito no reserva inventario ni capacidad.

---

### 15.6 Descuento

| Relación | Descripción |
|---|---|
| Tienda → Descuento | **0..N** Descuentos por Tienda |
| Descuento → Tienda | exactamente **1** Tienda |
| Carrito → Descuento | **0..1** Descuento aplicado en el MVP |

> ⚠️ El descuento aplicado debe pertenecer a la misma Tienda que el Carrito. Descuento no forma parte internamente del Carrito. CodigoDescuento continúa siendo Value Object cuando la modalidad del descuento utiliza código.

---

### 15.7 ConfiguracionCapacidad y ExcepcionCapacidad

```
Tienda 1——0..N ConfiguracionCapacidad
Tienda 1——0..N ExcepcionCapacidad
```

| Entidad | Descripción |
|---|---|
| ConfiguracionCapacidad | Regla habitual/base de capacidad. Contiene PeriodoCapacidad como Value Object. |
| ExcepcionCapacidad | Reemplaza la capacidad base para un periodo específico. Contiene PeriodoCapacidad. |

> 💡 No existe dependencia obligatoria directa entre ExcepcionCapacidad y ConfiguracionCapacidad. Ambas se resuelven mediante Tienda + periodo.

---

### 15.8 ReservaCapacidad

| Relación | Descripción |
|---|---|
| Tienda → ReservaCapacidad | **0..N** Reservas por Tienda |
| ReservaCapacidad → PeriodoCapacidad | contiene el periodo consumido |

**Estados y consumo de capacidad:**

| Estado | Consume capacidad | Vigencia |
|---|---|---|
| ACTIVA | Sí | 10 minutos máximo |
| PROTEGIDA_PAGO | Sí | 10 minutos adicionales |
| COMPROMETIDA | Sí | hasta completarse o cancelarse |
| EXPIRADA | No | — |
| LIBERADA | No | — |

> ⚠️ No debe existir más de una reserva ACTIVA o PROTEGIDA_PAGO simultánea para la misma intención de checkout.

**Fórmula:**
```
Capacidad disponible = Capacidad efectiva - Capacidad reservada - Capacidad comprometida
```

En el MVP: **1 pedido = 1 cupo**.

---

### 15.9 Carrito, ReservaCapacidad y Pago

| Relación | Descripción |
|---|---|
| Carrito → Pago | **0..N** Pagos históricamente (no más de uno activo simultáneo) |
| Pago → ReservaCapacidad | Association (cada pago vinculado a la reserva que protegía la compra) |

> ⚠️ ReservaCapacidad y Pago son entidades independientes; ninguna pertenece internamente a la otra.

---

### 15.10 Pago e IntentoPago

| Relación | Descripción |
|---|---|
| Pago → IntentoPago | **1..N** Intentos |
| IntentoPago | Objeto interno, sin ciclo de vida independiente |

**Estados de Pago:**

| EstadoPago | Descripción |
|---|---|
| PENDIENTE | El Pago fue creado pero todavía no se encuentra ejecutando un intento activo |
| PROCESANDO | Existe un intento de pago en procesamiento |
| APROBADO | El proveedor confirmó una aprobación financiera válida |
| FALLIDO | El proceso financiero terminó definitivamente sin obtener una aprobación válida |

**EstadoReembolso:**

| Valor | Descripción |
|---|---|
| NO_REQUERIDO | El pago no requiere reembolso |
| PENDIENTE | Reembolso pendiente de procesamiento |
| PROCESANDO | Reembolso en procesamiento |
| COMPLETADO | Reembolso completado |
| FALLIDO | El proceso de reembolso falló |

> 💡 Pago APROBADO con posterior reembolso COMPLETADO conserva el hecho histórico de aprobación. No modificar falsamente APROBADO → FALLIDO. No crear entidad Reembolso en el MVP.

---

### 15.11 Pago y Pedido

| Relación | Descripción |
|---|---|
| Pago → Pedido | **0..1** Pedido |
| Pedido → Pago | exactamente **1** Pago aprobado |

> ⚠️ Un Pago aprobado no crea automáticamente un Pedido: primero debe completarse correctamente el proceso de confirmación de compra. Si la confirmación interna falla después de que el proveedor aprobó el pago, no se crea Pedido y se inicia el proceso de compensación/void/refund.

---

### 15.12 ReservaCapacidad y Pedido

| Relación | Descripción |
|---|---|
| ReservaCapacidad → Pedido | **0..1** Pedido |
| Pedido → ReservaCapacidad | Association |

> ⚠️ Una ReservaCapacidad en estado COMPROMETIDA debe estar asociada a exactamente un Pedido. Confirmar el Pedido cambia el cupo de reservado a comprometido; no libera capacidad.

---

### 15.13 Usuario, Tienda y Pedido

```
Usuario 1——0..N Pedido
Tienda 1——0..N Pedido
```

| Relación | Cardinalidad |
|---|---|
| Usuario → Pedido | **0..N** Pedidos del cliente |
| Tienda → Pedido | **0..N** Pedidos recibidos |
| Pedido → Usuario | exactamente **1** Usuario cliente |
| Pedido → Tienda | exactamente **1** Tienda |

> ⚠️ Un Pedido nunca contiene productos de distintas tiendas.

---

### 15.14 Pedido y DetallePedido

| Relación | Descripción |
|---|---|
| Pedido → DetallePedido | **1..N** DetallePedido |
| DetallePedido → VarianteProducto | Originado a partir de exactamente **1** VarianteProducto |

> 💡 DetallePedido conserva un snapshot histórico. Los cambios futuros en Producto o VarianteProducto no deben modificar un Pedido ya confirmado.

---

### 15.15 Pedido e HistorialEstadoPedido

| Relación | Descripción |
|---|---|
| Pedido → HistorialEstadoPedido | **1..N** entradas (el primer registro corresponde a CONFIRMADO) |

**Flujo de estados:**

```
RECOJO:
CONFIRMADO → EN_PREPARACION → LISTO → COMPLETADO

DELIVERY:
CONFIRMADO → EN_PREPARACION → LISTO → EN_ENTREGA → COMPLETADO

CANCELADO (terminal alternativo)
```

> ⚠️ EN_ENTREGA solo aplica para DELIVERY. COMPLETADO y CANCELADO son estados terminales. La cancelación operativa no modifica falsamente un Pago APROBADO a FALLIDO.

---

### 15.16 Entrega

| Concepto | Tratamiento |
|---|---|
| ModalidadEntrega | Enum: RECOJO o DELIVERY. Cada Pedido posee exactamente una. |
| DireccionEntrega | Value Object histórico. Obligatorio cuando ModalidadEntrega = DELIVERY. Para RECOJO se utiliza la información de Tienda. |
| ConfiguracionEntrega | Value Object de Tienda. |

> ⚠️ No crear entidad Entrega en el MVP. No crear DireccionUsuario.

---

### 15.17 ResumenMonetario

| Relación | Descripción |
|---|---|
| Pedido → ResumenMonetario | exactamente **1** (Value Object histórico) |

> 💡 Debe preservar subtotal, descuento aplicado, costo de delivery, total e información tributaria. Una modificación posterior de precio, descuento o configuración de entrega no modifica pedidos históricos.

---

### 15.18 Favorito

| Relación | Descripción |
|---|---|
| Usuario → Favorito | **0..N** Favoritos |
| Favorito → Usuario | exactamente **1** |
| Favorito → Producto | exactamente **1** |
| Producto → Favorito | **0..N** |

> ⚠️ La combinación Usuario + Producto debe ser única. Favorito referencia Producto, no VarianteProducto. No establecer relación directa obligatoria Favorito → Tienda.

---

### 15.19 Reseña

| Relación | Descripción |
|---|---|
| Usuario → Reseña | **0..N** Reseñas |
| Reseña → Usuario | exactamente **1** |
| Reseña → Producto | exactamente **1** |
| Producto → Reseña | **0..N** |

> ⚠️ Máximo una Reseña por combinación Usuario + Producto. Para ser elegible, el usuario debe haber adquirido el producto en un Pedido COMPLETADO. No establecer relación obligatoria Reseña → Pedido.

---

### 15.20 Notificacion

| Relación | Descripción |
|---|---|
| Usuario → Notificacion | **0..N** Notificaciones |
| Notificacion → Usuario | exactamente **1** Usuario destinatario |

> ⚠️ No obligar Notificacion a pertenecer a Pedido, porque también puede originarse desde inventario u otros eventos. WebSocket/STOMP es infraestructura y no forma parte del modelo de entidades. La Notificacion debe poder persistir aunque el usuario esté desconectado.

---

## 16. Agregados del Dominio

> 💡 **Concepto:** Un Aggregate representa una frontera de consistencia. El Aggregate Root es el único punto de entrada para modificar las entidades y Value Objects internos del agregado. Una relación entre dos entidades **no implica** que pertenezcan al mismo Aggregate.

---

### 16.1 Usuario — Aggregate Root

**Contiene conceptualmente:**
- Rol
- EstadoUsuario

**No contiene:** Tienda, Favorito, Reseña ni Notificacion.

---

### 16.2 Tienda — Aggregate Root

**Contiene:**
- ConfiguracionEntrega
- ConfiguracionTributaria

**No contiene:** Categoria, Producto, Inventario, Capacidad, Carrito, Pago ni Pedido.

---

### 16.3 Categoria — Aggregate Root

Aggregate pequeño e independiente.

**Referencia:** Tienda

**Ciclo de vida propio:** creación, cambio de nombre, activación, desactivación y eliminación cuando las reglas de historial lo permitan.

---

### 16.4 Producto — Aggregate Root

**Contiene:**
- VarianteProducto como entidad interna
- AtributoVariante como Value Object de VarianteProducto

**Protege:** la invariante de poseer al menos una variante vendible.

**No contiene:** Inventario.

---

### 16.5 Inventario — Aggregate Root

Aggregate independiente.

**Referencia:** VarianteProducto

**Justificación:** Su separación se justifica por su ciclo de vida propio y por las necesidades de actualización/concurrencia del stock.

---

### 16.6 Carrito — Aggregate Root

**Contiene:**
- ItemCarrito

**Protege:**
- unicidad de variante dentro del carrito
- cantidades mayores a cero
- pertenencia de todos los items a la misma Tienda
- reglas básicas de descuento del carrito

**No contiene:** Descuento.

---

### 16.7 Descuento — Aggregate Root

Aggregate independiente.

**Referencia:** Tienda

**Contiene:** CodigoDescuento, TipoDescuento y ModalidadDescuento.

---

### 16.8 ConfiguracionCapacidad — Aggregate Root

Aggregate independiente.

**Contiene:** PeriodoCapacidad como Value Object.

---

### 16.9 ExcepcionCapacidad — Aggregate Root

Aggregate independiente.

**Contiene:** PeriodoCapacidad como Value Object.

**No pertenece internamente a** ConfiguracionCapacidad.

---

### 16.10 ReservaCapacidad — Aggregate Root

Aggregate independiente.

**Contiene:**
- PeriodoCapacidad
- EstadoReservaCapacidad

**Protege su propio ciclo:**

```
ACTIVA → PROTEGIDA_PAGO → COMPROMETIDA
         ↓                    ↓
      EXPIRADA            LIBERADA
```

**No contiene:** Pago ni Pedido.

---

### 16.11 Pago — Aggregate Root

**Contiene:**
- IntentoPago

**Protege:**
- evitar múltiples aprobaciones válidas
- cambios permitidos de EstadoPago
- RefundStatus

**No contiene:** ReservaCapacidad ni Pedido.

---

### 16.12 Pedido — Aggregate Root

**Contiene:**
- DetallePedido
- HistorialEstadoPedido
- ResumenMonetario
- DireccionEntrega
- ModalidadEntrega

**Protege:**
- composición histórica de la compra
- transición válida de estados
- historial de estados
- inmutabilidad de sus datos confirmados

**No contiene:** Pago, ReservaCapacidad, Inventario ni Tienda aunque los referencie.

---

### 16.13 Favorito — Aggregate Root pequeño

**Atributos:**
- identificador
- fechaCreacion

**Referencias:**
- Usuario
- Producto

**Protege:** la unicidad Usuario + Producto.

> 💡 Aggregate secundario y deliberadamente simple. No almacenar nombreProducto, imagenProducto, precio, stock, estadoProducto ni tienda dentro de Favorito.

---

### 16.14 Reseña — Aggregate Root pequeño

**Atributos:**
- identificador
- calificacion (1-5)
- comentario (opcional)
- estado (PUBLICADA, OCULTA)
- fechaCreacion
- fechaActualizacion

**Referencias:**
- Usuario
- Producto

**Protege:**
- una reseña por Usuario + Producto
- reglas propias de publicación/estado

> 💡 La elegibilidad de compra puede requer consultar Pedido sin incorporar Pedido al aggregate. La calificación debe ser entre 1 y 5.

---

### 16.15 Notificacion — Aggregate Root

**Atributos:**
- identificador
- tipo (TipoNotificacion)
- titulo
- mensaje
- estado (NO_LEIDA, LEIDA)
- fechaCreacion
- fechaLectura (opcional)

**Referencia:** Usuario destinatario

**Ciclo propio:**
- creación
- estado de lectura
- consulta

**TipoNotificacion:**
- NUEVO_PEDIDO
- CAMBIO_ESTADO_PEDIDO
- STOCK_BAJO
- SISTEMA

**No pertenece internamente a:** Usuario ni Pedido.

---

## 17. Coordinación Entre Agregados

> 💡 Las operaciones de negocio pueden coordinar varios Aggregate Roots sin necesidad de fusionarlos en uno solo.

### ConfirmPurchaseUseCase

Coordina conceptualmente los siguientes Aggregate Roots:

- **Pago**
- **ReservaCapacidad**
- **Inventario**
- **Pedido**
- **Carrito**

**Precondiciones:**

1. Pago = APROBADO
2. No existe Pedido para ese Pago
3. ReservaCapacidad = PROTEGIDA_PAGO
4. La protección temporal sigue vigente
5. Las variantes controladas tienen inventario suficiente

**Operación local conceptual:**

1. Descontar todos los Inventarios necesarios (de forma atómica y condicional)
2. Crear Pedido
3. Crear sus DetallePedido
4. Crear primer HistorialEstadoPedido = CONFIRMADO
5. Asociar Pedido con Pago
6. Asociar Pedido con ReservaCapacidad
7. ReservaCapacidad → COMPROMETIDA
8. Finalizar/vaciar el Carrito
9. Confirmar la operación local

**Si falla cualquier paso local:**

- Revertir la transacción local
- No dejar Pedido parcial
- No dejar descuentos parciales de Inventario
- No dejar ReservaCapacidad incorrectamente COMPROMETIDA

**Si el Pago ya fue aprobado externamente:**

- Iniciar compensación/reembolso después del fallo local.

> ⚠️ La coordinación de múltiples Aggregate Roots NO implica que pertenezcan al mismo Aggregate. Cada uno mantiene su propia frontera de consistencia y ciclo de vida independiente.

---

### Cancelación Temprana

**Regla APROBADA:**

Si un Pedido se cancela mientras permanece en CONFIRMADO, antes de EN_PREPARACION:

1. Pedido → CANCELADO
2. ReservaCapacidad COMPROMETIDA → LIBERADA
3. Restaurar el inventario previamente descontado
4. Iniciar reembolso/compensación cuando corresponda
5. Registrar HistorialEstadoPedido = CANCELADO con motivo

**Idempotencia:**

Todo debe realizarse de forma idempotente. Una misma cancelación no puede:

- Restaurar inventario dos veces
- Liberar capacidad dos veces
- Iniciar múltiples reembolsos por error

---

### Cancelación desde EN_PREPARACION en adelante

Desde EN_PREPARACION en adelante:

- Una eventual cancelación NO restaura automáticamente inventario
- NO libera automáticamente capacidad

**Motivo:** El negocio pudo haber consumido materiales y capacidad operativa. La política de reembolso correspondiente puede ser parcial según las reglas ya definidas. No agregar lógica avanzada de recuperación de insumos al MVP.

---

### Invariantes de Idempotencia

- Un mismo Pago aprobado puede producir máximo un Pedido.
- Una misma ReservaCapacidad puede respaldar máximo un Pedido.
- ConfirmPurchase puede recibirse/reintentarse varias veces sin duplicar efectos.
- Cancelar un Pedido puede reintentarse sin duplicar restauración de stock/capacidad/reembolso.
- Un callback/webhook repetido del proveedor no debe generar múltiples Pedidos.

> 📌 Los mecanismos técnicos concretos (Idempotency-Key HTTP, Redis, locks distribuidos) pertenecen a arquitectura/infraestructura, no al modelo de dominio.

---

## 18. Resumen de Conceptos

### Aggregate Roots principales

Usuario, Tienda, Categoria, Producto, Inventario, Carrito, Descuento, ConfiguracionCapacidad, ExcepcionCapacidad, ReservaCapacidad, Pago y Pedido.

### Aggregate Roots secundarios

Favorito, Reseña y Notificacion.

### Entidades internas

- VarianteProducto → Producto
- ItemCarrito → Carrito
- IntentoPago → Pago
- DetallePedido → Pedido
- HistorialEstadoPedido → Pedido

### Value Objects

Dinero, AtributoVariante, UbicacionTienda, ConfiguracionEntrega, ConfiguracionTributaria, CodigoDescuento, PeriodoCapacidad, ReferenciaPagoExterno, DatosClientePedido, ResumenMonetario y DireccionEntrega.

---

## 19. Estado del Documento

| Aspecto | Estado |
|---|---|
| Identificación de entidades y Value Objects | ✅ Completado |
| Responsabilidades y invariantes | ✅ Completado |
| Enumeraciones | ✅ Completado |
| Relaciones del Dominio | ✅ Completado |
| Agregados del Dominio | ✅ Completado |
| Coordinación entre Agregados | ✅ Completado |
| Catálogo (Categoría, Producto, Variante) | ✅ Cerrado |
| Inventario y estrategia de concurrencia | ✅ Cerrado |
| Carrito e ItemCarrito | ✅ Cerrado |
| Descuento (reglas y prioridad) | ✅ Cerrado |
| Capacidad y estrategia de concurrencia | ✅ Cerrado |
| Pago, EstadoPago, IntentoPago, EstadoIntentoPago | ✅ Cerrado |
| Estados de Reembolso/Compensación | ✅ Cerrado |
| Pedido, DetallePedido, HistorialEstadoPedido | ✅ Cerrado |
| ResumenMonetario, DatosClientePedido, DireccionEntrega | ✅ Cerrado |
| ConfirmPurchaseUseCase e idempotencia | ✅ Cerrado |
| Cancelación temprana e idempotencia | ✅ Cerrado |
| Favorito, Reseña, Notificacion | 🔄 Revisión pendiente |
| Revisión final de normalización y modelo ER | 🔄 Pendiente |
| Definición de PK/FK físicas, índices, constraints | 🔄 Pendiente |
| Definición de nombres finales de tablas/columnas | 🔄 Pendiente |
| Posterior mapeo JPA y migración Flyway | 🔄 Pendiente |

**Decisiones tomadas en esta versión (v1.5):**

- Pago: estados definitivos (PENDIENTE, PROCESANDO, APROBADO, FALLIDO), atributos completos incluyendo montoReembolsado y referenciaReembolso.
- IntentoPago: numeroIntento, EstadoIntentoPago (INICIADO, PROCESANDO, APROBADO, RECHAZADO, ERROR).
- EstadoReembolso: dimensión separada de EstadoPago (NO_REQUERIDO, PENDIENTE, PROCESANDO, COMPLETADO, FALLIDO).
- Protección temporal: reintentos no extienden protección de capacidad.
- Pago aprobado ≠ Pedido automático: flujo correcto documentado.
- Pedido: relaciones confirmadas con Pago, ReservaCapacidad, Usuario y Tienda.
- DetallePedido: referencia conceptual a VarianteProducto para trazabilidad.
- ResumenMonetario: restricciones de importes y fórmula.
- ConfirmPurchaseUseCase: precondiciones, flujo completo, manejo de fallos.
- Idempotencia conceptual: invariance de confirmación y cancelación.
- Cancelación temprana: efectos sobre Pedido, ReservaCapacidad, Inventario y reembolso.

**Decisiones de arquitectura técnica pendientes:**

- Detalles de implementación de transacciones (@Transactional, eventos de dominio, etc.)
- Mecanismos técnicos de idempotencia (Idempotency-Key HTTP, Redis, locks distribuidos)
- Queries concretas de bloqueo pesimista y actualización condicional
- Timeouts técnicos del SDK/HTTP del proveedor de pagos
- Posterior mapeo JPA y migración Flyway inicial

> 📌 El núcleo transaccional del dominio se encuentra definido. El modelo queda pendiente de revisión final y traducción al modelo entidad-relación físico.

---

[⬅ Alcance del MVP](./alcance-mvp.md) | [🏠 Volver al Índice General](../../README.md) | [Reglas de Negocio ➡](./reglas-negocio.md)
