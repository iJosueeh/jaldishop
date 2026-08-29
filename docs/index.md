<div align="center">

# 🛍️ JaldiShop
### Plataforma de Gestión de Pedidos y Control Inteligente de Capacidad para MYPE

[![Sprint](https://img.shields.io/badge/Sprint-01%20Completado-blueviolet?style=flat-square)](./06-scrum/sprint-01.md)
[![Docs](https://img.shields.io/badge/Docs-Estructuradas-success?style=flat-square&logo=markdown)](./index.md)

<br/>

> **Capa de orden operativa** para micro y pequeñas empresas que comercializan por WhatsApp e Instagram, eliminando la sobreventa y sincronizando pedidos con su capacidad real.

</div>

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
| **Josue Royer Tanta Cieza** | Full Stack Dev | Caso A (Pastelería), Caso C (Logística) & Modelo de Capacidad | [@iJosueeh](https://github.com/iJosueeh) |
| **Katherine Patricia Salas Quiroz** | Full Stack Dev | Caso B (Dark Kitchen) & Consolidación | [@kath144](https://github.com/kath144) |
| **Mia Vitalia Gual Vega** | Full Stack Dev | Flujo AS-IS / TO-BE & Modelado de Procesos | [@miagv](https://github.com/miagv) |

---

## 🛠️ Stack Tecnológico

| Capa | Tecnologías | Propósito |
|:---|:---|:---|
| **Backend** | Spring Boot 3, Java 17+ | API RESTful, Reglas de Dominio y Transaccionalidad |
| **Frontend** | Next.js, Angular, Tailwind CSS | Portal Cliente y Panel Administrativo MYPE |
| **Persistencia** | PostgreSQL, Hibernate | Base de datos relacional y control concurrente |
| **Tiempo Real** | WebSockets | Actualización de estados y disponibilidad en vivo |

---

## 🚀 Estado de los Sprints

```mermaid
gantt
    title Plan de Desarrollo JaldiShop
    dateFormat  YYYY-MM-DD
    section Fase 1: Concepción
    Sprint 1 - Modelo de Capacidad y Casos de Uso :done, s1, 2026-08-01, 2026-08-15
    section Fase 2: Backend
    Sprint 2 - Modelo de Datos, API REST y Concurrencia :active, s2, 2026-08-16, 2026-09-05
    section Fase 3: Frontend
    Sprint 3 - Portal Cliente y Dashboard MYPE : s3, 2026-09-06, 2026-09-25
    section Fase 4: Integración
    Sprint 4 - Pagos, WebSockets y Pruebas End-to-End : s4, 2026-09-26, 2026-10-15
```

---

## 💡 Conceptos Clave del Dominio

* **Capacidad Base:** Límite estándar de pedidos que una tienda procesa por día o bloque horario.
* **Excepción Temporal:** Ajuste en caliente para fechas festivas o imprevistos operativos sin alterar la base semanal.
* **Reserva Temporal (Hold):** Bloqueo transaccional de cupo durante 10 minutos mientras el cliente realiza el pago.
* **Capacidad Comprometida:** Total de cupos bloqueados por pedidos pagados y reservas en curso.
