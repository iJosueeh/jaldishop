# 🛍️ JaldiShop

### Plataforma de Gestión de Pedidos y Control Inteligente de Capacidad para MYPE

[![GitHub repo size](https://img.shields.io/github/repo-size/iJosueeh/jaldishop?style=flat-square&color=blue)](https://github.com/iJosueeh/jaldishop)
[![GitHub last commit](https://img.shields.io/github/last-commit/iJosueeh/jaldishop?style=flat-square&color=green)](https://github.com/iJosueeh/jaldishop)
[![CI](https://github.com/iJosueeh/jaldishop/actions/workflows/ci.yml/badge.svg)](https://github.com/iJosueeh/jaldishop/actions/workflows/ci.yml)
[![Sprint](https://img.shields.io/badge/Sprint-03%20En%20Progreso-yellow?style=flat-square)](./docs/06-scrum/sprint-03.md)
[![Tests](https://img.shields.io/badge/Tests-274%20Passing%20(153%20BE%20+%20121%20FE)-success?style=flat-square&logo=vitest)](./frontend-merchant)
[![Frontend Merchant](https://img.shields.io/badge/Deploy-Cloudflare%20Pages-F38020?style=flat-square&logo=cloudflare)](https://negocios-jaldishop.pages.dev/)
[![Backend API](https://img.shields.io/badge/Deploy-Render%20Web%20Service-46E3B7?style=flat-square&logo=render)](https://jaldishop-api.onrender.com/api/v1)
[![Docs](https://img.shields.io/badge/Docs-Estructuradas-success?style=flat-square&logo=markdown)](./docs)

> **Capa de orden operativa** para micro y pequeñas empresas que comercializan por WhatsApp e Instagram, eliminando la sobreventa y sincronizando pedidos con su capacidad real.

---

## 📌 Visión General

Las MYPE que trabajan bajo pedido (panaderías, dark kitchens, reposterías, catering) enfrentan a diario **sobreventa, pedidos traspapelados y saturación operativa**. 

A diferencia del e-commerce tradicional —que únicamente evalúa si hay *stock* de producto—, **JaldiShop** incorpora la **capacidad operativa** (tiempo de preparación, disponibilidad de personal y slots de entrega) como restricción inteligente para asegurar que el negocio solo acepte lo que verdaderamente puede cumplir.

```mermaid
flowchart LR
    subgraph CLIENTES["Canales de Venta"]
        W[WhatsApp]
        I[Instagram]
        O[Otros Medios]
    end

    subgraph PLATAFORMA["JaldiShop - Capa de Orden"]
        direction TB
        C1["Catálogo y Carrito Digital"]
        C2["Motor de Validación de Capacidad"]
        C3["Checkout con Reserva Temporal"]
        C4["Panel de Control MYPE"]
    end

    subgraph BENEFICIOS["Impacto Operativo"]
        B1["Cero Sobreventa"]
        B2["Ahorro de Tiempo"]
        B3["Seguimiento Transparente"]
    end

    CLIENTES --> PLATAFORMA --> BENEFICIOS
```

---

## 👥 Equipo de Desarrollo

| Miembro | Rol | Responsabilidad Sprint 3 | Perfil |
|---|:---:|---|:---:|
| **Josue Royer Tanta Cieza** | Full Stack Dev | **CI-01** · Pipeline CI/CD (✅ Hecho), **BE-17** · Carrito (🔒 Bloqueada), **Despliegue** (Render & Cloudflare Pages) y **FE Branding** | [![GitHub](https://img.shields.io/badge/-@iJosueeh-181717?style=flat-square&logo=github)](https://github.com/iJosueeh) |
| **Katherine Patricia Salas Quiroz** | Full Stack Dev | **BE-12** · Módulo de Catálogo (🟢 En Progreso) & **BE-13** · Módulo de Inventario (🔒 Bloqueada por BE-12) | [![GitHub](https://img.shields.io/badge/-@kath144-181717?style=flat-square&logo=github)](https://github.com/kath144) |
| **Mia Vitalia Gual Vega** | Full Stack Dev | **BE-14** · Capacidad Base (✅ Hecho), **BE-15** · Excepciones (✅ Hecho) & **BE-16** · Capacidad Efectiva (🟢 En Progreso) | [![GitHub](https://img.shields.io/badge/-@miagv-181717?style=flat-square&logo=github)](https://github.com/miagv) |

---

## 🛠️ Stack Tecnológico

| Capa | Tecnologías | Propósito |
|:---|:---|:---|
| **Backend** | ![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) ![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white) | API RESTful, Arquitectura Hexagonal/DDD, Bean Validation y Transaccionalidad |
| **Seguridad** | ![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white) | Autenticación sin estado, RBAC (CUSTOMER, MERCHANT, ADMIN) y control de acceso |
| **Persistencia** | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL_18.6-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) ![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white) | Base de datos NeonDB en la nube, versionamiento DDL forward-only y JPA |
| **Frontend** | ![Angular](https://img.shields.io/badge/Angular_20-DD0031?style=for-the-badge&logo=angular&logoColor=white) ![Tailwind](https://img.shields.io/badge/Tailwind_CSS_v4-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white) ![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white) | Panel de Comerciante (Angular Standalone/Signals) y Portal Cliente (Next.js) |
| **Despliegue & DevOps** | ![Cloudflare](https://img.shields.io/badge/Cloudflare_Pages-F38020?style=for-the-badge&logo=cloudflare&logoColor=white) ![Render](https://img.shields.io/badge/Render-46E3B7?style=for-the-badge&logo=render&logoColor=black) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white) | Despliegue en la nube (Frontend en Cloudflare Pages, Backend en Render) |
| **Calidad / QA & CI** | ![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?style=for-the-badge&logo=junit5&logoColor=white) ![Vitest](https://img.shields.io/badge/Vitest-FCC72B?style=for-the-badge&logo=vitest&logoColor=black) ![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white) | 274 pruebas automatizadas pasando al 100% (153 BE + 121 FE) y pipeline continuo |
| **Documentación** | ![Mermaid](https://img.shields.io/badge/Mermaid-FF3670?style=for-the-badge&logo=mermaid&logoColor=white) ![Markdown](https://img.shields.io/badge/Markdown-000000?style=for-the-badge&logo=markdown&logoColor=white) | Diagramas interactivos y especificaciones de arquitectura viva |

---

## 🗺️ Mapa de Documentación del Proyecto

Toda la documentación técnica y de producto se encuentra estructurada y versionada en [`/docs`](./docs):

```
jaldishop/
├── 📁 docs/
│   ├── 📁 01-producto/                 # Visión de negocio y modelo operativo
│   │   ├── 📄 propuesta.md             # Propuesta y justificación del producto
│   │   ├── 📄 modelo-capacidad-v1.md   # Core: Algoritmo y reglas de capacidad
│   │   └── 📄 decisiones-producto.md   # Decisiones técnicas y de negocio (DP-G01 a G06)
│   ├── 📁 02-investigacion/            # Levantamiento de requisitos de campo
│   │   ├── 📄 caso-a-pasteleria.md     # Caso A: Capacidad en repostería bajo pedido
│   │   ├── 📄 caso-b-dark-kitchen.md   # Caso B: Cocina oculta y franjas horarias
│   │   ├── 📄 caso-c-logistica.md      # Caso C: Capacidad de delivery y recojo
│   │   └── 📄 matriz-consolidacion.md  # Matriz comparativa de hallazgos
│   ├── 📁 03-requisitos/               # Alcance del MVP y especificaciones
│   │   ├── 📄 alcance-mvp.md           # Must / Should / Could / Won't have (v1.1)
│   │   ├── 📄 reglas-negocio.md        # Reglas e invariantes del dominio (RN-01 a 15) (v1.1)
│   │   ├── 📄 historia.md              # Historias de usuario del MVP (v1.1)
│   │   └── 📄 modelo-dominio.md        # Modelo de dominio y conceptos (v1.0)
│   ├── 📁 04-diseno/                   # Modelado de persistencia y API
│   │   ├── 📄 modelo-er.md             # Modelo Entidad-Relación v1.6 (Listo para DDL)
│   │   ├── 📄 diagrama-er.md           # Diagrama ER visual
│   │   ├── 📄 contrato-api-errores.md  # Contrato estandarizado de respuestas y errores HTTP
│   │   └── 📄 as-is-to-be.md           # Diagramas comparativos AS-IS vs TO-BE
│   ├── 📁 05-arquitectura/             # Decisiones arquitectónicas
│   │   └── 📄 arquitectura-sistema.md  # Arquitectura del sistema (v0.2)
│   ├── 📁 06-scrum/                    # Gestión ágil de sprints
│   │   ├── 📄 sprint-01.md             # Sprint 1: Modelo de Capacidad (Completado)
│   │   ├── 📄 sprint-02.md             # Sprint 2: Backend Base y Frontend Merchant Core (Completado)
│   │   └── 📄 sprint-03.md             # Sprint 3: Catálogo, Capacidad y CI/CD (En Progreso)
│   └── 📄 TEMPLATE.md                  # Plantilla estándar para nuevos documentos
├── 📁 backend/                         # Servidor Spring Boot (Identity + Store + Notification + Catalog + Capacity)
├── 📁 frontend-merchant/               # Panel de Comerciante (Angular 20 Standalone con Signals)
├── 📁 frontend/                        # Portal Cliente (Próximo Sprint)
└── 📄 README.md                        # Portal principal del repositorio
```

---

## 🚀 Estado de los Sprints

```mermaid
flowchart TD
    S1["Sprint 1: Modelo de Capacidad y Casos de Estudio - Completado ✅"]
    S2["Sprint 2: Backend Base & Frontend Merchant Core - Completado ✅"]
    S3["Sprint 3: Catálogo, Capacidad y CI/CD - En Progreso 🚀"]
    S4["Sprint 4: Inventario, Carrito y Checkout con Reserva - Próximo"]
    S5["Sprint 5: Pedidos, Pagos, Integración y Despliegue MVP - Pendiente"]

    S1 --> S2 --> S3 --> S4 --> S5
```

| Sprint | Enfoque | Estado | Entregable Clave |
|:---:|---|:---:|---|
| **01** | Modelo de Capacidad y Casos de Estudio | `COMPLETADO` | [Modelo de Capacidad v1](./docs/01-producto/modelo-capacidad-v1.md) |
| **02** | Backend Base & Frontend Merchant Core | `COMPLETADO` | [Sprint 02 Backlog](./docs/06-scrum/sprint-02.md) (Identity + Store + Notificaciones + Onboarding + Dashboards + Tienda + Errores + Perfil, 156 tests) |
| **03** | Catálogo, Capacidad y CI/CD | `EN PROGRESO` | [Sprint 03 Backlog](./docs/06-scrum/sprint-03.md) (Catálogo BE-12, Capacidad Base BE-14, Pipeline CI-01) |
| **04** | Inventario, Carrito y Checkout con Reserva | `PRÓXIMO` | Motor de reserva temporal (Hold 10 min) y catálogo público |
| **05** | Pedidos, Pagos, Integración y QA | `PENDIENTE` | Pasarelas de pago, ciclo de pedidos y MVP Desplegado |

---

## 💡 Conceptos Clave del Dominio

* **Capacidad Base:** Límite estándar de pedidos que una tienda procesa por día o bloque horario.
* **Excepción Temporal:** Ajuste en caliente para fechas festivas o imprevistos operativos sin alterar la base semanal.
* **Reserva Temporal (Hold):** Bloqueo transaccional de cupo durante **10 minutos para iniciar el pago**. El pago puede completarse después de este periodo si inició válidamente antes de la expiración.
* **Capacidad Comprometida:** Total de cupos bloqueados por pedidos pagados y reservas en curso.

---

**JaldiShop** — Construido con rigor de ingeniería de software para impulsar a las MYPE.
