# Sprint 2: Backend Base

### JaldiShop — Gestión Ágil, Backlog y Entregables del Sprint 02

[![Estado](https://img.shields.io/badge/Estado-En_Progreso-yellow?style=for-the-badge&logo=clockify&logoColor=white)](./sprint-02.md)
[![Fase](https://img.shields.io/badge/Fase-Sprint_02-orange?style=for-the-badge)](./sprint-02.md)
[![Duración](https://img.shields.io/badge/Duración-1_Semana-blue?style=for-the-badge)](./sprint-02.md)

---

`📍 Docs` > `06-Scrum` > **Sprint 02**  
[⬅ Sprint 01](./sprint-01.md) | [🏠 Índice General](../../README.md) | [Alcance del MVP ➡](../03-requisitos/alcance-mvp.md)

---

## 1. Objetivo del Sprint

> 📌 **Nota:** Iniciar la implementación del backend de JaldiShop con el módulo Identity (autenticación y usuarios).

* **Fase:** Semana 3 — *Backend Base*.
* **Propósito:** Implementar el dominio, puertos, persistencia y autenticación JWT.
* **Meta Central:** Tener el módulo Identity completo y funcionando con JWT.

---

## 2. Backlog del Sprint

```mermaid
flowchart LR
    subgraph ALTA["Prioridad Alta"]
        T1["BE-01 · Dominio Identity"]
        T2["BE-02 · Migración Roles"]
        T3["BE-03 · Casos de Uso Auth"]
    end

    subgraph MEDIA["Prioridad Media"]
        T4["BE-04 · Code Review Identity"]
        T5["BE-05 · Persistencia JPA"]
    end

    subgraph BAJA["Prioridad Baja"]
        T6["BE-06 · Integrar JWT"]
    end

    ALTA --> MEDIA --> BAJA
```

| Prioridad | Tarjeta | Responsable | Estado | Entregable |
|:---:|---|:---:|:---:|---|
| 🔴 **Alta** | BE-01 · Implementar dominio Identity | Josué | `EN PROGRESO` | User, Role, enums y ports |
| 🔴 **Alta** | BE-02 · Preparar datos iniciales roles | Katherine | `PENDIENTE` | Diseño + migración V2__seed_roles.sql |
| 🔴 **Alta** | BE-03 · Diseñar casos de uso Auth | Mia | `PENDIENTE` | Flujos Registro/Login + DTOs/errores |
| 🟠 **Media** | BE-04 · Code Review Identity | Equipo | `PENDIENTE` | Revisión cruzada antes de JPA |
| 🟠 **Media** | BE-05 · Persistencia JPA Identity | Josué | `PENDIENTE` | Entities + repositories + adapter |
| 🟢 **Baja** | BE-06 · Integrar JWT con UUID | Josué | `PENDIENTE` | Security/JWT funcionando |

---

## 3. Tarjetas de Trabajo

### 📋 BE-01 | Implementar dominio y puertos de Identity

**Responsable:** Josué  
**Entregable:** User, Role, enums y ports

**Objetivo:** Implementar el modelo de dominio inicial del módulo Identity siguiendo el modelo físico V1 ya validado. El dominio debe permanecer independiente de Spring, JPA, JWT y PostgreSQL.

**Checklist:**
- [ ] Crear UserStatus (enum)
- [ ] Crear RoleName (enum)
- [ ] Crear Role (entidad)
- [ ] Crear User (entidad)
- [ ] Implementar User.create(...)
- [ ] Generar UUID desde aplicación/dominio
- [ ] Normalizar email
- [ ] Implementar canAuthenticate()
- [ ] Implementar getFullName()
- [ ] Crear UserRepository (puerto)
- [ ] Crear RoleRepository (puerto)
- [ ] Crear UserTest
- [ ] Crear RoleTest
- [ ] Ejecutar tests
- [ ] Abrir Pull Request

**Decisiones cerradas que aplican:**
- UUID generado por JPA, no PostgreSQL
- Email normalizado lowercase por aplicación
- roles.id es SMALLINT
- UserStatus: ACTIVE, INACTIVE, SUSPENDED
- RoleName: CUSTOMER, MERCHANT, ADMIN
- password_encoded VARCHAR(255) NOT NULL

---

### 📋 BE-02 | Preparar migración de roles iniciales

**Responsable:** Katherine  
**Entregable:** Diseño + migración V2__seed_roles.sql

**Objetivo:** La BD tiene la estructura roles, pero necesitamos garantizar que existan los roles iniciales: CUSTOMER, MERCHANT, ADMIN.

**Checklist:**
- [ ] Revisar tabla roles en modelo-er.md
- [ ] Confirmar IDs SMALLINT
- [ ] Definir IDs estables para CUSTOMER/MERCHANT/ADMIN
- [ ] Crear V2__seed_roles.sql
- [ ] No modificar V1
- [ ] Ejecutar Flyway
- [ ] Verificar flyway_schema_history
- [ ] Consultar roles insertados
- [ ] Documentar resultado
- [ ] Abrir Pull Request

**Decisión de IDs estables:**
```sql
-- IDs estables para catálogo de roles
-- 1 → CUSTOMER
-- 2 → MERCHANT  
-- 3 → ADMIN
INSERT INTO roles (id, name)
VALUES
    (1, 'CUSTOMER'),
    (2, 'MERCHANT'),
    (3, 'ADMIN');
```

---

### 📋 BE-03 | Diseñar casos de uso Registro y Login

**Responsable:** Mia  
**Entregable:** Flujos Registro/Login + DTOs/errores documentados

**Objetivo:** Definir exactamente qué necesitarán Registration y Login. No es hacer pantallas, es bajar el flujo funcional a contrato de backend.

**Checklist:**
- [ ] Documentar flujo Register Customer
- [ ] Documentar flujo Register Merchant
- [ ] Documentar flujo Login
- [ ] Definir campos de RegisterRequest
- [ ] Definir campos de LoginRequest
- [ ] Definir AuthResponse
- [ ] Definir errores esperados
- [ ] Definir asignación CUSTOMER/MERCHANT
- [ ] Confirmar que ADMIN no tiene registro público
- [ ] Documentar validación de UserStatus
- [ ] Documentar comportamiento de email duplicado
- [ ] Pasar a Code Review

**Flujo Register Customer (ejemplo):**
```
Register Customer
       ↓
validar request
       ↓
normalizar email
       ↓
¿email existente?
   ↙        ↘
 sí         no
409       encode password
              ↓
       obtener CUSTOMER
              ↓
          crear User
              ↓
            save
```

**Flujo Login (ejemplo):**
```
email + password
       ↓
buscar User
       ↓
PasswordEncoder.matches()
       ↓
¿ACTIVE?
       ↓
obtener roles
       ↓
JWT
       ↓
AuthResponse
```

---

### 📋 BE-04 | Code Review Identity

**Responsable:** Todo el equipo  
**Entregable:** Revisión cruzada antes de JPA

**Objetivo:** Revisar el código del dominio y puertos antes de proceder a la persistencia JPA.

**Checklist:**
- [ ] Revisar entidades de dominio
- [ ] Verificar independencia de Spring/JPA
- [ ] Validar tests unitarios
- [ ] Confirmar puertos correctos
- [ ] Aprobar para JPA

---

### 📋 BE-05 | Persistencia JPA Identity

**Responsable:** Josué  
**Entregable:** Entities + repositories + adapter

**Objetivo:** Implementar la capa de persistencia con JPA siguiendo el modelo físico V1.

**Checklist:**
- [ ] Crear JPA Entities (User, Role)
- [ ] Crear Repositorios JPA
- [ ] Crear Adapter de puertos
- [ ] Configurar schema en application.properties
- [ ] Ejecutar tests de integración
- [ ] Abrir Pull Request

---

### 📋 BE-06 | Integrar JWT con UUID

**Responsable:** Josué  
**Entregable:** Security/JJWT funcionando

**Objetivo:** Integrar JWT con la autenticación usando UUID como identificador.

**Checklist:**
- [ ] Configurar Spring Security
- [ ] Implementar JwtTokenProvider
- [ ] Implementar JwtAuthenticationFilter
- [ ] Configurar endpoints públicos/privados
- [ ] Probar login completo
- [ ] Probar access/refresh tokens
- [ ] Abrir Pull Request

---

## 4. Estado de Avance del Sprint

| Métrica | Estado Actual |
|---|:---:|
| Entregables completados | 0 / 6 (0%) |
| Entregables en desarrollo activo | 1 / 6 (17%) |
| Entregables pendientes | 5 / 6 (83%) |
| **Estado General** | `EN PROGRESO INICIAL` |

---

## 5. Decisiones Cerradas del Modelo que Aplian a Este Sprint

| Decisión | Valor | Documento |
|---|---|---|
| UUID generado por JPA | `GenerationType.UUID` | modelo-er.md |
| Email normalizado | lowercase, trim antes de persistir | modelo-er.md |
| roles.id | SMALLINT | modelo-er.md |
| UserStatus | ACTIVE, INACTIVE, SUSPENDED | modelo-er.md |
| RoleName | CUSTOMER, MERCHANT, ADMIN | modelo-er.md |
| password_encoded | VARCHAR(255) NOT NULL | modelo-er.md |
| Flyway | Utilizado desde migración inicial | modelo-er.md |
| Rollback | Forward-only + backup/restore | modelo-er.md |

---

## 6. Dependencias entre Tarjetas

```mermaid
graph TD
    BE01[BE-01 · Dominio Identity] --> BE04[BE-04 · Code Review]
    BE02[BE-02 · Migración Roles] --> BE04
    BE03[BE-03 · Casos de Uso Auth] --> BE04
    BE04 --> BE05[BE-05 · Persistencia JPA]
    BE05 --> BE06[BE-06 · Integrar JWT]
```

**Notas:**
- BE-01, BE-02 y BE-03 pueden ejecutarse en paralelo
- BE-04 requiere que las tres tarjetas anteriores estén completas
- BE-05 depende de BE-04
- BE-06 depende de BE-05

---

[⬅ Volver a Sprint 01](./sprint-01.md) | [🏠 Volver al Índice General](../../README.md) | [Modelo ER ➡](../04-diseno/modelo-er.md)
