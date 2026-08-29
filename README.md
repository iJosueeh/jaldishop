# 🛍️ JaldiShop

### Plataforma de Gestión de Pedidos y Control Inteligente de Capacidad para MYPE

[![GitHub repo size](https://img.shields.io/github/repo-size/iJosueeh/jaldishop?style=flat-square&color=blue)](https://github.com/iJosueeh/jaldishop)
[![GitHub last commit](https://img.shields.io/github/last-commit/iJosueeh/jaldishop?style=flat-square&color=green)](https://github.com/iJosueeh/jaldishop)
[![GitHub issues](https://img.shields.io/github/issues/iJosueeh/jaldishop?style=flat-square&color=yellow)](https://github.com/iJosueeh/jaldishop/issues)
[![Sprint](https://img.shields.io/badge/Sprint-01%20Completado-blueviolet?style=flat-square)](./docs/06-scrum/sprint-01.md)
[![Docs](https://img.shields.io/badge/Docs-Estructuradas-success?style=flat-square&logo=markdown)](./docs)

> **Capa de orden operativa** para micro y pequeñas empresas que comercializan por WhatsApp e Instagram, eliminando la sobreventa y sincronizando pedidos con su capacidad real.

---

## 📌 Visión General

Las MYPE que trabajan bajo pedido (panaderías, dark kitchens, reposterías, catering) enfrentan a diario **sobreventa, pedidos traspapelados y saturación operativa**. 

A diferencia del e-commerce tradicional —que únicamente evalúa si hay *stock* de producto—, **JaldiShop** incorpora la **capacidad operativa** (tiempo de preparación, disponibilidad de personal y slots de entrega) como restricción inteligente para asegurar que el negocio solo acepte lo que verdaderamente puede cumplir.

```mermaid
flowchart LR
    subgraph CLIENTES["📱 Canales de Venta"]
        W[WhatsApp]
        I[Instagram]
        O[Otros Medios]
    end

    subgraph PLATAFORMA["⚡ JALDISHOP (Capa de Orden)"]
        direction TB
        C1["Catálogo & Carrito Digital"]
        C2["Motor de Validación de Capacidad"]
        C3["Checkout con Reserva Temporal"]
        C4["Panel de Control MYPE"]
    end

    subgraph BENEFICIOS["🎯 Impacto Operativo"]
        B1["🚫 Cero Sobreventa"]
        B2["⏱️ Ahorro de Tiempo"]
        B3["📦 Seguimiento Transparente"]
    end

    CLIENTES --> PLATAFORMA --> BENEFICIOS
```

---

## 👥 Equipo de Desarrollo

| Miembro | Rol | Responsabilidad Sprint 1 | Perfil |
|---|:---:|---|:---:|
| **Josue Royer Tanta Cieza** | Full Stack Dev | Caso A (Pastelería), Caso C (Logística) & Modelo de Capacidad | [![GitHub](https://img.shields.io/badge/-@iJosueeh-181717?style=flat-square&logo=github)](https://github.com/iJosueeh) |
| **Katherine Patricia Salas Quiroz** | Full Stack Dev | Caso B (Dark Kitchen) & Consolidación | [![GitHub](https://img.shields.io/badge/-@kath144-181717?style=flat-square&logo=github)](https://github.com/kath144) |
| **Mia Vitalia Gual Vega** | Full Stack Dev | Flujo AS-IS / TO-BE & Modelado de Procesos | [![GitHub](https://img.shields.io/badge/-@miagv-181717?style=flat-square&logo=github)](https://github.com/miagv) |

---

## 🛠️ Stack Tecnológico

| Capa | Tecnologías | Propósito |
|:---|:---|:---|
| **Backend** | ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) ![Java](https://img.shields.io/badge/Java_17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white) | API RESTful, Reglas de Dominio y Transaccionalidad |
| **Frontend** | ![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white) ![Angular](https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white) ![Tailwind](https://img.shields.io/badge/Tailwind_CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white) | Portal Cliente y Panel Administrativo MYPE |
| **Persistencia** | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) ![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white) | Base de datos relacional y control concurrente |
| **Tiempo Real** | ![WebSocket](https://img.shields.io/badge/WebSocket-764ABC?style=for-the-badge&logo=socket.io&logoColor=white) | Actualización de estados y disponibilidad en vivo |
| **Documentación** | ![Mermaid](https://img.shields.io/badge/Mermaid-FF3670?style=for-the-badge&logo=mermaid&logoColor=white) ![Markdown](https://img.shields.io/badge/Markdown-000000?style=for-the-badge&logo=markdown&logoColor=white) | Diagramas interactivos y especificaciones vivas |

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
│   │   └── 📄 alcance-mvp.md           # Must / Should / Could / Won't have
│   ├── 📁 04-design/                   # Modelado de procesos y UX
│   │   └── 📁 proceso/
│   │       └── 📄 as-is-to-be.md       # Diagramas comparativos AS-IS vs TO-BE
│   ├── 📁 06-scrum/                    # Gestión ágil de sprints
│   │   └── 📄 sprint-01.md             # Sprint 1: Modelo de capacidad (Completado)
│   └── 📄 TEMPLATE.md                  # Plantilla estándar para nuevos documentos
├── 📁 backend/                         # Servidor Spring Boot (Próximo Sprint)
├── 📁 frontend/                        # Aplicación Web (Próximo Sprint)
└── 📄 README.md                        # Portal principal del repositorio
```

---

## 🚀 Estado de los Sprints

```mermaid
flowchart TD
    S1["✅ Sprint 1: Modelo de Capacidad y Casos de Estudio (Completado)"]
    S2["🔄 Sprint 2: Backend Spring Boot, API REST y Persistencia (En Progreso)"]
    S3["⏳ Sprint 3: Frontend Web y Panel Administrativo (Pendiente)"]
    S4["⏳ Sprint 4: Pasarela de Pagos, Integración y Pruebas (Pendiente)"]

    S1 --> S2 --> S3 --> S4
```

| Sprint | Enfoque | Estado | Entregable Clave |
|:---:|---|:---:|---|
| **01** | Modelo de Capacidad y Casos de Estudio | `COMPLETADO` | [Modelo de Capacidad v1](./docs/01-producto/modelo-capacidad-v1.md) |
| **02** | Arquitectura Backend y Persistencia | `EN PROGRESO` | Diagrama ER, API REST Spring Boot |
| **03** | Frontend Web & Panel de Control | `PENDIENTE` | Interfaz Next.js / Angular |
| **04** | Integración, Pasarela de Pagos & QA | `PENDIENTE` | MVP Funcional Desplegado |

---

## 💡 Conceptos Clave del Dominio

* **Capacidad Base:** Límite estándar de pedidos que una tienda procesa por día o bloque horario.
* **Excepción Temporal:** Ajuste en caliente para fechas festivas o imprevistos operativos sin alterar la base semanal.
* **Reserva Temporal (Hold):** Bloqueo transaccional de cupo durante 10 minutos mientras el cliente realiza el pago.
* **Capacidad Comprometida:** Total de cupos bloqueados por pedidos pagados y reservas en curso.

---

**JaldiShop** — Construido con rigor de ingeniería de software para impulsar a las MYPE.
