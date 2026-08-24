<div align="center">

# Jaldi Shop

### Plataforma de Gestion de Pedidos y Capacidad para MYPE

[![GitHub repo size](https://img.shields.io/github/repo-size/iJosueeh/jaldishop)](https://github.com/iJosueeh/jaldishop)
[![GitHub last commit](https://img.shields.io/github/last-commit/iJosueeh/jaldishop)](https://github.com/iJosueeh/jaldishop)
[![GitHub issues](https://img.shields.io/github/issues/iJosueeh/jaldishop)](https://github.com/iJosueeh/jaldishop/issues)
[![GitHub stars](https://img.shields.io/github/stars/iJosueeh/jaldishop)](https://github.com/iJosueeh/jaldishop/stargazers)

---

</div>

## Descripcion

<div align="center">

### Control inteligente de capacidad para MYPE

</div>

**JaldiShop** es una plataforma creada para micro, pequenas y medianas empresas que gestionan sus pedidos a traves de canales digitales como **WhatsApp** e **Instagram**, y cuya operacion se ve limitada por su capacidad real de produccion, atencion o entrega. Las MYPE que trabajan bajo pedido enfrentan diariamente problemas como gestion manual, datos dispersos, sobreventa y pedidos incumplidos debido a la falta de control de capacidad. JaldiShop resuelve esto funcionando como una **capa de orden** que centraliza pedidos, controla capacidad automaticamente, verifica disponibilidad en tiempo real y ofrece seguimiento al cliente, sin reemplazar WhatsApp o Instagram sino complementando la operacion del negocio.

---

## Equipo

| Miembro | Rol | Responsabilidad Sprint 1 |
|---------|-----|--------------------------|
| **[Josue Royer Tanta Cieza](https://github.com/iJosueeh)** | Desarrollador | Caso A y Caso C |
| **[Katherine Patricia Salas Quiroz](https://github.com/kath144)** | Desarrolladora | Caso B |
| **[Mia Vitalia Gual Vega](https://github.com/miagv)** | Desarrolladora | Flujo AS-IS / TO-BE |

---

## Stack Preliminar

<div align="center">

| Tecnologia | Uso |
|------------|-----|
| ![Spring](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) | Backend |
| ![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white) | Frontend |
| ![Angular](https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white) | Frontend |
| ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) | Base de datos |
| ![WebSocket](https://img.shields.io/badge/WebSocket-764ABC?style=for-the-badge&logo=socket.io&logoColor=white) | Tiempo real |

</div>

---

## Estado Actual

<div align="center">

`sprint-1` `Establecimiento del modelo de capacidad`

</div>

| Sprint | Estado | Descripcion |
|:------:|:------:|-------------|
| 1 | `EN PROGRESO` | Establecimiento del modelo de capacidad |
| 2 | `PENDIENTE` | Desarrollo del backend |
| 3 | `PENDIENTE` | Desarrollo del frontend |
| 4 | `PENDIENTE` | Union y pruebas |

---

## Documentacion del Proyecto

Toda la documentacion del proyecto se encuentra disponible en la carpeta [`/docs`](./docs).

```
jaldishop/
├── docs/
│   ├── 01-producto/
│   │   └── propuesta.md              # Propuesta del producto
│   ├── 02-investigacion/
│   │   ├── caso-a-pasteleria.md      # Caso A: Capacidad en pasteleria
│   │   └── caso-c-logistica.md       # Caso C: Capacidad logistica y delivery
│   ├── 04-design/
│   │   └── proceso/
│   │   └── as-is-to-be.md        # Estudio de proceso actual vs futuro
│   └── 06-scrum/
│       └── sprint-01.md              # Sprint 1: Modelo de capacidad
├── backend/                          # Spring Boot
├── frontend/                         # Next.js / Angular
└── README.md
```

---

## Casos de Estudio — Modelo de Capacidad

<div align="center">

### Sprint 1: Establecimiento del Modelo de Capacidad v1

</div>

| Caso | Tema | Estado | Responsable |
|:----:|------|:------:|:-----------:|
| **A** | Capacidad en Pasteleria | `EN REVISION` | Josue |
| **B** | Dark Kitchen | `EN PROGRESO` | Katherine |
| **C** | Capacidad Logistica y Delivery | `EN REVISION` | Josue |

### Conceptos Clave

| Concepto | Descripcion |
|----------|-------------|
| **Capacidad productiva** | Cuanto puede producir o preparar el negocio |
| **Capacidad logistica** | Cuantas operaciones de entrega puede asumir |
| **Capacidad base** | Capacidad habitual configurada por el negocio |
| **Excepciones temporales** | Cambios puntuales sin alterar la base |

---

## Flujo del Proceso

<div align="center">

### AS-IS (Actual) vs TO-BE (Propuesto)

</div>

| Aspecto | AS-IS | TO-BE |
|---------|-------|-------|
| Gestion de pedidos | Manual, dispersa | Centralizada en plataforma |
| Control de capacidad | Calculo manual | Verificacion automatica |
| Confirmacion de pago | Verificacion manual | Integrada en sistema |
| Seguimiento del pedido | Cliente pregunta | Cliente consulta en plataforma |

> Ver diagramas completos en [`as-is-to-be.md`](./docs/04-design/proceso/as-is-to-be.md)

---

## Funciones Centrales

1. Ajuste de capacidad real
2. Gestion centralizada de pedidos
3. Control automatico de disponibilidad
4. Seguimiento del pedido
5. Datos basicos de clientes frecuentes

---

## Publico Objetivo

MYPE con produccion o atencion limitada, especialmente negocios que trabajan bajo pedido:

- Panaderias y reposterias
- Dark kitchens
- Comida preparada
- Regalos personalizados
- Servicios de delivery

---

<div align="center">

**Jaldishop** - Soluciones para MYPE

</div>
