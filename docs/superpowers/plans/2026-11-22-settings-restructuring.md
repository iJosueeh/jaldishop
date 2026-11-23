# Plan de Reestructuración UI/UX y Persistencia Backend de `/settings`

> **Para trabajadores agenticos:** SUB-SKILL REQUERIDO: Usar `superpowers:subagent-driven-development` o `superpowers:executing-plans` para implementar este plan tarea por tarea. Cada paso usa la sintaxis de checklist (`- [ ]`) para seguimiento.

**Meta:** Rediseñar la sección `/settings` del merchant para eliminar sobrecarga cognitiva y bloat (suprimir la pestaña de WhatsApp, eliminar configuración técnica de Mercado Pago y transferencias manuales, desacoplar liquidaciones hacia Finanzas), organizando la configuración en 3 pestañas limpias con diseño Impeccable (Dark/Teal-Mint de Shopify), persistencia en Spring Boot 3 con Arquitectura Hexagonal y sincronización reactiva en Angular 22 Signals.

**Arquitectura:**
- **Frontend (Angular 22)**: Standalone Components, Signals puros, CDK A11y, Tailwind CSS con tokens semánticos definidos en `DESIGN.md`, invalidación reactiva mediante `InvalidationBusService` y rollback optimista en mutaciones.
- **Backend (Spring Boot 3 + DDD / Hexagonal)**: Entidad `StoreSettingsEntity` en Postgres con Flyway `V33`, endpoints REST `GET/PUT /api/v1/stores/me/settings`, Value Objects desacoplados y casos de uso `GetStoreSettingsUseCase` / `UpdateStoreSettingsUseCase`.

**Tech Stack:** Angular 22, TypeScript, Tailwind CSS, Material Symbols (@ng-icons), Vitest, Spring Boot 3, Java 21, PostgreSQL, Flyway, JPA / Hibernate.

---

## Restricciones Globales y Criterios de Diseño (Impeccable & UX)

1. **Modo Operate (Admin / Settings)**: Escaneabilidad inmediata, jerarquía clara y sin abrumar al comerciante. Menos configuración inútil, más control directo.
2. **Eliminación Total de WhatsApp Tab**: Se elimina la pestaña de plantillas de WhatsApp. En su lugar, `order.service.ts` usará plantillas estandarizadas canónicas de plataforma con los tokens automáticos `{cliente}`, `{pedido}`, `{tienda}`, `{total}`, eliminando la necesidad de que el comerciante redacte mensajes manualmente.
3. **Desacoplo de Cobro vs. Liquidación**:
   - **Cobro al Cliente (Checkout)**: Solo dos opciones en la tienda: **Pagos Online JaldiShop** (tarjeta / Yape centralizado sin pedir public key ni sandbox) y **Pago Contra Entrega** (efectivo / POS).
   - **Cuenta de Liquidación (Payouts)**: Enlace prominente y contextual hacia Finanzas (`/finances`), donde reside la gestión de CCI/cuentas bancarias.
4. **Pausa Rápida de Tienda**: Se reubica en la cabecera principal de `Mi Tienda` (`/store`) y/o `header.html` de merchant, permitiendo pausar la tienda con 1 clic sin tener que entrar a ajustes.
5. **Consolidación en 3 Pestañas Claras**:
   - **Pestaña 1: Cobros y Despacho**: Métodos de cobro del cliente (Online vs Contra Entrega), monto mínimo de delivery y tiempo de preparación referencial.
   - **Pestaña 2: Políticas y Checkout**: Términos de cancelación y devoluciones conforme a normativa INDECOPI (con templates sugeridos de 1 clic), requisitos de DNI/RUC del cliente y notas de pedido.
   - **Pestaña 3: Alertas y Sonidos**: Alerta sonora de pedidos WebSocket, control de volumen con slider táctil y botón de prueba de audio, y avisos visuales de stock crítico.
6. **Persistencia Real**: Eliminación completa de `localStorage` para settings; consumo directo de API REST `GET/PUT /api/v1/stores/me/settings`.

---

## Foco de Revisión (Review Focus)

1. **Rollback Optimista**: Si la llamada PUT al backend falla por red o validación, la UI debe restaurar inmediatamente el estado anterior y mostrar un Toast de error.
2. **Invalidación de Caché Multi-Módulo**: Al guardar settings, debe dispararse `InvalidationBusService.dispatchCacheInvalidated({ storeId, module: 'STORE' })` para que otras vistas del merchant actualicen sus datos sin F5.
3. **Compatibilidad en `OrderService`**: Al remover plantillas de WhatsApp de los settings, la función `notifyCustomerWhatsApp(order)` debe operar con defaults robustos sin lanzar errores de runtime.
4. **Validación de INDECOPI en Políticas**: Los botones de plantilla rápida deben rellenar textos conformes a la legislación peruana de protección al consumidor sin borrar texto previamente personalizado sin confirmación.
5. **Accesibilidad**: Los switches deben usar `role="switch"`, `aria-checked`, soporte para teclado (`Space`/`Enter`) y contraste accesible según las pautas de `DESIGN.md`.

---

## Plan de Tareas Detallado

### Tarea 1: Migración Flyway y Persistencia Backend (Spring Boot 3)
- [ ] Crear migración Flyway `backend/src/main/resources/db/migration/V33__create_store_settings_table.sql`.
- [ ] Implementar modelo de dominio `StoreSettings.java` y puerto `StoreSettingsRepository.java`.
- [ ] Implementar `StoreSettingsEntity.java`, `StoreSettingsJpaRepository.java`, mapper y adapter.

### Tarea 2: Casos de Uso y API REST Backend (`/api/v1/stores/me/settings`)
- [ ] Implementar DTOs `StoreSettingsRequest.java` y `StoreSettingsResponse.java`.
- [ ] Implementar casos de uso `GetStoreSettingsUseCase` y `UpdateStoreSettingsUseCase` con inicialización lazy si no existe.
- [ ] Implementar `StoreSettingsController.java` (`GET` y `PUT`).
- [ ] Crear pruebas unitarias del controlador en Spring Boot.

### Tarea 3: Refactorización Core Frontend (Models & SettingsService con Signals)
- [ ] Actualizar `settings.models.ts` eliminando plantillas WhatsApp y datos técnicos obsoletos.
- [ ] Refactorizar `settings.service.ts` para conectar con API REST con Signals, invalidando `InvalidationBusService` y aplicando rollback optimista.
- [ ] Actualizar `order.service.ts` para proveer mensajes canónicos de WhatsApp automáticos.
- [ ] Actualizar `settings.service.spec.ts`.

### Tarea 4: Limpieza de Tabs Obsoletos y Reorganización de UI
- [ ] Eliminar carpetas obsoletas (`settings-general-tab`, `settings-orders-tab`, `settings-whatsapp-tab`, etc.).
- [ ] Implementar `SettingsPaymentsDeliveryTab` (Online JaldiShop, COD, POS, min delivery, tiempo prep y link a Finanzas).
- [ ] Implementar `SettingsPoliciesTab` (Cancelación y Devoluciones INDECOPI de 1-clic, DNI/RUC toggle).
- [ ] Implementar `SettingsAlertsTab` (Alertas sonoras WebSocket, volumen slider, prueba sonora y stock visual).
- [ ] Actualizar `settings.ts` y `settings.html` con las 3 pestañas y tokens Impeccable (Dark/Teal-Mint de Shopify).

### Tarea 5: Verificación y Tests Unitarios
- [ ] Tests unitarios en componentes de pestañas y componente principal con Vitest.
- [ ] Test suite Maven en backend.
- [ ] Ejecución de `impeccable detect` y documentación de walkthrough.
