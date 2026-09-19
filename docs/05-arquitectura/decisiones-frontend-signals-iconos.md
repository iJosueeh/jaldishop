# Decisiones Arquitectónicas Frontend: Gestión de Estado y Sistema de Iconografía

### JaldiShop — Merchant Frontend (`frontend-merchant`)

[![Estado](https://img.shields.io/badge/Estado-Aprobado-green?style=for-the-badge)](./decisiones-frontend-signals-iconos.md)
[![Versión](https://img.shields.io/badge/Versión-v1.0-blue?style=for-the-badge)](./decisiones-frontend-signals-iconos.md)
[![Fase](https://img.shields.io/badge/Fase-Sprint_03-orange?style=for-the-badge)](../06-scrum/sprint-03.md)

---

`📍 Docs` > `05-Arquitectura` > **Decisiones Frontend: Signals, Caché y `@ng-icons`**  
[⬅ Arquitectura del Sistema](./arquitectura-sistema.md) | [🏠 Índice General](../../README.md) | [Contrato API Errores ➡](../04-diseno/contrato-api-errores.md)

---

## 1. ADR-FE-01: Gestión de Estado y Caché en Memoria (*Cache-First con Signals*)

### 1.1 Contexto y Problema
En versiones iniciales, cada navegación dentro del panel (`/dashboard` $\leftrightarrow$ `/capacity` $\leftrightarrow$ `/store` $\leftrightarrow$ `/profile`) disparaba peticiones HTTP repetidas a endpoints semi-estáticos como `GET /api/v1/users/me`, `GET /api/v1/stores/me` y `GET /api/v1/capacity-configurations`.

Esto generaba:
- Latencia innecesaria de 200–500ms al navegar entre páginas.
- Parpadeo (*flicker*) visual por los *skeletons* de carga.
- Mayor consumo de cuota y ancho de banda contra la base de datos PostgreSQL.

### 1.2 Decisión Arquitectónica
Se adopta el patrón **Cache-First** apoyado en **Angular Signals** y **Servicios Singleton (`@Injectable({ providedIn: 'root' })`)**:

1. **Lectura con Parámetro `forceRefresh = false`:**
   Si el servicio ya dispone de los datos en memoria (`isLoaded() === true` o `currentProfile() !== null`) y no se solicita explícitamente refrescar, devuelve inmediatamente `of(datos)` con latencia **0 ms**.
2. **Mutaciones y Sincronización Reactiva:**
   Toda operación de creación (`POST`), edición (`PUT`) o cambio de estado (`PATCH`) actualiza directamente el `signal` en memoria en el operador `tap`, manteniendo el estado 100% fresco sin necesidad de refetch.
3. **Limpieza Atómica en Cierre de Sesión:**
   En [`AuthService.logout()`](file:///C:/Users/IJosueeh/Documents/Proyectos/jaldishop/frontend-merchant/src/app/core/services/auth-service.ts), se orquesta la invocación a `clearCache()`, `clearStore()` y `clearProfile()`, garantizando aislamiento absoluto entre sesiones.

### 1.3 Matriz de Estrategias por Módulo

| Módulo | Tipo de Dato | Estrategia | Razón |
| :--- | :--- | :--- | :--- |
| **Capacidad (`CapacityService`)** | Semi-estático | **Cache-First** | Franjas y cupos cambian únicamente por acción del comerciante. |
| **Mi Tienda (`StoreService`)** | Configuración | **Cache-First** | Datos de tienda cambian solo al guardar el formulario. |
| **Mi Perfil (`ProfileService`)** | Datos de Usuario | **Cache-First** | Perfil estático durante la sesión activa. |
| **Catálogo / Productos (`ProductsService`)** | Inventario | **Cache-First + Mutaciones Locales** | Catálogo cargado en memoria con actualizaciones optimistas. |
| **Pedidos (`OrdersService`)** | Tiempo Real | **Stale-While-Revalidate / Polling** | Alta concurrencia de pedidos de clientes en vivo. |

---

## 2. ADR-FE-02: Sistema de Iconografía Nativa SVG con `@ng-icons`

### 2.1 Contexto y Problema
El uso de fuentes de iconos basadas en ligaduras web (`<span class="material-symbols-outlined">storefront</span>`) presentaba dos deficiencias de experiencia de usuario:
1. **Selección accidental de texto:** El usuario podía seleccionar o arrastrar el texto de la ligadura (*"storefront"*) con el cursor.
2. **FOUT (*Flash of Unstyled Text*):** Antes de que Google Fonts descargue la tipografía en conexiones lentas, el navegador mostraba el texto crudo en la interfaz.
3. **Dependencia de red externa:** Requería conexión obligatoria a Google Fonts en tiempo de ejecución.

### 2.2 Decisión Arquitectónica
Se adopta la librería **`@ng-icons/core`** junto con la colección **`@ng-icons/material-symbols`**:

```typescript
import { NgIcon, provideIcons } from '@ng-icons/core';
import { matStorefrontOutline, matScheduleOutline } from '@ng-icons/material-symbols/outline';

@Component({
  imports: [NgIcon],
  providers: [provideIcons({ matStorefrontOutline, matScheduleOutline })],
  template: `<ng-icon name="matStorefrontOutline" class="text-xl text-primary" />`
})
```

### 2.3 Beneficios Obtenidos
- **SVGs Puros e Inmutables:** Renderizado directo en el DOM; imposible de seleccionar como texto.
- **Tree-Shaking Real:** Solo se empaquetan en el bundle final de producción los iconos explícitamente importados.
- **Cero Dependencia de Red:** Los glifos SVG viajan dentro de los chunks de JavaScript compilados.
- **Compatibilidad con Tailwind CSS:** Acepta clases de tamaño (`text-[18px]`, `w-5 h-5`), color (`text-primary`, `text-emerald-600`) y animaciones (`animate-spin`).

---

## 3. Plan de Migración de Iconos por Fases

- [x] **Fase 0: Capacidad (`/capacity`)** — *Completado*
- [ ] **Fase 1: Layout Global y Componentes Compartidos**
  - `MerchantLayout`: `siderbar`, `header`
  - `Shared`: `toast-container`, `alert-error`, `not-found`, `unauthorized`
- [ ] **Fase 2: Mi Tienda y Mi Perfil**
  - `Store`: `store-identity-card`, `store-location-card`, `store-delivery-card`, `store-preview-card`, `store.html`
  - `Profile`: `profile-info-card`, `profile-security-card`, `profile.html`
- [ ] **Fase 3: Dashboards y Autenticación**
  - `Dashboards`: `dashboard-header`, `dashboard-kpis`, `quick-actions`, `priority-orders`, `logistics-alerts`
  - `Auth`: `login`, `register`, `forgot-password`
