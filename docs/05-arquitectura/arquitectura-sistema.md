# Arquitectura del Sistema --- JaldiShop

**Versión:** 0.1\
**Estado:** En revisión\
**Fecha:** 2026-09-03\
**Proyecto:** JaldiShop --- Plataforma de gestión de pedidos y capacidad
para MYPE

------------------------------------------------------------------------

## 1. Propósito

Este documento describe la arquitectura inicial de JaldiShop y registra
las decisiones técnicas acordadas hasta la versión 0.1.

La arquitectura se mantendrá como documentación viva durante el
desarrollo. Las decisiones aún no cerradas se indican explícitamente
como propuestas o pendientes y podrán modificarse en versiones
posteriores.

### 1.1 Objetivos arquitectónicos

-   Mantener separada la lógica de negocio de los mecanismos de
    infraestructura.
-   Evitar acoplamiento innecesario entre funcionalidades.
-   Facilitar el desarrollo paralelo del equipo.
-   Mantener una complejidad adecuada para el alcance académico y el
    tiempo disponible.
-   Proteger las operaciones críticas de capacidad, inventario, pagos y
    pedidos.
-   Permitir sustituir integraciones externas sin afectar el núcleo del
    sistema.
-   Mantener una base que pueda evolucionar sin adoptar microservicios
    prematuramente.

------------------------------------------------------------------------

## 2. Estado de las decisiones

  Tema                                         Estado v0.1
  -------------------------------------------- --------------------
  Arquitectura cliente-servidor                Aprobado
  Monolito modular                             Aprobado
  Organización interna por responsabilidades   Aprobado
  Next.js para marketplace                     Aprobado
  Angular para dashboard                       Aprobado
  Spring Boot para backend                     Aprobado
  PostgreSQL                                   Aprobado
  Persistencia con Spring Data JPA             Propuesta aceptada
  Repositorios mediante contratos propios      Propuesta aceptada
  Transacciones en capa de aplicación          Propuesta aceptada
  Control de concurrencia                      En revisión
  Checkout como orquestador                    Propuesta aceptada
  Mercado Pago                                 Propuesto
  Seguridad y autorización por tienda          Pendiente
  WebSocket y notificaciones                   Pendiente
  Patrones de diseño                           Pendiente
  Modelo ER y relaciones JPA definitivas       Pendiente

------------------------------------------------------------------------

## 3. Estilo arquitectónico

JaldiShop se implementará inicialmente como un **monolito modular**.

El backend será una única aplicación Spring Boot desplegable, pero
estará dividido internamente en módulos funcionales con
responsabilidades delimitadas.

La organización interna estará inspirada en principios de **Clean
Architecture** y **arquitectura hexagonal**, aplicados de manera
pragmática. No se pretende implementar una variante estricta que obligue
a duplicar modelos o introducir abstracciones sin una necesidad real.

Spring MVC podrá utilizarse como mecanismo para exponer la API HTTP,
pero MVC no será utilizado como la organización arquitectónica principal
del backend.

### 3.1 Principios

1.  Organizar primero por capacidad funcional y después por
    responsabilidad técnica.
2.  Mantener las reglas centrales del negocio dentro del dominio.
3.  Utilizar la capa de aplicación para coordinar casos de uso.
4.  Mantener persistencia e integraciones externas en infraestructura.
5.  Los Controllers se limitarán a responsabilidades HTTP.
6.  Un módulo no accederá directamente a los repositorios o
    infraestructura interna de otro módulo.
7.  No todos los módulos estarán obligados a implementar todas las capas
    si no las necesitan.

------------------------------------------------------------------------

## 4. Arquitectura general

JaldiShop contará inicialmente con dos aplicaciones frontend
independientes:

-   **Marketplace:** desarrollado con Next.js y orientado al cliente.
-   **Dashboard:** desarrollado con Angular y orientado al comerciante.

Ambas aplicaciones consumirán una API central desarrollada con Spring
Boot.

PostgreSQL será el sistema de persistencia principal.

``` text
Next.js Marketplace ─┐
                     ├── HTTPS / REST ──► Spring Boot ──► PostgreSQL
Angular Dashboard ───┘                         │
                                              ├── Pasarela de pagos
                                              ├── Correo electrónico
                                              ├── WebSocket
                                              └── Mapas (opcional)
```

No existirán backends independientes para el marketplace y el dashboard
en el MVP.

Las reglas críticas serán validadas en el backend; el frontend no será
considerado autoridad para stock, capacidad, pagos, autorización o
estados de pedidos.

------------------------------------------------------------------------

## 5. Módulos funcionales

La primera división propuesta es:

``` text
com.jaldishop
├── identity
├── store
├── catalog
├── inventory
├── capacity
├── cart
├── checkout
├── payment
├── ordering
└── shared
```

### 5.1 Responsabilidades

  -----------------------------------------------------------------------
  Módulo                              Responsabilidad principal
  ----------------------------------- -----------------------------------
  `identity`                          Usuarios, roles, autenticación y
                                      autorización.

  `store`                             Tienda y configuraciones propias
                                      del comercio.

  `catalog`                           Categorías, productos, variantes y
                                      conceptos comerciales asociados.

  `inventory`                         Stock y reglas de disponibilidad de
                                      inventario.

  `capacity`                          Capacidad operativa, excepciones y
                                      reservas temporales.

  `cart`                              Intención de compra y elementos del
                                      carrito.

  `checkout`                          Orquestación del proceso de compra.

  `payment`                           Pagos, intentos de pago e
                                      integración con la pasarela.

  `ordering`                          Pedidos y su ciclo de vida.

  `shared`                            Conceptos realmente compartidos
                                      entre módulos.
  -----------------------------------------------------------------------

> La distribución de módulos es una decisión arquitectónica. No
> determina todavía los límites definitivos de agregados DDD ni las
> cardinalidades del modelo de datos.

Funcionalidades como notificaciones, promociones, reseñas, favoritos,
delivery avanzado, analítica o mapas no se convertirán automáticamente
en módulos independientes. Se separarán únicamente si su complejidad lo
justifica.

------------------------------------------------------------------------

## 6. Arquitectura interna de un módulo

Cuando la complejidad lo requiera, un módulo podrá organizarse de la
siguiente forma:

``` text
module/
├── api/
├── domain/
├── application/
├── infrastructure/
└── web/
```

### 6.1 `domain`

Contendrá:

-   entidades;
-   Value Objects;
-   invariantes;
-   reglas de negocio;
-   contratos de repositorio;
-   conceptos propios del módulo.

El dominio deberá evitar dependencias directas con Controllers, SDK de
proveedores externos o detalles específicos de transporte HTTP.

### 6.2 `application`

Contendrá:

-   casos de uso;
-   servicios de aplicación;
-   coordinación de operaciones;
-   comandos y resultados cuando sean necesarios;
-   fronteras transaccionales.

Ejemplos:

``` text
ReserveCapacityUseCase
ConfirmPurchaseUseCase
CancelOrderUseCase
```

No se creará obligatoriamente una clase `UseCase` para cada operación
CRUD trivial. Consultas y operaciones simples podrán agruparse en
servicios de aplicación.

### 6.3 `infrastructure`

Contendrá implementaciones técnicas como:

-   Spring Data JPA;
-   adaptadores de repositorio;
-   clientes HTTP;
-   Mercado Pago;
-   correo electrónico;
-   mecanismos externos de infraestructura.

### 6.4 `web`

Contendrá la interfaz HTTP:

-   `@RestController`;
-   request DTO;
-   response DTO;
-   validación de entrada;
-   traducción HTTP ↔ aplicación.

Un Controller podrá depender de varios casos de uso relacionados. No
contendrá reglas de negocio ni accederá directamente a repositorios.

### 6.5 `api`

Será opcional y se utilizará cuando un módulo necesite exponer contratos
Java estables a otros módulos.

------------------------------------------------------------------------

## 7. Comunicación entre módulos

Los módulos se comunicarán dentro del mismo proceso Spring Boot.

Para el MVP no se utilizarán llamadas HTTP internas, Kafka ni RabbitMQ.

### 7.1 Comunicación síncrona

Cuando un proceso necesite una respuesta inmediata se utilizarán
contratos públicos de aplicación.

``` text
Checkout
    ↓
InventoryOperations
    ↓
Inventory
```

Ejemplos:

-   comprobar stock;
-   reservar capacidad;
-   consultar un pago aprobado;
-   crear un pedido.

Un módulo no deberá hacer:

``` text
Checkout → InventoryRepository        X
Checkout → InventoryJpaEntity         X
Checkout → JpaInventoryRepository     X
```

En su lugar:

``` text
Checkout
    ↓
InventoryOperations
    ↓
InventoryApplicationService
    ↓
InventoryRepository
```

### 7.2 Eventos internos

Los eventos podrán utilizarse posteriormente para comunicar hechos que
ya ocurrieron, por ejemplo:

``` text
PedidoConfirmado
PedidoCancelado
StockBajo
PagoAprobado
ReservaExpirada
```

Su mecanismo definitivo queda pendiente. No se convertirá cada
interacción entre módulos en un evento.

------------------------------------------------------------------------

## 8. Persistencia

JaldiShop utilizará:

-   PostgreSQL;
-   Spring Data JPA;
-   restricciones de integridad en base de datos.

### 8.1 Estrategia JPA pragmática

Para evitar duplicar todas las entidades, las entidades de dominio
podrán incorporar anotaciones JPA directamente.

Esto supone aceptar una dependencia pragmática del dominio respecto de
Jakarta Persistence a cambio de reducir la complejidad del proyecto.

Los casos de uso no dependerán directamente de `JpaRepository`.

``` text
Application
     ↓
Repository Port
     ↑
Repository Adapter
     ↓
Spring Data JPA
     ↓
PostgreSQL
```

Ejemplo conceptual:

``` text
ProductRepository
        ▲
        │ implements
ProductRepositoryAdapter
        │
        ▼
SpringDataProductRepository
        │
        ▼
PostgreSQL
```

### 8.2 Relaciones entre módulos

Se evitará crear grafos JPA excesivamente acoplados entre módulos.

Cuando sea apropiado, un módulo podrá mantener el identificador de un
concepto perteneciente a otro módulo en lugar de navegar directamente su
entidad Java.

Ejemplo conceptual:

``` text
Inventory
    └── variantId
```

La base de datos podrá mantener una Foreign Key aunque el modelo Java
preserve el límite modular.

Las relaciones JPA definitivas serán establecidas después de cerrar el
modelo de dominio y el modelo ER.

### 8.3 Integridad

PostgreSQL será la última línea de protección mediante mecanismos como:

-   `NOT NULL`;
-   `FOREIGN KEY`;
-   `UNIQUE`;
-   `CHECK`;
-   índices.

Ejemplo de invariante:

``` text
stock >= 0
```

------------------------------------------------------------------------

## 9. Gestión de transacciones

**Estado:** Propuesta aceptada / pendiente de refinamiento durante
implementación.

Las fronteras transaccionales se ubicarán principalmente en la capa de
aplicación.

``` text
Controller
    ↓
@Transactional
UseCase / ApplicationService
    ↓
Domain
    ↓
Repositories
```

Las transacciones deberán ser cortas.

No se mantendrá una transacción PostgreSQL abierta mientras:

-   el usuario completa un pago;
-   se espera una respuesta de Mercado Pago;
-   se realiza una operación externa potencialmente lenta.

### 9.1 Confirmación de compra

La confirmación local de una compra deberá comportarse como una
operación atómica.

``` text
BEGIN

verificar pago aprobado
verificar reserva
proteger / descontar inventario
crear pedido
comprometer capacidad

COMMIT
```

Si una operación interna falla:

``` text
ROLLBACK

Pedido       → no confirmado
Inventario   → sin cambio parcial
Capacidad    → sin cambio parcial
```

------------------------------------------------------------------------

## 10. Concurrencia

Una transacción por sí sola no garantiza que dos usuarios no consuman
simultáneamente el último recurso.

JaldiShop aplicará mecanismos adicionales en operaciones críticas.

### 10.1 Capacidad

La creación de reservas podrá utilizar bloqueo pesimista durante una
transacción corta.

``` text
Cliente A
    ↓
LOCK capacidad del periodo
    ↓
comprobar disponibilidad
    ↓
crear ReservaCapacidad
    ↓
COMMIT
    ↓
UNLOCK
```

La reserva de capacidad de negocio y el lock de base de datos son
conceptos distintos:

``` text
Lock PostgreSQL       → milisegundos
ReservaCapacidad      → hasta 10 minutos
```

El lock nunca deberá mantenerse durante los diez minutos de reserva.

### 10.2 Inventario

La estrategia definitiva queda en revisión.

Se consideran:

-   bloqueo pesimista;
-   actualización SQL atómica;
-   optimistic locking mediante `@Version`;
-   restricciones de base de datos.

El sistema deberá garantizar que el stock nunca resulte negativo.

### 10.3 Idempotencia

Las operaciones críticas deberán impedir efectos duplicados.

Ejemplos:

-   un mismo pago no deberá generar dos pedidos;
-   un webhook repetido no deberá procesarse dos veces;
-   una confirmación repetida no deberá descontar inventario nuevamente.

Se utilizarán identificadores únicos y restricciones de base de datos
cuando corresponda.

------------------------------------------------------------------------

## 11. Checkout

Checkout será responsable de coordinar el proceso de compra.

En la versión inicial no tendrá un modelo de dominio propio porque
representa un proceso de aplicación y no una entidad independiente del
negocio.

``` text
checkout/
├── application/
│   ├── StartCheckoutUseCase
│   ├── ConfirmPurchaseUseCase
│   └── CancelCheckoutUseCase
└── web/
    ├── CheckoutController
    ├── request/
    └── response/
```

Checkout podrá consumir contratos públicos de:

``` text
Cart
Inventory
Capacity
Payment
Ordering
```

pero no sus repositorios o implementaciones internas.

------------------------------------------------------------------------

## 12. Flujo técnico de compra

El flujo general propuesto es:

``` text
CARRITO
   │
   ▼
StartCheckoutUseCase
   │
   ├── validar carrito
   ├── validar productos/precios
   ├── validar stock
   └── reservar capacidad
           │
           ▼
      TX PostgreSQL
           │
           ├── crear ReservaCapacidad
           └── crear Pago PENDIENTE
           │
         COMMIT
           ▼
      INICIAR PAGO
           │
           ▼
       PASARELA
           │
           ▼
        WEBHOOK
           │
           ▼
ProcessPaymentResultUseCase
           │
           ▼
     Pago APROBADO
           │
           ▼
ConfirmPurchaseUseCase
           │
      TX PostgreSQL
           │
           ├── verificar pago
           ├── verificar reserva
           ├── descontar inventario
           ├── crear pedido
           └── comprometer capacidad
           │
         COMMIT
           ▼
    PEDIDO CONFIRMADO
```

### 12.1 Primera transacción: preparación

``` text
BEGIN
  validar carrito
  validar stock
  reservar capacidad
  crear pago pendiente
COMMIT
```

### 12.2 Operación externa

``` text
COMMIT
   ↓
Pasarela de pagos
   ↓
resultado verificado por backend
```

### 12.3 Segunda transacción: confirmación

``` text
BEGIN
  verificar pago aprobado
  verificar reserva
  descontar inventario
  crear pedido
  comprometer capacidad
COMMIT
```

El frontend no será la autoridad para declarar un pago como aprobado.

------------------------------------------------------------------------

## 13. Reservas de capacidad durante checkout

En el MVP:

``` text
1 pedido = 1 cupo
```

La capacidad podrá configurarse por día o por franja horaria.

La disponibilidad se entiende conceptualmente como:

``` text
Capacidad disponible =
Capacidad efectiva
- Capacidad reservada
- Capacidad comprometida
```

La reserva temporal tendrá inicialmente una duración fija de:

``` text
10 minutos
```

Si el checkout se abandona o el pago falla, la capacidad deberá
liberarse.

Si un pago válido fue iniciado antes del vencimiento, la reserva deberá
mantenerse protegida durante un periodo controlado mientras se determina
el resultado del pago. El mecanismo y timeout definitivo se encuentran
pendientes de diseño.

------------------------------------------------------------------------

## 14. Pagos

**Estado de la pasarela:** Propuesta.

La opción inicial propuesta es **Mercado Pago**.

La integración permanecerá encapsulada dentro del módulo `payment`.

### 14.1 Contrato externo

El módulo definirá un contrato propio:

``` text
PaymentGateway
```

Infraestructura podrá implementar:

``` text
PaymentGateway
      ▲
      │
      ├── MercadoPagoAdapter
      └── FakePaymentAdapter
```

El resto del sistema no dependerá directamente del SDK o estructuras
propias de Mercado Pago.

### 14.2 Fronteras

Se diferencian dos tipos de contratos:

``` text
Checkout
    ↓
PaymentOperations
    ↓
PAYMENT
    ↓
PaymentGateway
    ↓
Mercado Pago
```

-   `PaymentOperations`: operaciones que Payment expone a otros módulos.
-   `PaymentGateway`: operaciones externas que Payment necesita de una
    pasarela.

### 14.3 Confirmación

El frontend podrá participar en la experiencia de pago, pero no
determinará su resultado definitivo.

La confirmación se realizará mediante información verificada por el
backend, por ejemplo mediante webhook y/o consulta a la pasarela.

Los webhooks deberán procesarse de forma idempotente.

------------------------------------------------------------------------

## 15. Acciones reversibles y compensación

Un rollback PostgreSQL solamente puede revertir cambios realizados
dentro de la base de datos local.

No puede revertir automáticamente una operación financiera ya realizada
en una pasarela externa.

Por ello:

``` text
Pago aprobado externamente
        ↓
falla confirmación local
        ↓
registrar inconsistencia
        ↓
reconciliación / compensación
        ↓
anulación o reembolso cuando corresponda
```

Las devoluciones financieras serán tratadas como nuevas operaciones
compensatorias, no como rollback de la transacción original.

Las llamadas de reembolso no deberán mantenerse dentro de una
transacción de base de datos prolongada.

------------------------------------------------------------------------

## 16. Cancelaciones

Una cancelación de pedido y un reembolso financiero serán conceptos
relacionados pero distintos.

Ejemplo conceptual:

``` text
CancelOrderUseCase
      ↓
TX local
      ├── validar cancelación
      ├── cambiar estado del pedido
      ├── restaurar inventario si corresponde
      └── liberar capacidad si corresponde
      ↓
COMMIT

      ↓

RefundPaymentUseCase
      ↓
PaymentGateway
      ↓
Pasarela
```

Según las reglas de negocio actuales, la capacidad podrá liberarse
automáticamente cuando el pedido aún no haya iniciado preparación. Una
vez iniciado `EN_PREPARACION`, no se liberará automáticamente.

Los estados y políticas definitivas de reembolso quedan pendientes de
refinamiento.

------------------------------------------------------------------------

## 17. API REST y Controllers

Spring MVC será utilizado para exponer la API REST.

Los Controllers:

-   recibirán solicitudes HTTP;
-   validarán datos de entrada;
-   convertirán request DTO a comandos;
-   ejecutarán casos de uso o servicios de aplicación;
-   convertirán resultados a response DTO;
-   devolverán códigos HTTP apropiados.

No deberán:

-   contener reglas de negocio;
-   acceder directamente a repositorios;
-   coordinar manualmente varios módulos cuando exista un caso de uso
    responsable de ello.

Ejemplo:

``` text
CheckoutController
        ↓
StartCheckoutUseCase
        ↓
Cart / Inventory / Capacity / Payment
```

Un Controller puede depender de múltiples casos de uso relacionados.

Para CRUD y consultas simples se podrán utilizar servicios de aplicación
agrupados en lugar de crear una clase por cada operación.

------------------------------------------------------------------------

## 18. Integraciones externas

Las integraciones externas se realizarán desde infraestructura mediante
contratos definidos por el sistema.

Integraciones previstas:

  Integración          Estado
  -------------------- -----------------
  Mercado Pago         Propuesta
  Correo electrónico   Pendiente
  WebSocket            Pendiente
  Mapas                Opcional / Plus

Los mapas no forman parte del núcleo obligatorio del MVP y no deberán
condicionar la arquitectura central.

------------------------------------------------------------------------

## 19. Seguridad

**Estado:** Pendiente.

La arquitectura deberá contemplar como mínimo:

-   autenticación;
-   JWT;
-   Spring Security;
-   roles de cliente, comerciante y administrador;
-   autorización por recurso;
-   aislamiento lógico entre tiendas.

No será suficiente comprobar únicamente que un usuario posea el rol
`MERCHANT`.

El backend deberá impedir que un comerciante modifique recursos
pertenecientes a otra tienda.

La estrategia concreta se definirá en la siguiente revisión
arquitectónica.

------------------------------------------------------------------------

## 20. WebSocket y notificaciones

**Estado:** Pendiente.

REST será el mecanismo principal para comandos y consultas.

WebSocket podrá utilizarse como canal complementario para
actualizaciones en tiempo real, especialmente:

-   cambios de estado de pedidos;
-   nuevos pedidos para comerciantes;
-   otras notificaciones que justifiquen tiempo real.

WebSocket no sustituirá a REST para operaciones principales del sistema.

La estrategia de eventos, persistencia de notificaciones y entrega en
tiempo real queda pendiente.

------------------------------------------------------------------------

## 21. Decisiones pendientes

Para la versión 0.2 deberán revisarse principalmente:

1.  Seguridad con Spring Security y JWT.
2.  Autorización y aislamiento lógico por tienda.
3.  Estrategia definitiva de concurrencia para inventario.
4.  Relaciones JPA definitivas después del modelo de dominio/ER.
5.  Estrategia de expiración de reservas.
6.  Timeout de protección de una reserva durante procesamiento de pago.
7.  Configuración técnica definitiva de Mercado Pago.
8.  Procesamiento posterior a `PagoAprobado`: síncrono o mediante evento
    interno.
9.  Manejo de reembolsos y reconciliación.
10. WebSocket y notificaciones.
11. Patrones de diseño realmente utilizados.
12. Estrategia de migraciones y DDL PostgreSQL.

------------------------------------------------------------------------

## 22. Fuera de alcance arquitectónico del MVP

No se diseñarán inicialmente:

-   microservicios;
-   Kafka o RabbitMQ;
-   base de datos independiente por módulo;
-   infraestructura multi-tenant avanzada;
-   capacidad ponderada;
-   múltiples recursos o estaciones de producción;
-   tracking GPS en tiempo real;
-   optimización de rutas;
-   inteligencia artificial;
-   integración directa con redes sociales;
-   facturación electrónica/SUNAT;
-   almacenamiento de datos sensibles de tarjetas;
-   arquitectura distribuida de pagos.

Estas capacidades podrán evaluarse como evolución futura.

------------------------------------------------------------------------

## 23. Resumen de decisiones v0.1

La arquitectura actual de JaldiShop puede resumirse como:

``` text
                        CLIENTES
             ┌────────────┴────────────┐
             │                         │
      Next.js Marketplace       Angular Dashboard
             │                         │
             └──────────┬──────────────┘
                        │
                  HTTPS / REST
                        │
                        ▼
              SPRING BOOT BACKEND
                 Monolito Modular
                        │
      ┌─────────────────┼─────────────────────┐
      │                 │                     │
   Catalog          Checkout              Ordering
 Inventory        /    |    \              Payment
 Capacity       Cart Capacity Payment         │
      │                                   PaymentGateway
      │                                         │
      └────────────── PostgreSQL          Mercado Pago
```

Principios centrales:

``` text
Monolito modular
+
separación interna pragmática
+
contratos entre módulos
+
PostgreSQL compartido
+
transacciones locales cortas
+
concurrencia en recursos críticos
+
idempotencia
+
integraciones externas mediante adapters
+
compensación para operaciones externas reversibles
```

------------------------------------------------------------------------

## 24. Historial del documento

  -----------------------------------------------------------------------
  Versión                 Fecha                   Descripción
  ----------------------- ----------------------- -----------------------
  0.1                     2026-09-03              Arquitectura inicial:
                                                  sistema, módulos,
                                                  persistencia,
                                                  transacciones,
                                                  concurrencia, checkout
                                                  y pagos.

  -----------------------------------------------------------------------

### Próxima versión prevista

**v0.2**

Incorporará las decisiones correspondientes a seguridad, autorización
por tienda y, de ser posible, WebSocket/notificaciones, además de los
ajustes surgidos de la revisión del equipo.
