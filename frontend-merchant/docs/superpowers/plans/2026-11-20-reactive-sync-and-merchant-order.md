# Sincronización Reactiva Multimódulo y Comanda de Mostrador - Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar un Invalidation Bus reactivo unificado, estrategia Stale-While-Revalidate (SWR) multimódulo, Optimistic UI en transiciones de pedidos y restricción formal del modal de pedidos a comandas directas de MOSTRADOR/LOCAL.

**Architecture:** Capa centralizada `InvalidationBusService` basada en RxJS/Signals para desacoplar `OrderService`, `InventoryService`, `CapacityService` y `AnalyticsService`. WebSocket (STOMP) y mutaciones locales emiten al bus para invalidar atómicamente la memoria e iniciar revalidación transparente (Cache-Then-Network) sin bloquear vistas.

**Tech Stack:** Angular 22, TypeScript 5.8+, Signals, RxJS, STOMP/WebSockets, Tailwind CSS v4, Vitest.

---

## Global Constraints
- Mantener compatibilidad total con la suite actual (mínimo 858 tests pasando).
- Cero violaciones en `echo "y" | /home/ijosueeh/.agents/skills/impeccable/scripts/impeccable detect src`.
- `npm run build` exitoso sin errores de tipos o bundles rotos.
- No introducir dependencias circulares entre servicios de dominio.

---

## Fases de Implementación

### Fase 1: Creación de Invalidation Bus (`InvalidationBusService`)
- [x] Crear `src/app/core/services/invalidation-bus.service.ts` con eventos tipados:
  - `orderCreated$`
  - `orderMutated$`
  - `inventoryChanged$`
  - `capacityUpdated$`
  - `reconnected$`
- [x] Crear suite de pruebas unitarias `src/app/core/services/invalidation-bus.service.spec.ts`.
- [x] Ejecutar tests unitarios de Fase 1 hasta verde.

### Fase 2: Conexión STOMP y Mutaciones Locales al Invalidation Bus
- [x] Conectar `StompService` para despachar `ORDER_CONFIRMED` y `ORDER_STATUS_CHANGED` hacia `InvalidationBusService`.
- [x] Conectar `OrderService.createMerchantOrder` para despachar `orderCreated$` al registrar comanda directa.
- [x] Adaptar `OrderService` para emitir a través de `InvalidationBusService`.
- [x] Ejecutar suite de pruebas de `stomp.service.spec.ts` y `order.service.spec.ts`.

### Fase 3: Optimistic UI con Reconciliación Segura en `OrderService`
- [x] Implementar actualización optimista inmediata en `OrderService.updateOrderStatus`:
  - Modificar Signals `orders`, `selectedOrder` y `selectedOrderDetail` en < 5ms.
  - Notificar cambio al `InvalidationBusService` de forma inmediata.
- [x] Implementar mecanismo de rollback seguro y `pendingVerificationOrderIds` ante errores HTTP 4xx/5xx/red.
- [x] Añadir pruebas unitarias de Optimistic UI y rollback en `order.service.spec.ts`.
- [x] Verificar que todos los tests de órdenes sigan pasando.

### Fase 4: Stale-While-Revalidate (SWR) Multimódulo (Inventory, Capacity, Dashboard, Finances)
- [x] Refactorizar `InventoryService`:
  - Suscribirse a `InvalidationBusService` (`orderCreated$`, `orderMutated$`, `reconnected$`, `inventoryChanged$`).
  - Implementar SWR al entrar a `/inventory`: servir caché en 0ms y revalidar en segundo plano.
- [x] Refactorizar `CapacityService`:
  - Suscribirse a `InvalidationBusService` para invalidar y revalidar ocupación real inmediatamente.
  - Implementar SWR al entrar a `/capacity` o `/dashboards`.
- [x] Refactorizar `AnalyticsService`:
  - Suscribirse a `InvalidationBusService` para invalidar métricas financieras cuando un pedido pase a `COMPLETED` o `CANCELLED`.
  - Refrescar al entrar a `/finances` si el caché está marcado como sucio.
- [x] Ajustar `PriorityOrders` y `DashboardKpis` para ejecutar revalidación SWR en `ngOnInit`.
- [x] Ejecutar pruebas de `inventory.service.spec.ts`, `capacity.service.spec.ts`, `analytics.service.spec.ts` y componentes de dashboard.

### Fase 5: Restricción y Pulido de Comanda Directa (`create-order-modal`)
- [x] Configurar canales de `create-order-modal`:
  - Forzar canal a `MOSTRADOR / LOCAL`, `WHATSAPP` o `TELÉFONO`.
  - Modalidad predeterminada `PICKUP` (Recojo en local) y entrega directa propia.
  - Métodos de pago manuales (`EFECTIVO`, `YAPE`, `PLIN`, `POS_TARJETA`).
- [x] Garantizar consistencia con ticket térmico y contratos backend.
- [x] Ejecutar suite completa `npm test -- --watch=false`, `npm run build` e Impeccable detect.
