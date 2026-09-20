# 🛍️ JaldiShop

### Plataforma de Gestión de Pedidos y Control Inteligente de Capacidad para MYPE

[![Sprint](https://img.shields.io/badge/Sprint-03%20En%20Progreso-yellow?style=flat-square)](./06-scrum/sprint-03.md)
[![Docs](https://img.shields.io/badge/Docs-Estructuradas-success?style=flat-square&logo=markdown)](./index.md)
[![Frontend Merchant](https://img.shields.io/badge/Deploy-Cloudflare%20Pages-F38020?style=flat-square&logo=cloudflare)](https://negocios-jaldishop.pages.dev/)
[![Backend API](https://img.shields.io/badge/Deploy-Render%20Web%20Service-46E3B7?style=flat-square&logo=render)](https://jaldishop-api.onrender.com/api/v1)

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
| **Josue Royer Tanta Cieza** | Full Stack Dev | CI-01 Pipeline CI/CD + BE-17 Carrito + Despliegue Render/Cloudflare + FE Branding | [@iJosueeh](https://github.com/iJosueeh) |
| **Katherine Patricia Salas Quiroz** | Full Stack Dev | BE-12 Módulo de Catálogo + BE-13 Inventario | [@kath144](https://github.com/kath144) |
| **Mia Vitalia Gual Vega** | Full Stack Dev | BE-14 Capacidad Base + BE-15 Excepciones + BE-16 Capacidad Efectiva | [@miagv](https://github.com/miagv) |

---

## 🛠️ Stack Tecnológico

| Capa | Tecnologías | Propósito |
|:---|:---|:---|
| **Backend** | Spring Boot 4.1.1, Java 21 | API RESTful, Arquitectura Hexagonal/DDD y Transaccionalidad |
| **Seguridad** | JWT, Spring Security | Autenticación sin estado y RBAC (CUSTOMER, MERCHANT, ADMIN) |
| **Frontend** | Angular 20 Standalone, Next.js, Tailwind CSS | Panel de Comerciante (Angular) y Portal Cliente (Next.js) |
| **Persistencia** | PostgreSQL 18.6 (NeonDB), Flyway, Hibernate | Base de datos relacional en la nube y JPA |
| **Despliegue & DevOps** | Render, Cloudflare Pages, Docker, GitHub Actions | Despliegue continuo y validación automatizada en la nube |
| **Calidad / QA** | JUnit 5, Mockito, Vitest | 260 pruebas automatizadas pasando al 100% (153 Backend + 107 Frontend) |
| **Tiempo Real** | WebSockets | Actualización de estados y disponibilidad en vivo |

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

---

## 💡 Conceptos Clave del Dominio

* **Capacidad Base:** Límite estándar de pedidos que una tienda procesa por día o bloque horario.
* **Excepción Temporal:** Ajuste en caliente para fechas festivas o imprevistos operativos sin alterar la base semanal.
* **Reserva Temporal (Hold):** Bloqueo transaccional de cupo durante 10 minutos mientras el cliente realiza el pago.
* **Capacidad Comprometida:** Total de cupos bloqueados por pedidos pagados y reservas en curso.
