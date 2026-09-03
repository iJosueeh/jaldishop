# Historias de Usuario

### JaldiShop — Especificación Funcional del MVP v1.1

[![Estado](https://img.shields.io/badge/Estado-En%20Revisión-orange?style=for-the-badge&logo=checkmarx&logoColor=white)](./historia.md)
[![Versión](https://img.shields.io/badge/Versión-v1.1-blue?style=for-the-badge)](./historia.md)
[![Fase](https://img.shields.io/badge/Fase-Sprint_02-orange?style=for-the-badge)](../06-scrum/sprint-02.md)

---

`📍 Docs` > `03-Requisitos` > **Historias de Usuario**  
[⬅ Reglas de Negocio](./reglas-negocio.md) | [🏠 Índice General](../../README.md) | [Alcance del MVP ➡](./alcance-mvp.md)

---

## 1. Objetivo

Definir las historias de usuario de **JaldiShop**, traduciendo las necesidades identificadas en la investigación y propuesta del producto hacia funcionalidades concretas del sistema.

Las historias se organizan según los tres actores definidos:

- **Cliente:** 9 historias.
- **Comerciante:** 10 historias.
- **Administrador:** 4 historias.

---

# 2. Historias de Usuario – Cliente

## HU-CLI-01 – Registro e inicio de sesión

### Necesidad

El cliente necesita acceder de forma segura a la plataforma para realizar compras y consultar sus pedidos.

### Funcionalidad

**Autenticación de clientes:** registro e inicio de sesión mediante credenciales y control de acceso por rol.

### Historia de usuario

**Como** cliente,

**quiero** registrarme e iniciar sesión en JaldiShop,

**para** realizar compras y consultar la información de mis pedidos de manera segura.

### Criterios de aceptación

- El cliente puede registrarse proporcionando los datos obligatorios.
- El sistema valida que los datos ingresados sean correctos.
- El sistema evita registros duplicados con el mismo correo.
- El cliente puede iniciar sesión utilizando sus credenciales.
- El sistema valida las credenciales antes de permitir el acceso.
- El sistema identifica al usuario con el rol correspondiente.
- Un cliente no puede acceder a funcionalidades exclusivas del comerciante o administrador.

### MoSCoW

**Must Have**

### Justificación

Es indispensable para identificar al cliente, proteger sus datos y permitir el acceso a las funcionalidades asociadas a sus compras. Forma parte del flujo comercial básico del MVP.

---

## HU-CLI-02 – Consultar catálogo

### Necesidad

El cliente necesita conocer los productos que ofrece una tienda antes de decidir qué comprar.

### Funcionalidad

**Catálogo digital:** consulta de productos organizados por categorías con información de precio, imagen y disponibilidad.

### Historia de usuario

**Como** cliente,

**quiero** consultar el catálogo de productos de una tienda,

**para** conocer su oferta y seleccionar los productos que deseo comprar.

### Criterios de aceptación

- El cliente puede visualizar el catálogo de una tienda.
- Los productos muestran como mínimo nombre, precio, imagen y disponibilidad.
- Los productos pueden visualizarse organizados por categorías.
- Solo se muestran como disponibles los productos que pueden ser adquiridos.
- El cliente puede consultar la información de un producto antes de agregarlo al carrito.

### MoSCoW

**Must Have**

### Justificación

El catálogo constituye el punto de entrada al proceso de compra. Sin esta funcionalidad no es posible que el cliente seleccione productos dentro del flujo comercial.

---

## HU-CLI-03 – Consultar fecha y franja disponible

### Necesidad

El cliente necesita conocer cuándo puede realizarse su pedido sin generar una sobrecarga operativa para el negocio.

### Funcionalidad

**Consulta de disponibilidad:** visualización de fechas y franjas horarias disponibles según la capacidad configurada por el comerciante.

### Historia de usuario

**Como** cliente,

**quiero** consultar las fechas y franjas horarias disponibles,

**para** seleccionar un momento en el que mi pedido pueda ser atendido.

### Criterios de aceptación

- El sistema muestra las fechas disponibles para realizar pedidos.
- Cuando la capacidad está configurada por franjas, muestra las franjas disponibles.
- Las fechas o franjas sin capacidad disponible se muestran como no disponibles.
- La disponibilidad considera la capacidad efectiva de la tienda.
- El cliente no puede seleccionar una fecha o franja que haya alcanzado su capacidad.

### MoSCoW

**Must Have**

### Justificación

Permite que el cliente conozca cuándo el negocio realmente puede comprometerse a atender su pedido.

---

## HU-CLI-04 – Agregar productos al carrito y realizar checkout

### Necesidad

El cliente necesita seleccionar los productos que desea comprar y preparar su pedido antes de realizar el pago.

### Funcionalidad

**Carrito y checkout:** gestión de productos seleccionados y preparación de la compra.

### Historia de usuario

**Como** cliente,

**quiero** agregar y modificar productos en mi carrito antes del checkout,

**para** revisar y confirmar mi pedido antes de realizar el pago.

### Criterios de aceptación

- El cliente puede agregar productos disponibles al carrito.
- Puede modificar la cantidad de productos.
- Puede eliminar productos del carrito.
- El sistema calcula el subtotal y total correspondiente.
- Agregar productos al carrito **no reserva** inventario ni capacidad operativa.
- El stock se valida durante el proceso de checkout.
- Inmediatamente antes de iniciar el pago, el sistema revalida el stock de todos los productos.
- Antes de iniciar el pago, el sistema verifica que la reserva de capacidad siga vigente.
- Si el stock resulta insuficiente durante la revalidación, el pago no se inicia.
- Si la reserva de capacidad ya no es válida, el pago no se inicia.
- El cliente puede seleccionar la modalidad de entrega disponible.

### MoSCoW

**Must Have**

### Justificación

El carrito y checkout son necesarios para ejecutar el proceso de compra y conectar la selección de productos con la reserva de capacidad y el pago.

---

## HU-CLI-05 – Reservar temporalmente un cupo

### Necesidad

El cliente necesita conservar la disponibilidad seleccionada mientras inicia el proceso de pago, evitando que otro cliente comprometa simultáneamente el mismo cupo.

### Funcionalidad

**Reserva temporal de capacidad:** bloqueo temporal de un cupo durante 10 minutos para que el cliente inicie válidamente el proceso de pago.

### Historia de usuario

**Como** cliente,

**quiero** reservar temporalmente un cupo durante el checkout,

**para** asegurar la disponibilidad seleccionada mientras inicio el pago.

### Criterios de aceptación

- El sistema valida la disponibilidad antes de crear la reserva.
- Si existe capacidad disponible, se genera un Hold temporal.
- El Hold tiene una duración de **10 minutos** desde su creación.
- Los 10 minutos representan el tiempo disponible para **iniciar** válidamente el proceso de pago.
- La compra no necesita completarse dentro de los 10 minutos, pero el pago debe iniciarse antes de que la reserva expire.
- Mientras el Hold está activo, el cupo se considera **capacidad reservada**.
- Si el pago se inicia antes de que expire la reserva, el cupo queda temporalmente protegido mientras la pasarela procesa la transacción.
- Si el pago es confirmado, la capacidad reservada se convierte en **capacidad comprometida** definitiva.
- Si el pago falla o se cancela, el cupo vuelve a estar disponible.
- Si transcurren los 10 minutos sin haber iniciado válidamente el pago, la reserva expira y libera el cupo.
- El sistema evita que dos clientes comprometan simultáneamente el mismo cupo.

### MoSCoW

**Must Have**

### Justificación

Es una funcionalidad crítica para el diferenciador de JaldiShop. Sin la reserva temporal, el sistema no podría proteger la capacidad durante el checkout ni evitar conflictos de concurrencia.

---

## HU-CLI-06 – Realizar pago

### Necesidad

El cliente necesita completar el pago para confirmar su pedido y comprometer definitivamente la capacidad.

### Funcionalidad

**Pago electrónico:** integración con una pasarela de pagos en modo sandbox.

### Historia de usuario

**Como** cliente,

**quiero** realizar el pago de mi pedido mediante la pasarela habilitada,

**para** confirmar mi compra y asegurar el cupo seleccionado.

### Criterios de aceptación

- El pago solo puede iniciarse si existe una reserva temporal de capacidad válida y activa.
- El stock debe haberse revalidado inmediatamente antes de iniciar el cobro.
- El cliente puede iniciar el proceso de pago desde el checkout.
- Puede existir más de un intento de pago mientras la reserva temporal esté vigente.
- Un pago iniciado válidamente antes del vencimiento de la reserva puede seguir procesándose bajo protección temporal.
- El pago se procesa mediante la pasarela configurada.
- El sistema registra el resultado de la operación.
- Si el pago es aprobado, se confirma la compra, se compromete el cupo, se actualiza el inventario y se crea el Pedido en estado `CONFIRMADO`.
- Si el pago es rechazado o cancelado, el pedido no se confirma y la capacidad reservada se libera.
- Un pago rechazado o cancelado no compromete capacidad de forma definitiva.
- Un resultado de pago recibido cuando la reserva ya expiró no debe crear automáticamente un pedido.
- Una misma confirmación lógica no debe generar pedidos duplicados ni aplicar inventario o capacidad más de una vez.

### MoSCoW

**Must Have**

### Justificación

El pago es necesario para completar el flujo comercial y determinar cuándo una reserva temporal pasa a convertirse en capacidad comprometida.

---

## HU-CLI-07 – Seleccionar modalidad de entrega

### Necesidad

El cliente necesita conocer si su pedido puede ser entregado en su ubicación o si debe recogerlo en la tienda.

### Funcionalidad

**Delivery básico:** selección entre delivery y recojo con información de dirección para la entrega.

### Historia de usuario

**Como** cliente,

**quiero** seleccionar delivery o recojo y proporcionar mi dirección cuando corresponda,

**para** recibir mi pedido en la ubicación indicada o recogerlo en tienda.

### Criterios de aceptación

- El cliente puede seleccionar entre delivery y recojo cuando ambas modalidades estén disponibles.
- Para delivery, el cliente ingresa la información de dirección suficiente para realizar la entrega.
- Para recojo, no se requiere información de dirección.
- La modalidad seleccionada forma parte del proceso de checkout.
- La validación geográfica avanzada (radio de cobertura, distancia, geocodificación) no es obligatoria para el núcleo del MVP.
- Si existe un costo de delivery básico, se informa al cliente antes de confirmar la compra.

### MoSCoW

**Must Have**

### Justificación

El delivery básico está definido explícitamente dentro del alcance del MVP y es necesario para que el negocio pueda aceptar pedidos considerando sus condiciones reales de despacho.

---

## HU-CLI-08 – Realizar seguimiento del pedido

### Necesidad

El cliente necesita conocer el avance de su pedido sin tener que consultar constantemente al negocio por WhatsApp o redes sociales.

### Funcionalidad

**Seguimiento de pedidos:** consulta del estado actualizado del pedido.

### Historia de usuario

**Como** cliente,

**quiero** consultar el estado de mi pedido,

**para** conocer en qué etapa se encuentra hasta su entrega o recojo.

### Criterios de aceptación

- El cliente puede consultar sus pedidos.
- Puede visualizar el detalle de cada pedido.
- Puede visualizar el estado actual.
- Los estados visibles son: `CONFIRMADO`, `EN_PREPARACION`, `LISTO`, `EN_ENTREGA`, `COMPLETADO` y `CANCELADO`.
- El estado `EN_ENTREGA` solo aplica a pedidos con modalidad `DELIVERY`.
- El seguimiento inicia a partir de la existencia del Pedido (estado `CONFIRMADO`).
- Los estados reflejan los cambios realizados por el comerciante.
- El pedido conserva la información necesaria para realizar su seguimiento.

### MoSCoW

**Should Have**

### Justificación

El seguimiento aporta valor directo a la propuesta de JaldiShop al reducir consultas repetitivas y mejorar la experiencia del cliente. Sin embargo, el flujo comercial básico puede completarse sin esta funcionalidad.

---

## HU-CLI-09 – Cancelar pedido confirmado

### Necesidad

El cliente necesita poder cancelar un pedido que acaba de confirmar si cambia de opinión o detecta un error, siempre que el comerciante aún no haya iniciado su preparación.

### Funcionalidad

**Cancelación de pedido:** anulación de un pedido en estado `CONFIRMADO` con liberación automática de la capacidad comprometida.

### Historia de usuario

**Como** cliente,

**quiero** cancelar un pedido que he confirmado recientemente,

**para** liberar el cupo si ya no puedo o no deseo recibir el producto.

### Criterios de aceptación

- El cliente puede cancelar un pedido mientras permanezca en estado `CONFIRMADO`.
- Al cancelar un pedido en estado `CONFIRMADO`, se libera automáticamente la capacidad comprometida.
- Una vez que el pedido avanza a `EN_PREPARACION`, el cliente no puede cancelarlo mediante el flujo ordinario.
- La cancelación registra el estado `CANCELADO` como estado terminal del pedido.
- No se definen políticas avanzadas de reembolso en el MVP.

### MoSCoW

**Must Have**

### Justificación

La cancelación temprana es parte del flujo comercial básico y permite recuperar capacidad operativa cuando el pedido aún no ha sido procesado.

---

# 3. Historias de Usuario – Comerciante

## HU-COM-01 – Configurar tienda

### Necesidad

El comerciante necesita disponer de una tienda digital propia donde pueda ofrecer sus productos y establecer sus condiciones de atención.

### Funcionalidad

**Gestión de tienda:** configuración del perfil del negocio y horarios de atención.

### Historia de usuario

**Como** comerciante,

**quiero** configurar la información y horarios de mi tienda,

**para** establecer las condiciones bajo las cuales los clientes pueden realizar pedidos.

### Criterios de aceptación

- El comerciante puede registrar y editar la información básica de su tienda.
- Puede configurar sus horarios de atención.
- La tienda queda asociada al comerciante correspondiente.
- Los datos de una tienda no pueden ser modificados por otro comerciante.
- Los clientes pueden consultar la información publicada de la tienda.

### MoSCoW

**Must Have**

### Justificación

Es necesaria para establecer el contexto comercial de los productos, pedidos y capacidad que serán administrados por cada comerciante.

---

## HU-COM-02 – Gestionar categorías

### Necesidad

El comerciante necesita organizar sus productos para facilitar la navegación del catálogo.

### Funcionalidad

**Gestión de categorías:** creación, modificación y eliminación de categorías.

### Historia de usuario

**Como** comerciante,

**quiero** crear y gestionar categorías de productos,

**para** organizar mi catálogo de manera clara para los clientes.

### Criterios de aceptación

- El comerciante puede crear categorías.
- Puede editar una categoría existente.
- Puede eliminar categorías cuando corresponda.
- Los productos pueden asociarse a una categoría.
- Las categorías pertenecen exclusivamente a la tienda del comerciante.

### MoSCoW

**Must Have**

### Justificación

Permite organizar el catálogo y forma parte de la gestión básica de productos contemplada en el MVP.

---

## HU-COM-03 – Gestionar productos

### Necesidad

El comerciante necesita mantener actualizada su oferta comercial.

### Funcionalidad

**CRUD de productos:** creación, consulta, modificación y eliminación de productos, incluyendo imágenes.

### Historia de usuario

**Como** comerciante,

**quiero** crear y gestionar los productos de mi tienda,

**para** mantener actualizado el catálogo disponible para los clientes.

### Criterios de aceptación

- El comerciante puede registrar un producto.
- Puede ingresar nombre, descripción, precio, categoría, imagen y stock.
- Puede modificar los datos del producto.
- Puede retirar o eliminar un producto.
- Los cambios realizados se reflejan en el catálogo de su tienda.
- Un comerciante no puede modificar productos pertenecientes a otra tienda.

### MoSCoW

**Must Have**

### Justificación

Sin productos disponibles no existe una oferta que pueda ser comprada, por lo que esta funcionalidad es indispensable para el flujo comercial.

---

## HU-COM-04 – Gestionar inventario

### Necesidad

El comerciante necesita controlar el stock físico disponible para evitar aceptar pedidos de productos que no puede entregar.

### Funcionalidad

**Gestión de inventario:** consulta y actualización del stock de productos.

### Historia de usuario

**Como** comerciante,

**quiero** gestionar el stock de mis productos,

**para** evitar aceptar pedidos que superen las existencias disponibles.

### Criterios de aceptación

- El comerciante puede consultar el stock de sus productos.
- Puede actualizar las cantidades disponibles.
- El sistema valida el stock al momento de realizar una compra.
- El sistema no permite solicitar una cantidad superior al stock disponible.
- El sistema diferencia la disponibilidad de inventario de la capacidad operativa.
- El stock se actualiza de acuerdo con la confirmación de los pedidos.

### MoSCoW

**Must Have**

### Justificación

El inventario es una de las condiciones que deben validarse antes de comprometer una venta. JaldiShop debe controlar tanto el stock como la capacidad operativa.

---

## HU-COM-05 – Configurar capacidad por día

### Necesidad

El comerciante necesita establecer un límite realista de pedidos que puede producir o atender diariamente.

### Funcionalidad

**Configuración de capacidad diaria:** establecimiento de una cantidad máxima de pedidos por día.

### Historia de usuario

**Como** comerciante,

**quiero** configurar mi capacidad de pedidos por día,

**para** evitar aceptar más pedidos de los que puedo producir o atender.

### Criterios de aceptación

- El comerciante puede establecer una capacidad para una fecha.
- La capacidad representa el número máximo de pedidos que puede atender.
- Para el MVP, cada pedido consume un cupo.
- Los pedidos pagados incrementan la capacidad comprometida.
- El sistema calcula la capacidad disponible.
- Cuando la capacidad disponible llega a cero, la fecha queda bloqueada para nuevos pedidos.

### MoSCoW

**Must Have**

### Justificación

Es una de las funcionalidades centrales de JaldiShop porque permite controlar el límite operativo del negocio y prevenir la sobreventa.

---

## HU-COM-06 – Configurar capacidad por franjas horarias

### Necesidad

El comerciante necesita distribuir su capacidad según los diferentes momentos del día, especialmente durante periodos de alta demanda.

### Funcionalidad

**Configuración de capacidad por franjas:** definición de cupos para franjas horarias de duración configurable.

### Historia de usuario

**Como** comerciante,

**quiero** configurar cupos por franjas horarias,

**para** distribuir mi capacidad operativa durante el día y evitar saturaciones.

### Criterios de aceptación

- El comerciante puede definir franjas horarias.
- Las franjas tienen una duración configurable según las necesidades operativas de la tienda.
- No existe una duración mínima o máxima obligatoria para las franjas.
- Puede establecer una capacidad específica para cada franja.
- El sistema calcula la disponibilidad de cada franja.
- Una franja se bloquea cuando su capacidad disponible llega a cero.
- Los clientes solo pueden seleccionar franjas con disponibilidad.

### MoSCoW

**Must Have**

### Justificación

La configuración por franjas es necesaria para negocios cuya capacidad varía durante el día, uno de los escenarios identificados en los casos de investigación.

---

## HU-COM-07 – Configurar excepciones temporales

### Necesidad

El comerciante necesita adaptar temporalmente su capacidad ante fechas de alta demanda, fallas o situaciones operativas imprevistas.

### Funcionalidad

**Gestión de excepciones de capacidad:** modificación temporal de la capacidad efectiva para fechas o franjas específicas.

### Historia de usuario

**Como** comerciante,

**quiero** configurar excepciones temporales de capacidad,

**para** adaptar mis cupos ante situaciones especiales o imprevistos operativos.

### Criterios de aceptación

- El comerciante puede definir una excepción para una fecha o franja.
- La excepción modifica la capacidad efectiva durante el periodo establecido.
- El sistema considera la excepción al calcular la disponibilidad.
- Una excepción puede aumentar o reducir temporalmente la capacidad.
- Finalizado el periodo de la excepción, deja de aplicarse.

### MoSCoW

**Must Have**

### Justificación

Las excepciones forman parte del modelo consolidado de capacidad y permiten representar situaciones reales como fechas festivas, fallas de equipo o refuerzo temporal.

---

## HU-COM-08 – Controlar reservas y capacidad comprometida

### Necesidad

El comerciante necesita que el sistema controle automáticamente los cupos reservados y comprometidos para evitar sobreventa.

### Funcionalidad

**Motor de capacidad:** cálculo de capacidad efectiva, capacidad reservada, capacidad comprometida y capacidad disponible.

### Historia de usuario

**Como** comerciante,

**quiero** que JaldiShop controle automáticamente los cupos reservados y comprometidos,

**para** evitar aceptar pedidos por encima de mi capacidad operativa.

### Criterios de aceptación

- El sistema determina la capacidad efectiva considerando la capacidad base y las excepciones.
- **Capacidad Reservada:** cupos retenidos temporalmente por Hold de checkout activos.
- **Capacidad Comprometida:** cupos consumidos por Pedidos ya confirmados.
- La capacidad disponible se obtiene descontando la capacidad reservada y la capacidad comprometida de la capacidad efectiva.
- Las reservas temporales duran 10 minutos para iniciar el pago.
- Una reserva confirmada mediante pago se convierte en capacidad comprometida.
- Una reserva expirada o asociada a un pago fallido libera el cupo.
- Cuando la capacidad disponible llega a cero, el sistema bloquea nuevos checkouts para esa fecha o franja.

### MoSCoW

**Must Have**

### Justificación

Es el principal diferenciador funcional de JaldiShop. El producto existe precisamente para controlar la capacidad operativa en tiempo real y prevenir la sobreventa.

---

## HU-COM-09 – Gestionar pedidos

### Necesidad

El comerciante necesita centralizar los pedidos que recibe desde su tienda para evitar pérdidas o confusiones derivadas de la gestión manual.

### Funcionalidad

**Gestión centralizada de pedidos:** dashboard con listado, detalle y organización de órdenes.

### Historia de usuario

**Como** comerciante,

**quiero** visualizar y gestionar los pedidos de mi tienda desde un panel,

**para** centralizar las ventas y organizar su atención.

### Criterios de aceptación

- El comerciante puede visualizar los pedidos de su tienda.
- Puede consultar el detalle de cada pedido.
- Puede identificar los productos, cantidades y modalidad de entrega.
- Puede consultar la fecha o franja seleccionada.
- Los pedidos de otras tiendas no aparecen en su panel.
- El panel permite identificar el estado actual de cada pedido.

### MoSCoW

**Must Have**

### Justificación

Centralizar los pedidos es una función central de la propuesta de valor de JaldiShop y permite sustituir la gestión fragmentada por WhatsApp, Instagram u otros canales.

---

## HU-COM-10 – Actualizar estados y gestionar cancelaciones

### Necesidad

El comerciante necesita informar el avance operativo del pedido y aplicar las reglas de cancelación según su estado.

### Funcionalidad

**Gestión del ciclo de vida del pedido:** actualización de estados y aplicación de reglas de cancelación.

### Historia de usuario

**Como** comerciante,

**quiero** actualizar el estado de los pedidos y gestionar sus cancelaciones según la etapa operativa,

**para** mantener informado al cliente y controlar correctamente la capacidad comprometida.

### Criterios de aceptación

- El comerciante puede actualizar el estado del pedido según el flujo definido.
- El flujo estándar es: `CONFIRMADO` → `EN_PREPARACION` → `LISTO` → `COMPLETADO`.
- Para pedidos con modalidad `DELIVERY`, el estado `LISTO` pasa a `EN_ENTREGA` antes de `COMPLETADO`.
- Para pedidos con modalidad `RECOJO`, el estado `LISTO` pasa directamente a `COMPLETADO`.
- El estado `CANCELADO` es terminal y puede ocurrir desde `CONFIRMADO` o excepcionalmente desde `EN_PREPARACION`.
- Una cancelación en estado `CONFIRMADO` libera la capacidad comprometida.
- Una cancelación durante o después de la preparación no recupera el cupo operativo del periodo.
- El comerciante puede realizar cancelaciones excepcionales cuando no pueda continuar con la atención.
- Los cambios de estado son visibles para el cliente durante el seguimiento.

### MoSCoW

**Must Have**

### Justificación

El estado operativo determina tanto el seguimiento del cliente como el comportamiento de la capacidad ante cancelaciones.

---

# 4. Historias de Usuario – Administrador

## HU-ADM-01 – Gestionar usuarios y roles

### Necesidad

La plataforma necesita mantener control sobre los usuarios y sus permisos para garantizar la separación entre clientes, comerciantes y administradores.

### Funcionalidad

**Administración de usuarios y roles:** consulta y gestión de cuentas según su rol.

### Historia de usuario

**Como** administrador,

**quiero** gestionar los usuarios y sus roles,

**para** mantener el control de acceso y funcionamiento de la plataforma.

### Criterios de aceptación

- El administrador puede consultar los usuarios registrados.
- Puede identificar el rol asignado a cada usuario.
- Puede gestionar las cuentas según los permisos definidos.
- Los usuarios no pueden acceder a funciones que no corresponden a su rol.
- Las acciones administrativas requieren autenticación y permisos de administrador.

### MoSCoW

**Must Have**

### Justificación

El control de roles es necesario para garantizar el aislamiento entre las funciones de cliente, comerciante y administración de la plataforma.

---

## HU-ADM-02 – Supervisar tiendas

### Necesidad

La plataforma necesita supervisar las tiendas registradas para mantener un control general de los comercios que utilizan JaldiShop.

### Funcionalidad

**Administración de tiendas:** consulta y supervisión de las tiendas registradas.

### Historia de usuario

**Como** administrador,

**quiero** consultar las tiendas registradas en JaldiShop,

**para** supervisar los comercios que operan dentro de la plataforma.

### Criterios de aceptación

- El administrador puede consultar las tiendas registradas.
- Puede identificar al comerciante asociado a cada tienda.
- Puede consultar información general de la tienda.
- Una tienda mantiene sus productos, capacidad y pedidos aislados de las demás.
- Las acciones del administrador respetan los permisos establecidos.

### MoSCoW

**Should Have**

### Justificación

Aporta control global sobre la plataforma, pero el flujo principal de compra y gestión de capacidad puede funcionar sin una supervisión administrativa avanzada.

---

## HU-ADM-03 – Supervisar pedidos

### Necesidad

La plataforma necesita disponer de una visión general de los pedidos para detectar problemas y supervisar el funcionamiento del servicio.

### Funcionalidad

**Supervisión global de pedidos:** consulta de pedidos y sus estados a nivel de plataforma.

### Historia de usuario

**Como** administrador,

**quiero** consultar los pedidos registrados en JaldiShop,

**para** supervisar el funcionamiento general de las operaciones.

### Criterios de aceptación

- El administrador puede consultar pedidos de las tiendas registradas.
- Puede identificar la tienda asociada.
- Puede consultar el estado actual de cada pedido.
- Puede consultar información básica del pedido.
- La consulta administrativa no modifica automáticamente los pedidos.

### MoSCoW

**Should Have**

### Justificación

La supervisión permite controlar el funcionamiento general del sistema, pero no es necesaria para que el cliente compre ni para que el comerciante gestione sus pedidos.

---

## HU-ADM-04 – Supervisar capacidad y disponibilidad

### Necesidad

La plataforma necesita permitir la supervisión del funcionamiento del motor de capacidad para identificar fechas o franjas saturadas.

### Funcionalidad

**Supervisión del motor de capacidad:** consulta de capacidad efectiva, capacidad reservada, capacidad comprometida y capacidad disponible de las tiendas.

### Historia de usuario

**Como** administrador,

**quiero** consultar la capacidad y disponibilidad de las tiendas,

**para** supervisar el correcto funcionamiento del control de capacidad de JaldiShop.

### Criterios de aceptación

- El administrador puede consultar la capacidad configurada por las tiendas.
- Puede visualizar la capacidad efectiva, reservada, comprometida y disponible.
- Puede identificar fechas o franjas saturadas.
- Puede consultar las reservas activas cuando corresponda.
- La consulta administrativa no altera la capacidad configurada por el comerciante.

### MoSCoW

**Could Have**

### Justificación

Es útil para supervisar el principal componente diferenciador de JaldiShop, pero el motor de capacidad puede operar correctamente sin que el administrador necesite consultar estos datos directamente.

