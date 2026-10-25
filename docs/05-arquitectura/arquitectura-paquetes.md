# Arquitectura de Paquetes (Backend)

### JaldiShop · Convención de organización Java y reglas de dependencia

[![Estado](https://img.shields.io/badge/Estado-Aprobado-green?style=for-the-badge&logo=checkmarx&logoColor=white)](./arquitectura-paquetes.md)
[![Fase](https://img.shields.io/badge/Fase-F0_Documentaci%C3%B3n-blue?style=for-the-badge)](./arquitectura-paquetes.md)

---

`Docs` > `05-Arquitectura` > **Arquitectura de Paquetes**
[Arquitectura del Sistema](./arquitectura-sistema.md) | [Índice General](../index.md) | [Modelo de Dominio](../03-requisitos/modelo-dominio.md)

---

## 1. Propósito

Este documento define la **convención de paquetes del backend** (`com.jaldishop.backend`), acordada por el equipo tras la auditoría de organización de paquetes (345 clases `main` / 114 `test` en F0), y las **reglas de dependencia** permitidas entre módulos.

Distingue claramente entre:

- **Reglas deseadas:** cómo debe organizarse el código *a partir de ahora*.
- **Deuda existente:** problemas arquitectónicos ya presentes en `main`, registrados con evidencia y pendientes de tarjeta propia. **No son la convención** y no deben copiarse como patrón.

Alcance de este documento: solo convención y deuda. **No implica por sí mismo ningún movimiento de clases**; cada reorganización se ejecuta en una fase propia (F1–F5) con aprobación previa.

---

## 2. Convención aprobada

### 2.1 Estructura de módulos y capas (inalterable)

El backend es un **modular monolith**: paquete por funcionalidad en el nivel 1, capas en el nivel 2.

```
com.jaldishop.backend.{modulo}
├── domain/            # Entidades, value objects, enums y puertos (interfaces de repositorio)
├── application/       # Casos de uso (servicios), inputs (commands/queries), outputs (results)
├── infrastructure/    # Adaptadores: persistence/{adapter,entity,mapper,repository}, security, cloudinary
└── web/               # Controllers, dto/{request,response}, mapper
```

Módulos actuales: `capacity`, `catalog`, `checkout`, `identity`, `inventory`, `cart`, `ordering`, `notification`, `media`, `store`, `shared`.

Reglas fijas:

1. Los 4 niveles de capa existen siempre en cada módulo (aunque una capa hoy no tenga clases, como `inventory/web`).
2. `infrastructure/persistence` conserva su subestructura de 4 subpaquetes: `adapter`, `entity`, `mapper`, `repository`.
3. Los puertos de repositorio (`XxxRepository`) viven **siempre en `domain`**.
4. No se crean módulos nuevos sin un caso de negocio real que lo justifique.

### 2.2 Organización interna de `application`

1. **`application` es plano por defecto.** Todas las clases del caso de uso viven directamente en `com.jaldishop.backend.{modulo}.application`.
2. **Subdivisión por responsabilidad funcional**, nunca por tipo de clase, cuando exista **suficiente cohesión**: un subdominio con responsabilidad y razón de cambiar propias.
3. **El umbral de clases es orientativo, no obligatorio**: como referencia, ~15 clases y ≥ 2 grupos cohesivos de ≥ 4 clases. La decisión final la da la cohesión real, no el número.
4. **Prohibido agrupar por tipo:** no existirán carpetas `service/`, `command/`, `query/`, `result/`, `port/` ni similares dentro de `application`.
5. Decisión por módulo (aprobada):
   - **`capacity/application` → subdividir** en `configuration/`, `reservation/` y `availability/`. *(Aprobado y **ejecutado en `main`** vía PR #65: las clases ya viven en esos subpaquetes — 12 / 5 / 9.)*
     - `configuration/` (~12): creación/actualización/toggle/consulta de configuraciones y excepciones de capacidad.
     - `reservation/` (~5): crear, confirmar, liberar y consultar reservas de capacidad.
     - `availability/` (~9): motor de disponibilidad (`EffectiveCapacity*`, `GetCapacityAvailabilityService`, `ListStoreCapacitySlotsService`, `CapacitySlotAvailability`).
   - **`store`, `identity`, `cart` y `checkout` → sin subdivisiones por ahora.** Reevaluar solo si crecen y aparecen grupos cohesivos reales.

### 2.3 Sufijos de clase (nivel 3 — por rol, no por carpeta)

| Sufijo | Rol | Regla |
|---|---|---|
| `XxxService` | Caso de uso en `application` | Siempre, en todos los módulos (110 actuales, 100% consistente) |
| `XxxCommand` | Input de escritura | Solo si el caso de uso tiene ≥ 2 parámetros o se espera crecer |
| `XxxQuery` | Input de lectura | Ídem; no crear queries de un solo campo |
| `XxxResult` / `XxxView` | Output de caso de uso / proyección de lectura | `Result` para casos de uso; `View` para proyecciones |
| `XxxRequest` / `XxxResponse` | DTO web | Solo en `web/dto` |
| `XxxRepository` | Puerto en `domain` | Implementado por `XxxRepositoryAdapter` (infra) sobre `XxxJpaRepository` |
| `XxxEntity` / `XxxPersistenceMapper` | Persistencia JPA | Solo en `infrastructure/persistence` |

Reglas complementarias:

- **DTO web:** factory estática `from(...)` cuando el mapeo es 1:1 con un registro; **clase mapper** solo cuando agrega o compone campos de varias fuentes.
- **Validadores:** Bean Validation (`@ValidXxx`) para sintaxis de entrada web; servicio estático en `domain` para reglas de negocio. No crear nuevos paquetes `web/validation`.

---

## 3. Reglas de dependencia entre módulos

Matriz permitida (de izquierda a derecha desde cualquier clase):

| Desde → Hacia | ¿Permitido? | Condición |
|---|---|---|
| cualquier módulo → `shared.*` | ✅ Siempre | — |
| `X.application` / `X.web` → `Y.domain` | ✅ Sí | Solo si no crea ciclo entre módulos |
| `X.web` → `Y.application` | ✅ Sí | Patrón de consumo (controllers usando servicios ajenos) |
| `X.application` → `Y.application` | ⚠️ Solo orquestadores declarados | Hoy: `checkout → capacity/inventory/ordering`, `store → identity` (implementación del puerto `identity.application.StoreProvisioning` desde `store.application` para el registro de merchant; antes `identity → store`, excepción retirada en E4). Prohibido en `domain` e `infrastructure` |
| cualquier módulo → `Y.infrastructure` | ❌ No | **DEUDA-ARC-03 resuelta (E1)**: `JwtPrincipal` ahora en `shared/security`. Permanece `store.infrastructure → identity.infrastructure` (`UserEntity`/`UserJpaRepository`), registrado como vuelta de identity↔store en **DEUDA-ARC-02** (ver §4) |
| cualquier módulo → `Y.web` | ❌ Nunca | Sin violaciones desde **E2**: **DEUDA-ARC-01** resuelta con `StoreCustomerSummary` (ver §4) |

Reglas complementarias:

- **Ciclos entre módulos prohibidos.** Hoy existen 2 (§4); no se introduzcan nuevos.
- **Los módulos no deben conocer los paquetes internos de otros módulos más allá de `domain`** salvo la excepción de orquestadores.
- La verificación de esta matriz es **manual hoy**; se contempla un test de arquitectura automatizado en una fase posterior (F5).

---

## 4. Deuda existente (problemas pendientes — NO son la convención)

Registrada con evidencia de archivos. Los puntos resueltos por la rama `refactor/backend-architecture-cleanup` se marcan ✅ con su etapa (E1–E3) y se conservan como histórico; **los pendientes no están resueltos ni deben entenderse como patrón a seguir**.

### DEUDA-ARC-01 — DTO web dentro de `store` (capas invertidas) — ✅ Resuelto (E2)

| Archivo (antes de E2) | Capa | Importaba |
|---|---|---|
| `store/domain/StoreCustomerRepository.java:3` | domain | `store.web.dto.StoreCustomerResponse` |
| `store/application/GetStoreCustomersService.java:4` | application | `store.web.dto.StoreCustomerResponse` |
| `store/infrastructure/persistence/adapter/StoreCustomerRepositoryAdapter.java:6` | infrastructure | `store.web.dto.StoreCustomerResponse` |

La interfaz de repositorio del dominio devolvía una respuesta HTTP. Su arreglo **exigía cambiar una interfaz pública** → por eso quedó como tarjeta propia. **Resuelto en E2:** el dominio expone el read-model `store/domain/StoreCustomerSummary` (`findCustomersByStoreId` devuelve `List<StoreCustomerSummary>`), `GetStoreCustomersService` y el adaptador devuelven ese tipo, y el mapeo a DTO vive en `store/web/mapper/StoreCustomerResponseMapper` (patrón `AdminStoreResponseMapper`). Hoy: 0 imports de `store.web.dto` fuera de `web`.

### DEUDA-ARC-02 — Ciclos de módulo (2)

| Ciclo | Aristas causantes | Evaluación |
|---|---|---|
| **identity ↔ store** | **Inversión ejecutada en E4:** `identity/application/RegisterMerchantService` ya no importa `store` (puerto `identity.application.StoreProvisioning`, implementado en `store.application` y declarado en §3; antes → `store.application.{CreateStoreCommand, CreateStoreService}`). **Pendiente:** `identity/web/mapper/AdminUserResponseMapper` → `store.domain.{Store, StoreRepository}` (no tocado en E4); vuelta por `store.*` → `identity.*` (`UserRepository`, `UserJpaRepository`/`UserEntity`, `store.application → identity.application`) | **El par sigue existiendo a nivel de módulo** y no se afirma eliminado. La deuda restante es la capa `web` de identity mostrando datos de tienda; inversión aprobada en **D1**, cierre total pendiente |
| **checkout ↔ inventory** | `inventory/application/ReserveInventoryService` → `checkout.domain.CheckoutItemSnapshot`; vuelta por `InitiateCheckoutService`/`ConfirmPurchaseService` | Decisión **D2** pendiente: la propuesta revisada es un **contrato independiente en `inventory/domain`** (`InventoryReservationItem`), **no** mover `CheckoutItemSnapshot` a inventory (invertiría la semántica: precios/presentación en el módulo de stock). *No implementado.* |
| **store ↔ media** | ✅ **Roto en E3:** `store/application/CloseMyStoreService` ya no importa `media`; usa el puerto `store/domain/StoreMediaCleaner`, implementado por `media/application/StoreMediaCleanupAdapter`. Se conserva la dirección permitida `media → store` (`X.application` → `Y.domain`) | Sin ciclo: queda una sola arista (D3 ejecutada en E3) |

### DEUDA-ARC-03 — `JwtPrincipal` en infraestructura — ✅ Resuelto (E1)

`identity/infrastructure/security/JwtPrincipal` era importado por **7 controllers** de capacity, cart, catalog, checkout, media, notification y store: toda la API dependía de la infraestructura de seguridad de identity (arista `→ Y.infrastructure`, prohibida en §3). **Resuelto en E1:** el archivo vive en `shared/security/JwtPrincipal` (35 archivos tocados, solo package/imports) y `shared.*` es siempre permitido (§3). Permanece la arista `store.infrastructure → identity.infrastructure` (`UserEntity`/`UserJpaRepository`), registrada como vuelta de identity↔store en DEUDA-ARC-02.

### Otras observaciones registradas

| ID | Hallazgo | Estado |
|---|---|---|
| OBS-ARC-01 | `ChangeOrderStatusService` sin test (único servicio de `ordering`) | Pendiente |
| OBS-ARC-02 | `ordering/web/**` (`CreateOrderRequest`, `OrderDeliveryValidator`, `ValidOrderDelivery`): código muerto (0 referencias externas) que duplica la regla viva `checkout/domain/CheckoutFulfillmentValidator` | Eliminación aprobada en principio (F1), **pendiente de confirmación con Mia** (territorio `ordering/**`); no eliminado |
| OBS-ARC-03 | No existe módulo `payment`: pagos embebidos en `checkout` (`PaymentEntity`, `PaymentReader`, `PaymentResume`) | Registrado, sin acción |
| OBS-ARC-04 | Doble estilo de validación de entrega (Bean Validation en `ordering/web` muerto vs. validador de dominio en checkout) | Se resuelve al ejecutar F1 |
| OBS-C1-01 | Pools de capacidad independientes entre ventanas solapadas (contabilidad por ventana exacta) | Preexistente; revisión con Mia (ticket propio) |

---

## 5. Fases de refactorización pendientes de aprobación

Solo documentación hoy (F0). F4 y F5 se ejecutaron **parcialmente** en la rama `refactor/backend-architecture-cleanup` (E1–E4); F1–F3 siguen sin ejecutarse.

| Fase | Cambio | Estado |
|---|---|---|
| **F0** | Este documento (convención + reglas + deuda) | **Ejecutado en esta tarjeta** |
| **F1** | Eliminar `ordering/web/**` (3 clases muertas) | Bloqueada: confirmación con Mia |
| **F2** | Mover `SlugUtils` de `catalog/application` a `catalog/domain` | Pendiente de aprobación |
| **F3** | Contrato independiente `InventoryReservationItem` en `inventory/domain` (reemplaza a la propuesta anterior de mover `CheckoutItemSnapshot`) | Pendiente de decisión D2 |
| **F4** | Ciclo identity↔store (puerto inverso, D1) | **Ejecutado parcialmente (E4):** puerto `StoreProvisioning` implementado e `identity/application` sin imports de `store`; el par de módulos sigue pendiente por `identity.web → store.domain` (DEUDA-ARC-02) |
| **F5** | Ciclo store↔media (D3) + `JwtPrincipal` → `shared/security` (DEUDA-ARC-03) + test de arquitectura de la matriz §3 | **Ejecutado parcialmente:** `JwtPrincipal` → `shared/security` (E1) y ciclo store↔media roto con `StoreMediaCleaner` (E3); test de arquitectura de §3 **pendiente** |

**Regla de oro por fase:** una fase = un PR pequeño y verificable (`mvnw clean test` con línea base actual de 572 tests verdes, `git diff` limitado a los archivos esperados). Si el diff supera lo estimado, se detiene y se re-planifica. Ninguna fase toca `SecurityConfig`, `GlobalExceptionHandler`, contratos REST, migraciones Flyway ni lógica de negocio.

---

## 6. Zonas con trabajo activo (no reorganizar sin coordinar)

| Zona | Motivo | Evidencia |
|---|---|---|
| `catalog/**` (backend) | Rama sin merge de Katherine: `origin/feature/BE-12-catalog-module` (4 commits, archivos propios de catálogo) | `git diff --name-only origin/main...origin/feature/BE-12-catalog-module` |
| `cart/**`, `ordering/**` | Territorio de Mia según DoD | DoD del equipo |
| `identity/infrastructure/security/**`, handlers de `shared/exception` | Compartidos en revisión HARDEN | Backlog `06-scrum/backlog-harden-seguridad.md` |
| `capacity/**` | Actividad reciente (C1 mergeada; OBS-C1-01 pendiente con Mia) | PR #63 |

---

## 7. Registro de cambios

| Fecha | Versión | Cambio |
|---|---|---|
| 2026-10-24 | v1.0 | F0: convención aprobada (§2), matriz de dependencias (§3), deuda existente DEUDA-ARC-01..03 y OBS (§4), plan de fases (§5), zonas activas (§6) |
| 2026-10-24 | v1.1 | Actualización tras E1–E4 de la rama `refactor/backend-architecture-cleanup`: `JwtPrincipal` → `shared/security` (E1, DEUDA-ARC-03 ✅), read-model `StoreCustomerSummary` (E2, DEUDA-ARC-01 ✅), puerto `StoreMediaCleaner` rompe store↔media (E3), inversión `StoreProvisioning` del registro (E4); matriz §3 con la arista `store → identity` declarada y la excepción `identity → store` retirada; ciclos 3 → 2; capacity subdivision reflejada (PR #65) |
