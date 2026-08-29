# Alcance del MVP — JaldiShop

## 1. Propósito

Este documento define el alcance de la primera versión funcional de **JaldiShop**, estableciendo las funcionalidades que formarán parte del MVP, sus prioridades y aquellas características que se mantendrán fuera de esta primera versión.

El objetivo es delimitar el desarrollo del producto, evitar incorporar funcionalidades que incrementen innecesariamente la complejidad y mantener el proyecto alineado con el problema identificado y los entregables académicos.

---

## 2. Descripción del producto

**JaldiShop** es una plataforma de gestión de pedidos y capacidad dirigida a MYPE que comercializan productos principalmente mediante canales digitales.

La plataforma busca complementar el proceso de venta del negocio mediante un entorno donde los clientes puedan consultar productos y realizar pedidos, mientras que el comerciante puede gestionar su catálogo, inventario, pedidos y capacidad operativa.

A diferencia de un e-commerce tradicional centrado principalmente en la disponibilidad de stock, JaldiShop incorpora la **capacidad operativa** como una restricción adicional para determinar si un negocio puede aceptar un pedido dentro de un periodo determinado.

---

## 3. Objetivo del MVP

El MVP de JaldiShop tiene como objetivo validar que una MYPE pueda recibir y gestionar pedidos digitales sin superar la capacidad operativa que ha configurado para un día o franja horaria.

La primera versión deberá permitir demostrar un flujo completo desde la configuración de una tienda hasta la realización, confirmación y gestión de un pedido.

El MVP deberá demostrar principalmente que:

- Un comerciante puede gestionar su propia tienda.
- Cada tienda mantiene sus propios productos, inventario, capacidad y pedidos.
- Un cliente puede consultar productos y realizar un pedido.
- El sistema valida inventario y capacidad antes de confirmar el pedido.
- El sistema evita aceptar pedidos cuando la capacidad disponible se encuentra agotada.
- La capacidad puede reservarse temporalmente durante el proceso de checkout.
- Un pago exitoso permite confirmar el pedido y comprometer la capacidad correspondiente.
- El comerciante puede gestionar posteriormente el estado del pedido.

---

## 4. Actores del MVP

### 4.1 Cliente

Usuario que accede a una tienda para consultar su catálogo y realizar pedidos.

Principales acciones:

- Consultar, buscar y filtrar productos.
- Consultar disponibilidad.
- Gestionar su carrito.
- Seleccionar una fecha o franja disponible.
- Realizar checkout y pago.
- Consultar el estado de sus pedidos.
- Cancelar un pedido cuando las reglas del negocio lo permitan.

### 4.2 Comerciante / Administrador de tienda

Usuario responsable de administrar el entorno correspondiente a su MYPE dentro de JaldiShop.

Principales acciones:

- Configurar información de su tienda.
- Gestionar categorías, productos e inventario.
- Configurar capacidad y excepciones temporales.
- Consultar pedidos.
- Actualizar estados de pedidos.
- Consultar la disponibilidad operativa de su negocio.

---

## 5. Modelo de tiendas

JaldiShop permitirá registrar diferentes MYPE mediante el concepto de **Store (Tienda)**.

Cada tienda representa el entorno operativo de un negocio dentro de la plataforma. Los datos pertenecientes a una tienda deberán mantenerse separados de los datos correspondientes a otras tiendas.

Una tienda podrá mantener sus propios:

- Productos.
- Categorías.
- Inventario.
- Configuraciones y excepciones de capacidad.
- Pedidos.
- Configuraciones básicas.

Para el MVP se considerará un modelo sencillo en el cual un comerciante administra su tienda.

No forma parte del alcance inicial implementar infraestructura multi-tenant avanzada, múltiples sucursales por negocio ni sistemas complejos de permisos organizacionales.

---

## 6. Diferencia entre inventario y capacidad

JaldiShop manejará el **inventario** y la **capacidad operativa** como conceptos independientes.

### Inventario

Representa la disponibilidad de productos registrados por una tienda.

> ¿Existe disponibilidad del producto solicitado?

### Capacidad

Representa la cantidad de pedidos que el negocio puede comprometer dentro de un periodo determinado.

> ¿El negocio puede aceptar y atender otro pedido en ese periodo?

Por lo tanto, disponer de inventario no implica necesariamente disponer de capacidad.

---

## 7. Modelo de capacidad del MVP

La primera versión utilizará un modelo de capacidad simple.

### 7.1 Unidad de capacidad

> **Un pedido consume un cupo de capacidad del periodo seleccionado.**

La cantidad o complejidad de productos contenidos dentro del pedido no modificará el consumo de capacidad en esta primera versión.

```text
Capacidad de 20:00 a 20:30 = 5 cupos

Pedido A → 1 cupo
Pedido B → 1 cupo
Pedido C → 1 cupo
Pedido D → 1 cupo
Pedido E → 1 cupo

Capacidad disponible = 0
```

Un modelo ponderado podrá evaluarse como evolución futura.

### 7.2 Periodos de capacidad

La capacidad podrá configurarse:

- Por día.
- Por franja horaria.

### 7.3 Capacidad base

El comerciante podrá establecer una capacidad habitual para sus periodos de operación.

### 7.4 Excepciones temporales

El comerciante podrá modificar temporalmente la capacidad de un día o periodo específico sin alterar permanentemente su configuración base.

### 7.5 Capacidad disponible

```text
Capacidad disponible =
Capacidad efectiva
- Capacidad reservada
- Capacidad comprometida
```

### 7.6 Saturación

Cuando la capacidad disponible de un periodo llegue a cero, el sistema no permitirá reservar nuevos pedidos para dicho periodo.

---

## 8. Reserva temporal de capacidad

Durante el checkout, JaldiShop podrá reservar temporalmente el cupo requerido por el pedido para evitar que dos clientes comprometan simultáneamente el último cupo.

```text
Capacidad disponible
        ↓
Reserva temporal
        ↓
      Checkout
        ↓
       Pago
      /    \
 Fallido   Exitoso
    ↓         ↓
 Liberar   Confirmar
 capacidad   pedido
```

Para la primera versión se propone una duración inicial de **10 minutos**.

Si el pago no se completa dentro del periodo establecido, la reserva expirará y la capacidad volverá a estar disponible.

---

## 9. Concurrencia

JaldiShop deberá garantizar que una misma unidad de capacidad no pueda ser comprometida simultáneamente por diferentes pedidos.

Cuando exista un último cupo disponible, el primer proceso que consiga reservar correctamente dicho cupo tendrá prioridad durante el tiempo de reserva establecido.

La implementación técnica específica se definirá posteriormente durante el diseño de arquitectura y persistencia.

---

## 10. Módulos incluidos en el MVP

### 10.1 Autenticación y autorización — Must Have

- Registro e inicio de sesión.
- Autenticación.
- Autorización según rol.
- Cliente.
- Comerciante / administrador de tienda.

### 10.2 Gestión de tienda — Must Have

- Registro y actualización de tienda.
- Información básica del negocio.
- Asociación del comerciante con su tienda.
- Separación lógica de datos entre tiendas.

### 10.3 Productos y categorías — Must Have

- CRUD de productos.
- Activación/desactivación.
- Precio e información del producto.
- Gestión de categorías.
- Asociación con una tienda.

### 10.4 Inventario — Must Have

- Stock por producto.
- Consulta y actualización de existencias.
- Validación de disponibilidad.
- Actualización al confirmar pedidos.
- Alerta básica de stock bajo.

No incluye recetas, ingredientes o materias primas.

### 10.5 Catálogo — Must Have

- Visualización y detalle de productos.
- Búsqueda.
- Filtros básicos.
- Visualización de disponibilidad.

### 10.6 Carrito — Must Have

- Agregar, modificar y eliminar productos.
- Visualizar resumen.
- Persistencia del carrito.

### 10.7 Gestión de capacidad — Must Have

- Capacidad base.
- Capacidad diaria y por franja.
- Excepciones temporales.
- Consulta de disponibilidad.
- Saturación.
- Reserva temporal.
- Liberación de reservas expiradas.
- Confirmación de capacidad.
- Protección ante concurrencia.

Este módulo constituye el principal diferenciador funcional de JaldiShop.

### 10.8 Pedidos — Must Have

- Creación y detalle.
- Historial/listado.
- Consulta por tienda.
- Estados.
- Actualización de estado.
- Cancelación básica.

Estados iniciales:

```text
PENDIENTE_PAGO
      ↓
CONFIRMADO
      ↓
EN_PREPARACION
      ↓
LISTO
      ↓
COMPLETADO
```

También se considerará `CANCELADO`. Para delivery podrá incorporarse `EN_ENTREGA`.

### 10.9 Pagos — Must Have

- Checkout.
- Pasarela de pago en sandbox.
- Confirmación de pago.
- Registro de transacción.
- Manejo básico de pagos fallidos.
- Asociación del pago con el pedido.

### 10.10 Entrega — Must Have

- Recojo.
- Delivery básico.
- Dirección de entrega.

No incluye optimización de rutas, gestión de flota ni seguimiento GPS.

### 10.11 Notificaciones y actualización de estado — Must Have

- Confirmación del pedido.
- Actualización del estado.
- Comunicación de cambios relevantes al cliente.
- Comunicación en tiempo real cuando corresponda.

### 10.12 Panel del comerciante — Must Have

Gestión de:

- Tienda.
- Productos y categorías.
- Inventario.
- Capacidad y excepciones.
- Pedidos y sus estados.

---

## 11. Funcionalidades complementarias

### 11.1 Integración con Maps — Could Have

Como valor agregado podrá utilizarse para:

- Visualizar la ubicación del negocio.
- Seleccionar o visualizar la dirección de entrega.
- Validar cobertura básica.
- Consultar distancia.

Maps no deberá bloquear el funcionamiento del flujo principal.

### 11.2 Notificación por correo — Should Have

Confirmación del pedido mediante correo electrónico.

### 11.3 Reportes básicos — Could Have

- Cantidad de pedidos.
- Pedidos por estado.
- Capacidad utilizada.
- Capacidad disponible.

---

## 12. Priorización general

| Funcionalidad | Prioridad |
|---|---|
| Autenticación y autorización | Must |
| Gestión de tienda | Must |
| Productos y categorías | Must |
| Inventario | Must |
| Catálogo, búsqueda y filtros | Must |
| Carrito | Must |
| Capacidad base y por periodo | Must |
| Excepciones de capacidad | Must |
| Consulta de capacidad | Must |
| Reserva y liberación temporal | Must |
| Control de concurrencia | Must |
| Pedidos y estados | Must |
| Cancelación básica | Must |
| Checkout y pago sandbox | Must |
| Recojo / delivery básico | Must |
| Panel del comerciante | Must |
| Actualización de estados | Must |
| Notificación por correo | Should |
| Integración con Maps | Could |
| Reportes básicos | Could |

---

## 13. Flujo principal del MVP

1. El comerciante se registra e inicia sesión.
2. Registra o configura su tienda.
3. Registra categorías, productos e inventario.
4. Configura la capacidad base y sus periodos.
5. El cliente accede a la tienda y consulta el catálogo.
6. Agrega productos al carrito.
7. Selecciona fecha o franja.
8. JaldiShop valida inventario y capacidad.
9. Si existe disponibilidad, genera una reserva temporal.
10. El cliente realiza checkout y pago.
11. JaldiShop valida nuevamente las condiciones.
12. Si el pago es exitoso, confirma el pedido.
13. La capacidad reservada pasa a comprometida y se actualiza el inventario.
14. El comerciante recibe y gestiona el pedido.
15. El cliente consulta los cambios de estado.
16. El pedido finaliza como completado.

---

## 14. Cancelación en el MVP

Si un pedido confirmado es cancelado antes de iniciar su preparación, la capacidad comprometida podrá liberarse.

Una vez que el pedido se encuentre en preparación o en una etapa posterior, la capacidad no se liberará automáticamente.

No se implementarán cálculos complejos de recuperación parcial, ingredientes consumidos, tiempo invertido ni reembolsos parciales automáticos.

---

## 15. Fuera del alcance del MVP

### Capacidad avanzada

- Capacidad ponderada por producto, cantidad o complejidad.
- Múltiples recursos simultáneos.
- Estaciones de producción.
- Capacidad individual por empleado o máquina.

### Producción e inventario avanzado

- Recetas.
- Ingredientes y materias primas.
- Planificación automática de producción.

### Logística avanzada

- Optimización de rutas.
- Tracking GPS.
- Gestión de flota.
- Asignación automática de repartidores.
- ETA inteligente.

### Automatización avanzada

- Predicción de demanda o capacidad mediante IA.
- Busy Mode avanzado.
- Ajustes automáticos de capacidad.

### Integraciones

- Integración directa con WhatsApp.
- Integración directa con Instagram.
- Sincronización automática de pedidos desde redes sociales.

### Organización avanzada

- Múltiples sucursales por tienda.
- Equipos con permisos personalizados.
- Infraestructura multi-tenant avanzada.

### Otros

- Aplicación móvil nativa.
- Analítica avanzada.
- Sistema avanzado de recomendaciones.

---

## 16. Criterio de éxito del MVP

El MVP será considerado funcional cuando pueda demostrarse de extremo a extremo que:

1. Un comerciante administra su tienda, productos e inventario.
2. Configura su capacidad.
3. Un cliente consulta el catálogo y crea un pedido.
4. El sistema valida inventario y capacidad.
5. Impide pedidos cuando el periodo está saturado.
6. Reserva temporalmente capacidad durante checkout.
7. Un pago exitoso confirma el pedido.
8. Se actualizan inventario y capacidad.
9. El comerciante gestiona el estado del pedido.
10. El cliente consulta su progreso.
11. Los datos de diferentes tiendas permanecen separados.

---

## 17. Evolución posterior

Posibles evoluciones posteriores:

- Capacidad ponderada.
- Capacidad por múltiples recursos.
- Gestión avanzada de producción.
- Integración con redes sociales.
- Delivery y rutas avanzadas.
- Analítica operativa.
- Predicción de demanda.
- Automatización de capacidad.
- Múltiples sucursales.

Estas funcionalidades se evaluarán después de completar y validar el MVP.

---

## 18. Estado del documento

**Versión:** 1.0  
**Estado:** Propuesta para revisión del equipo  
**Proyecto:** JaldiShop  
**Documento:** Alcance del MVP
