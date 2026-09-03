# Modelo de Dominio

### JaldiShop — Conceptos y Responsabilidades del Dominio v1.0

[![Estado](https://img.shields.io/badge/Estado-En%20Revisión-orange?style=for-the-badge&logo=checkmarx&logoColor=white)](./modelo-dominio.md)
[![Versión](https://img.shields.io/badge/Versión-v1.0-blue?style=for-the-badge)](./modelo-dominio.md)
[![Fase](https://img.shields.io/badge/Fase-Sprint_02-orange?style=for-the-badge)](../06-scrum/sprint-02.md)

---

`📍 Docs` > `03-Requisitos` > **Modelo de Dominio**  
[⬅ Alcance del MVP](./alcance-mvp.md) | [🏠 Índice General](../../README.md) | [Reglas de Negocio ➡](./reglas-negocio.md)

---

## 1. Propósito

> 📌 **Nota:** Este documento identifica los principales conceptos del dominio de **JaldiShop** y define sus responsabilidades dentro del negocio. El modelo se mantiene independiente de decisiones técnicas como frameworks, base de datos, endpoints, DTO, repositorios o mecanismos de comunicación.

Las relaciones formales entre conceptos y la identificación de agregados quedan pendientes para la revisión y desarrollo correspondiente del equipo.

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

**Clasificación:** Entidad

**Responsabilidad:** Representar a una persona registrada en JaldiShop, manteniendo su identidad, estado y los roles mediante los cuales participa en la plataforma.

> 📌 **Nota:** Un usuario puede participar mediante los roles habilitados en su cuenta, como cliente, comerciante o administrador. `Rol` y `EstadoUsuario` son conceptos simples del estado del usuario y no entidades independientes del MVP.

---

### 3.2 Tienda

**Clasificación:** Entidad

**Responsabilidad:** Representar a la MYPE que opera dentro de JaldiShop, manteniendo su información comercial, estado y configuración general de operación.

> ⚠️ **Alcance del MVP:** Para el MVP se considera un modelo simple de un comerciante con una única tienda. La tienda no debe asumir directamente las responsabilidades propias del catálogo, inventario, capacidad, pedidos o pagos.

---

### 3.3 Categoría

**Clasificación:** Entidad

**Responsabilidad:** Organizar los productos ofrecidos por una tienda y permitir al comerciante gestionar su clasificación comercial.

> 📌 **Nota:** Las categorías son definidas dentro del contexto de cada tienda. Para el MVP no se consideran subcategorías.

---

### 3.4 Producto

**Clasificación:** Entidad

**Responsabilidad:** Representar el concepto comercial que una tienda ofrece a sus clientes, manteniendo su información general y disponibilidad comercial.

> 💡 **Responsabilidad:** El producto representa la oferta general y no es responsable directamente del control de inventario ni de la capacidad operativa.

---

### 3.5 VarianteProducto

**Clasificación:** Entidad

**Responsabilidad:** Representar una presentación concreta y vendible de un producto, diferenciada mediante atributos y condiciones comerciales propias.

> 📌 **Nota:** Todo producto vendible tendrá al menos una variante. Cuando no existan opciones visibles al cliente, podrá utilizarse una variante estándar. No todas las variantes requieren control de inventario.

---

### 3.6 Inventario

**Clasificación:** Entidad

**Responsabilidad:** Mantener la cantidad disponible de una variante que utiliza control de existencias, permitiendo validar disponibilidad y evitar stock negativo.

> ⚠️ **Alcance del MVP:** El MVP mantiene directamente las existencias actuales y permite reflejar ventas confirmadas y ajustes del comerciante. No se considera un sistema avanzado de movimientos, lotes, almacenes, proveedores, transferencias o reservas de inventario.

---

### 3.7 Carrito

**Clasificación:** Entidad

**Responsabilidad:** Representar la intención de compra activa de un cliente dentro de una tienda, manteniendo las variantes seleccionadas y sus cantidades antes de iniciar la confirmación de compra.

> 💡 **Características:** El carrito no representa una compra confirmada, no reserva inventario ni capacidad y utiliza valores comerciales que deben revalidarse durante el checkout.

#### ItemCarrito

**Clasificación:** Objeto interno

**Responsabilidad:** Representar una variante seleccionada por el cliente junto con la cantidad deseada y la información comercial necesaria para mostrar su valor dentro del carrito.

---

### 3.8 Descuento

**Clasificación:** Entidad secundaria

**Responsabilidad:** Representar una promoción comercial creada por una tienda que permite reducir el valor de una compra según una condición básica, vigencia y modalidad definida.

> 📌 **Nota:** Para el MVP se consideran descuentos porcentuales o de monto fijo. Como simplificación, una compra puede utilizar como máximo un descuento a la vez.

------------------------------------------------------------------------

## 4. Capacidad Operativa

### 4.1 ConfiguracionCapacidad

**Clasificación:** Entidad

**Responsabilidad:** Representar las condiciones operativas base mediante las cuales una tienda establece cuántos pedidos puede atender dentro de un determinado periodo.

> ⚡ **Regla del MVP:** La capacidad puede configurarse por día o mediante franjas horarias configurables. Para el MVP: **1 pedido = 1 cupo**

---

### 4.2 PeriodoCapacidad

**Clasificación:** Value Object

**Responsabilidad:** Representar el intervalo operacional para el cual se configura, consulta o consume capacidad.

> 📌 **Nota:** Puede representar un día completo o una franja horaria. No debe confundirse con el tiempo de vigencia de una reserva.

---

### 4.3 ExcepcionCapacidad

**Clasificación:** Entidad

**Responsabilidad:** Representar una modificación temporal de la capacidad operativa habitual de una tienda para un periodo específico.

> 💡 **Comportamiento:** Para el MVP, cuando existe una excepción para un periodo, su valor reemplaza la capacidad base:

```text
Capacidad efectiva = excepción si existe; de lo contrario, capacidad base
```

---

### 4.4 ReservaCapacidad

**Clasificación:** Entidad

**Responsabilidad:** Representar la retención temporal de un cupo operativo durante el proceso de compra, evitando que dicho cupo sea utilizado simultáneamente por otra compra mientras la reserva permanezca válida.

> ⏱️ **Hold de 10 Minutos:** Cada reserva consume un cupo. La reserva normal dispone de **10 minutos para que el cliente inicie el pago**. Si el pago se inicia válidamente, podrá mantenerse protegida temporalmente mientras se procesa la operación.

> ⚠️ **Cálculo de Disponibilidad:** `CapacidadDisponible` no constituye una entidad. Es un valor calculado:

```text
Capacidad disponible = Capacidad efectiva - Capacidad reservada - Capacidad comprometida
```

---

## 5. Pago

### 5.1 Pago

**Clasificación:** Entidad

**Responsabilidad:** Representar el proceso financiero mediante el cual se intenta cobrar el importe correspondiente a una compra, conservando su estado y los intentos realizados hasta obtener un resultado definitivo.

> 💡 **Reglas del Pago:**
> - Un pago puede requerir varios intentos
> - Un pago rechazado no genera un pedido
> - Un mismo pago aprobado no puede generar más de un pedido ni producir repetidamente los efectos de una compra confirmada

---

### 5.2 IntentoPago

**Clasificación:** Objeto interno

**Responsabilidad:** Representar una operación individual realizada para intentar completar un pago y conservar su resultado.

---

### 5.3 MetodoPago

**Clasificación:** Enum o Value Object simple

**Responsabilidad:** Representar el medio utilizado por el cliente para realizar un pago.

> 📌 **Nota:** Las modalidades concretas dependerán del proveedor seleccionado. JaldiShop no debe almacenar información sensible de tarjetas u otros medios financieros.

---

## 6. Pedido

### 6.1 Pedido

**Clasificación:** Entidad

**Responsabilidad:** Representar una compra confirmada realizada en JaldiShop, conservando de forma histórica los productos adquiridos, importes aplicados, modalidad de entrega y evolución de su atención.

> 🔄 **Estado Inicial:** Un pedido nace únicamente después de una confirmación exitosa. Su estado inicial es `CONFIRMADO`; `PENDIENTE_PAGO` no es un estado de Pedido.

> 📌 **Nota:** La composición y los importes de un pedido confirmado no se modifican en el MVP.

---

### 6.2 DetallePedido

**Clasificación:** Objeto interno

**Responsabilidad:** Conservar la información comercial correspondiente a cada variante adquirida en el momento en que la compra fue confirmada.

> 💡 **Contenido:** Debe conservar la fotografía histórica del producto, presentación, atributos, cantidad, precio unitario y subtotal aplicados en la compra.

---

### 6.3 EstadoPedido

**Clasificación:** Enum

**Flujo básico:**

```text
CONFIRMADO
    ↓
EN_PREPARACION
    ↓
LISTO
```

**Para recojo:**

```text
LISTO → COMPLETADO
```

**Para delivery:**

```text
LISTO → EN_ENTREGA → COMPLETADO
```

> ⚠️ **Salida alternativa:** Como salida alternativa se considera `CANCELADO`, sujeto a las reglas de cancelación.

---

### 6.4 HistorialEstadoPedido

**Clasificación:** Objeto interno

**Responsabilidad:** Conservar los cambios relevantes en el estado de atención de un pedido y el momento en que ocurrieron.

> 📌 **Nota:** `SeguimientoPedido` no requiere ser una entidad independiente: puede derivarse del estado actual y del historial.

---

### 6.5 ResumenMonetario

**Clasificación:** Value Object

**Responsabilidad:** Representar los importes definitivos que componen el total económico de una compra.

> 💡 **Componentes:** Puede contemplar subtotal de productos, descuento, costo de entrega, IGV incluido cuando corresponda y total final.

---

## 7. Entrega

### 7.1 ModalidadEntrega

**Clasificación:** Enum

**Responsabilidad:** Representar la forma mediante la cual el cliente recibirá una compra confirmada.

> 📌 **Modalidades del MVP:** Para el MVP: `RECOJO` y `DELIVERY`

---

### 7.2 DireccionEntrega

**Clasificación:** Value Object

**Responsabilidad:** Representar los datos necesarios para identificar el destino utilizado en un pedido con modalidad delivery.

> 💡 **Diseño Flexible:** La dirección utilizada debe conservarse como fotografía de la información al momento de la compra. Las coordenadas pueden ser opcionales, permitiendo integrar mapas posteriormente sin convertirlos en dependencia del MVP.

---

### 7.3 ConfiguracionEntrega

**Clasificación:** Value Object

**Responsabilidad:** Representar las modalidades de entrega habilitadas por una tienda y sus condiciones económicas básicas.

> ⚠️ **Fuera del MVP:** No se considera en esta versión optimización de rutas, seguimiento GPS, gestión de repartidores ni cálculo avanzado por distancia.

---

## 8. Experiencia del Usuario

### 8.1 Notificacion

**Clasificación:** Entidad secundaria

**Responsabilidad:** Representar un aviso persistente generado para informar a un usuario sobre un evento relevante ocurrido dentro de JaldiShop.

> 💡 **Ejemplos:** Pedido confirmado, cambio de estado, cancelación, nuevo pedido para el comerciante o stock bajo.

> 📌 **Nota:** WebSocket, correo electrónico y otros mecanismos son canales técnicos, no entidades del dominio.

---

### 8.2 Favorito

**Clasificación:** Entidad secundaria

**Responsabilidad:** Representar el interés explícito de un cliente por conservar un producto para consultarlo nuevamente más adelante.

> 💡 **Reglas:** Un cliente no debe registrar más de una vez el mismo producto como favorito. El favorito se realiza sobre el producto general, no sobre una variante, y no garantiza disponibilidad futura.

---

### 8.3 Reseña

**Clasificación:** Entidad secundaria

**Responsabilidad:** Representar la valoración que un cliente realiza sobre un producto después de haberlo adquirido mediante una compra confirmada.

> 📌 **Restricciones del MVP:**
> - Solo puede reseñar quien haya adquirido el producto y cuyo pedido esté `COMPLETADO`
> - Se considera como máximo una reseña por cliente y producto
> - La valoración promedio es información calculada, no una entidad independiente

---

## 9. Value Objects Identificados

| Value Object | Responsabilidad |
|---|---|
| **Dinero** | Representar un importe monetario junto con su moneda |
| **AtributoVariante** | Representar una característica y su valor dentro de una variante |
| **PeriodoCapacidad** | Representar un día o intervalo operativo |
| **ResumenMonetario** | Representar la composición económica de una compra |
| **DireccionEntrega** | Representar el destino utilizado para un delivery |
| **ConfiguracionEntrega** | Representar las condiciones básicas de entrega de una tienda |
| **ConfiguracionTributaria** | Representar las condiciones tributarias básicas aplicables a los precios de una tienda |
| **CodigoDescuento** | Representar un código de promoción cuando la modalidad lo requiera |

---

### Dinero

Representa tanto una cantidad como su moneda. Para la primera versión JaldiShop operará principalmente con soles peruanos, sin acoplar conceptualmente el dominio de forma permanente a una única moneda.

---

### AtributoVariante

Representa una característica que diferencia una variante, por ejemplo `Talla = M`, `Color = Negro` o `Tamaño = Grande`. Dentro de una misma variante no debe repetirse el mismo tipo de atributo.

---

### ConfiguracionTributaria

Representa las condiciones tributarias básicas utilizadas por una tienda para interpretar sus precios.

> 💡 **Precios con IGV Incluido:** Los precios registrados en JaldiShop representan el precio final de venta. Cuando corresponda aplicar IGV, este se considera incluido en dicho precio.

> ⚠️ **Nota:** JaldiShop no agrega automáticamente IGV sobre el precio mostrado ni pretende implementar un sistema contable o tributario completo.

---

## 10. Enumeraciones Identificadas

| Enum | Propósito |
|---|---|
| **Rol** | Distinguir CLIENTE, COMERCIANTE y ADMINISTRADOR |
| **EstadoUsuario** | Representar el estado operativo de un usuario |
| **EstadoTienda** | Representar el estado operativo de una tienda |
| **EstadoCategoria** | Representar la disponibilidad de una categoría |
| **EstadoProducto** | Representar la disponibilidad comercial de un producto |
| **EstadoVariante** | Representar la disponibilidad comercial de una variante |
| **EstadoPedido** | Representar la etapa actual de atención de un pedido |
| **EstadoPago** | Representar la situación actual de un pago |
| **EstadoReservaCapacidad** | Representar el estado de una reserva temporal |
| **ModalidadEntrega** | Distinguir RECOJO y DELIVERY |
| **MetodoPago** | Representar el medio utilizado para realizar un pago |
| **TipoDescuento** | Distinguir porcentaje y monto fijo |
| **ModalidadDescuento** | Distinguir automático y mediante código |
| **EstadoNotificacion** | Distinguir notificaciones leídas y no leídas |
| **TipoNotificacion** | Clasificar el evento que origina una notificación |
| **EstadoReseña** | Representar si una reseña está activa u oculta |

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
| **Maps** | Integración externa opcional |
| **ReputacionProducto** | Valor calculado a partir de reseñas |
| **CostoDelivery** | Importe representado mediante `Dinero` |
| **Impuesto** | Regla/configuración tributaria, no entidad independiente |
| **MetodoPagoGuardado** | Fuera del alcance actual |
| **DireccionUsuario** | Fuera del alcance actual |
| **Entrega** | No requiere ciclo de vida independiente en el MVP |

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
| 16 | Un producto solo puede ser reseñado por quien lo haya adquirido en un pedido completado |
| 17 | Un cliente no puede registrar repetidamente el mismo producto como favorito |
| 18 | Una entidad solo puede eliminarse físicamente cuando no posea registros o historial asociado; en caso contrario debe conservarse mediante el estado que corresponda |

---

## 13. Fuera del Modelo de Dominio MVP

> ⚠️ **Límites del MVP:** Quedan fuera del modelo funcional actual:

- Capacidad ponderada por producto
- Capacidad por múltiples recursos o estaciones
- Reserva de inventario
- Múltiples almacenes
- Lotes y kardex avanzado
- Proveedores
- Múltiples tiendas por comerciante
- Sucursales
- Empleados con permisos avanzados
- Promociones complejas o acumulables
- Métodos de pago almacenados
- Seguimiento GPS
- Repartidores
- Optimización de rutas
- Cobertura geográfica avanzada
- Facturación electrónica completa
- Respuestas e imágenes en reseñas
- Moderación avanzada
- Historial de búsquedas

---

## 14. Pendiente — Relaciones del Dominio

> 🔧 **Pendiente de revisión y completado por el equipo asignado.**

Esta sección deberá definir cómo se relacionan los conceptos identificados, incluyendo cardinalidades y dependencias conceptuales.

No se establecen relaciones definitivas en esta versión para no adelantar decisiones correspondientes a la siguiente etapa del modelado.

---

## 15. Pendiente — Agregados

> 🔧 **Pendiente de revisión y completado por el equipo asignado.**

Esta sección deberá identificar los posibles agregados y sus límites después de revisar las relaciones y las invariantes del dominio.

No se definen agregados definitivos en esta versión.

---

## 16. Resumen de Conceptos

### Entidades principales

Usuario, Tienda, Categoria, Producto, VarianteProducto, Inventario, Carrito, Descuento, ConfiguracionCapacidad, ExcepcionCapacidad, ReservaCapacidad, Pago y Pedido.

### Entidades secundarias

Notificacion, Favorito y Reseña.

### Objetos internos

ItemCarrito, IntentoPago, DetallePedido e HistorialEstadoPedido.

### Value Objects principales

Dinero, AtributoVariante, PeriodoCapacidad, ResumenMonetario, DireccionEntrega, ConfiguracionEntrega, ConfiguracionTributaria y CodigoDescuento.

---

## 17. Estado del Documento

> 📌 **Nota:** La identificación de conceptos y la definición inicial de responsabilidades se consideran completadas para esta versión.

**Quedan pendientes:**

- Revisión de los conceptos propuestos
- Definición de relaciones
- Definición de cardinalidades
- Identificación de agregados
- Revisión final del modelo de dominio
- Aprobación del equipo

Una vez completadas estas etapas, el modelo podrá utilizarse como base conceptual para el posterior modelo entidad-relación y el diseño técnico de JaldiShop.

---

[⬅ Alcance del MVP](./alcance-mvp.md) | [🏠 Volver al Índice General](../../README.md) | [Reglas de Negocio ➡](./reglas-negocio.md)
