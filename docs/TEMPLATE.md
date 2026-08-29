<div align="center">

# [TÍTULO DEL DOCUMENTO]
### [Subtítulo o Módulo del Sistema]

[![Estado](https://img.shields.io/badge/Estado-Borrador%20%7C%20Revisión%20%7C%20Aprobado-blue?style=for-the-badge)](#)
[![Sprint](https://img.shields.io/badge/Sprint-0X-orange?style=for-the-badge)](#)
[![Autor](https://img.shields.io/badge/Autor-Nombre-lightgrey?style=for-the-badge)](#)

</div>

`📍 Docs` > `XX-Categoría` > **[Nombre del Archivo]**  
[⬅ Documento Anterior](./anterior.md) | [🏠 Índice General](../../README.md) | [Documento Siguiente ➡](./siguiente.md)

---

<details open>
<summary><b>📑 Tabla de Contenidos</b></summary>

- [1. Propósito y Contexto](#1-propósito-y-contexto)
- [2. Diagrama Conceptual / Flujo](#2-diagrama-conceptual--flujo)
- [3. Especificaciones Técnicas](#3-especificaciones-técnicas)
- [4. Reglas de Negocio](#4-reglas-de-negocio)
- [5. Consideraciones de Implementación](#5-consideraciones-de-implementación)

</details>

---

## 1. Propósito y Contexto

> [!NOTE]
> Breve resumen ejecutivo de 2 a 3 líneas describiendo la finalidad del documento y su aporte al proyecto.

Explica aquí el problema que aborda este entregable o módulo.

---

## 2. Diagrama Conceptual / Flujo

```mermaid
flowchart LR
    A[Inicio] --> B{¿Validación?}
    B -->|Sí| C[Procesar Operación]
    B -->|No| D[Rechazar]
    C --> E[Fin]
```

---

## 3. Especificaciones Técnicas

### 3.1 Estructura de Datos / Entidades

| Campo | Tipo | Requerido | Descripción |
|---|:---:|:---:|---|
| `id` | `UUID` | Sí | Identificador único |
| `nombre` | `VARCHAR(100)` | Sí | Nombre descriptivo |
| `estado` | `ENUM` | Sí | Estado actual de la entidad |

---

## 4. Reglas de Negocio

> [!IMPORTANT]
> **Regla Crítica:** Destaca aquí las validaciones o invariantes fundamentales de negocio que el backend debe asegurar.

> [!TIP]
> **Recomendación:** Sugerencias para optimización o buenas prácticas de integración.

> [!WARNING]
> **Riesgo Operativo:** Puntos donde pueda ocurrir concurrencia, fallo de datos o excepciones.

---

## 5. Consideraciones de Implementación

- [ ] Tarea técnica 1 relacionada
- [ ] Tarea técnica 2 relacionada
- [ ] Pruebas unitarias/integración asociadas

---

<div align="center">

[⬅ Documento Anterior](./anterior.md) | [🏠 Volver al Índice General](../../README.md) | [Documento Siguiente ➡](./siguiente.md)

</div>
