# Análisis AS-IS y TO-BE

### JaldiShop — Modelado de Procesos de Negocio

[![Estado](https://img.shields.io/badge/Estado-Completado-success?style=for-the-badge&logo=checkmarx&logoColor=white)](./as-is-to-be.md)
[![Fase](https://img.shields.io/badge/Fase-Diseño_de_Proceso-blue?style=for-the-badge)](./as-is-to-be.md)

---

`📍 Docs` > `04-Diseño` > `Proceso` > **AS-IS vs TO-BE**  
[⬅ Alcance del MVP](../../03-requisitos/alcance-mvp.md) | [🏠 Índice General](../../../README.md) | [Sprint 01 ➡](../../06-scrum/sprint-01.md)

---

<details open>
<summary><b>📑 Tabla de Contenidos</b></summary>

- [1. Objetivo](#1-objetivo)
- [2. Proceso AS-IS (Actual)](#2-proceso-as-is)
- [3. Problemas del Proceso AS-IS](#3-problemas-identificados-en-el-as-is)
- [4. Proceso TO-BE (Propuesto con JaldiShop)](#4-proceso-to-be)
- [5. Comparativa de Transformación](#5-comparación-as-is--to-be)
- [6. Responsabilidades de la Plataforma](#6-qué-gestiona-la-plataforma)

</details>

---

## 1. Objetivo

> [!NOTE]
> Representar cómo una MYPE gestiona actualmente sus pedidos de manera manual y fragmentada (AS-IS), frente a cómo operará de forma sincronizada y automatizada mediante **JaldiShop** (TO-BE).

---

## 2. Proceso AS-IS

Actualmente, los pedidos ingresan por múltiples canales (WhatsApp, Instagram, llamadas). El comerciante debe verificar manualmente inventarios y agendas sin validación automática de capacidad.

### Flujo AS-IS

```mermaid
flowchart TD
    A[Cliente contacta al negocio] --> B{Canal de contacto}
    B -->|WhatsApp / Instagram| C[Cliente envía solicitud de pedido]
    C --> D[Encargado responde y toma datos manualmente]
    D --> E[Cliente indica cantidad y fecha deseada]
    
    E --> F{¿Hay capacidad operativa?}
    F -->|Duda| G[Revisa libreta, Excel o memoria]
    G --> H{¿Puede cumplir?}
    H -->|No| I[Rechaza pedido / Pierde venta]
    H -->|Sí| J[Informa disponibilidad al cliente]
    F -->|Asume que sí| J
    
    J --> K[Cliente envía comprobante de pago]
    K --> L[Encargado verifica manualmente la cuenta]
    L --> M[Registra pedido en libreta o chat]
    M --> N[Elaboración manual]
    N --> O[Despacho / Entrega]
```

---

## 3. Problemas Identificados en el AS-IS

```mermaid
flowchart TD
    A[Recepción dispersa por WhatsApp/Instagram] --> B[Falta de control de capacidad]
    B --> C{Riesgos Críticos}
    
    C --> D[🚨 Sobreventa y comprometer más de lo posible]
    C --> E[⏳ Pedidos olvidados o traspapelados]
    C --> F[📉 Demoras en responder disponibilidad]
    
    D --> G[Colapso Operativo e Incumplimiento al Cliente]
    E --> G
    F --> G
```

---

## 4. Proceso TO-BE

Con **JaldiShop**, el catálogo es digital y el motor de capacidad valida la disponibilidad en tiempo real antes de permitir el checkout.

### Flujo TO-BE

```mermaid
flowchart TD
    A[Cliente ingresa al catálogo de la tienda] --> B[Selecciona productos y cantidades]
    B --> C[Selecciona fecha y franja horaria requerida]
    
    C --> D[Sistema consulta Capacidad Disponible]
    D --> E{¿Existe capacidad disponible?}
    
    E -->|No| F[Plataforma bloquea franja y sugiere alternativas]
    E -->|Sí| G[Crear Reserva Temporal - Hold 10 min]
    
    G --> H[Cliente realiza pago en pasarela integrada]
    H --> I{¿Pago exitoso?}
    
    I -->|No / Timeout| J[Liberar cupo a disponible]
    I -->|Sí| K[Confirmar pedido y pasar a comanda del negocio]
    
    K --> L[Negocio visualiza orden en Dashboard]
    L --> M[Actualización de estados en tiempo real]
    M --> N[Cliente realiza autoseguimiento transparente]
```

---

## 5. Comparación AS-IS → TO-BE

| Dimensión Operativa | Proceso AS-IS (Actual) | Transformación JaldiShop | Proceso TO-BE (Propuesto) |
|---|---|:---:|---|
| **Canal de Pedidos** | Disperso en chats de WhatsApp/Instagram | Centralización | Catálogo web unificado por tienda |
| **Control de Capacidad** | Cálculo mental o en libretas | Automatización | Validación de cupos en tiempo real |
| **Protección de Sobreventa** | Inexistente (se aceptan pedidos a ciegas) | Bloqueo Inteligente | Cierre automático al saturar franja |
| **Confirmación de Pago** | Revisión manual de capturas | Integración | Pasarela con reserva temporal *Hold* |
| **Seguimiento del Cliente** | El cliente pregunta constantemente por chat | Autoseguimiento | Consulta de estado y tracking en vivo |

---

## 6. Qué gestiona la plataforma

* **Catálogo e Inventario:** Productos, precios y stock por tienda.
* **Motor de Capacidad:** Límites por día/franja y excepciones temporales.
* **Checkout y Pagos:** Bloqueo temporal anti-concurrencia y confirmación de órdenes.
* **Dashboard Comerciante:** Panel de control de pedidos y cambio de estados.
* **Tracking Cliente:** Vista pública para consultar avance del pedido.

---

[⬅ Alcance del MVP](../../03-requisitos/alcance-mvp.md) | [🏠 Volver al Índice General](../../../README.md) | [Sprint 01 ➡](../../06-scrum/sprint-01.md)