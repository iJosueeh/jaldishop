<div align="center">

# MATRIZ DE CONSOLIDACION

## Casos A + B + C

`Investigacion` `Modelo de Capacidad` `Consolidacion`

</div>

---

## 1. Objetivo

> Consolidar los hallazgos obtenidos en los tres escenarios analizados para identificar el **modelo general** que pueda utilizar JaldiShop.

**Casos analizados:**

| Caso | Tema |
|:----:|------|
| **A** | Panaderia |
| **B** | Dark Kitchen |
| **C** | Capacidad logistica |

> El proposito de esta matriz **no es implementar** literalmente los tres casos, sino descubrir un modelo general.

---

## 2. Matriz Comparativa

| Concepto | Caso A | Caso B | Caso C | Consolidacion |
|----------|--------|--------|--------|:-------------:|
| Capacidad limitada | Produccion | Operacion de cocina | Produccion / entrega | General |
| Capacidad base | Si | Si | Si | General |
| Periodo | Principalmente dia | Franjas | Fecha / franja | Configurable |
| Pedido consume capacidad | Segun unidades | Segun carga operativa | Produccion / destino | General |
| Consumo ponderado | Posible | Propuesto | Posible | Evolucion |
| Multiples recursos | Posible | Estaciones | Produccion / logistica | Evolucion |
| Excepciones temporales | Si | Si | Si | General |
| Capacidad por franja | Posible | Fundamental | Fundamental | General configurable |
| Saturacion | Bloqueo | Bloqueo / modos avanzados | Franja no disponible | General |
| Prioridad | Pago confirmado | Pago confirmado | Pendiente | General propuesta |
| Reserva durante checkout | Pendiente | Planteada | Pendiente | MVP propuesto |
| Cancelacion | Segun avance | Segun estado | Produccion / logistica | General |
| Liberacion de capacidad | Condicional | Condicional | Condicional | General |
| Delivery | No central | Despacho | Central | Configurable |
| Maps | No necesario | No central | Cobertura / distancia | Especifico |
| Estaciones internas | No | Si | No | Evolucion |
| Busy Mode / ETA | No | Si | No | Evolucion |
| Optimizacion de rutas | No | No esencial | Analizada | Fuera del MVP |
| Inventario | Relacionado | Relacionado | No central | Separar de capacidad |

---

## 3. Conceptos Comunes

### 3.1 Capacidad

Los tres casos muestran que una MYPE puede tener un **limite operativo diferente** al inventario disponible.

<blockquote>

> Capacidad representa cuanto trabajo adicional puede comprometer el negocio durante un periodo.

</blockquote>

---

### 3.2 Capacidad Base

Los tres casos requieren una configuracion habitual.

```
Negocio
   |
   v
Capacidad base
   |
   v
Periodo
```

> Esta configuracion representa la operacion normal.

---

### 3.3 Periodo

La capacidad siempre se encuentra asociada a un periodo.

```
Periodo
+-- Dia
+-- Franja horaria
```

> Por ello, el periodo debe considerarse configurable.

---

### 3.4 Consumo

Un pedido consume parte de la capacidad disponible.

```
capacidad_disponible =
  capacidad_efectiva - capacidad_comprometida
```

Cuando:

```
capacidad_disponible = 0
```

> El sistema no debera continuar confirmando pedidos para ese periodo.

---

### 3.5 Excepciones Temporales

Los tres casos identifican situaciones donde la capacidad habitual puede cambiar temporalmente.

**Ejemplos:**

| Tipo | Ejemplos |
|------|----------|
| Reduccion | Ausencia de personal, falla de equipos |
| Incremento | Personal adicional, fechas especiales |

```
CAPACIDAD BASE
      |
      v
+-----------+
| Existe     |
| excepcion? |
+-----------+
 No     |     Si
 |      |      |
 v      v      v
Base  Excepcion
 \      /
  \    /
   v  v
CAPACIDAD EFECTIVA
```

> Una excepcion **no modifica permanentemente** la configuracion base.

---

### 3.6 Saturacion

Los tres casos necesitan impedir que se continue comprometiendo capacidad cuando el limite se alcanza.

| Caso | Comportamiento |
|------|----------------|
| Panaderia | Fecha agotada |
| Dark Kitchen | Franja agotada |
| Delivery | Horario no disponible |

> El comportamiento visual puede cambiar, pero **la regla general es la misma**.

---

### 3.7 Cancelaciones

Los tres casos muestran que cancelar un pedido **no significa automaticamente** recuperar todos los recursos utilizados.

```
Liberacion de capacidad
        !=
Recuperacion de recursos
```

> La capacidad que puede reutilizarse dependera del estado del pedido.

---

## 4. Elementos Configurables

Los siguientes conceptos aparecen en varios casos, pero su comportamiento depende del negocio.

| Concepto | Configuracion Posible |
|----------|----------------------|
| Periodo | Dia / franja |
| Capacidad base | Valor definido por negocio |
| Excepcion | Aumento / reduccion / cierre |
| Modalidad | Recojo / delivery |
| Cobertura | Distancia o zona |
| Costo delivery | Fijo / distancia / zona |

---

## 5. Elementos Especificos para el Nucleo Inicial

Algunos conceptos son utiles para determinados negocios, pero **no deben convertirse** inmediatamente en reglas generales.

<details>
<summary><strong>Clic para ver elementos especificos</strong></summary>

### Caso B

| Elemento | Tipo |
|----------|------|
| Cocina fria | Recurso especifico |
| Cocina caliente | Recurso especifico |
| Empaque | Recurso especifico |
| Busy Mode | Modo avanzado |
| Throttle | Modo avanzado |
| Freeze | Modo avanzado |
| ETA dinamico | Funcionalidad avanzada |

### Caso C

| Elemento | Tipo |
|----------|------|
| Multiples motorizados | Recurso especifico |
| Optimizacion de rutas | Funcionalidad avanzada |
| Tracking GPS | Funcionalidad avanzada |
| Gestion avanzada de recorridos | Funcionalidad avanzada |

</details>

> Estos elementos pueden considerarse **extensiones futuras**.

---

---

## 6. Decisiones Generales Propuestas

### DP-G01 — Capacidad Simple

**Decision propuesta:** El MVP utilizara un modelo simple de capacidad.

```
Capacidad maxima = 10
Capacidad comprometida = 7
Capacidad disponible = 3
```

> El consumo ponderado queda como evolucion posterior.

**Motivo:** Permite validar el concepto central sin obligar a la MYPE a configurar pesos, estaciones o formulas complejas.

---

### DP-G02 — Capacidad Operativa Principal

**Decision propuesta:** El MVP manejara una capacidad operativa principal por configuracion.

No sera obligatorio controlar simultaneamente:

| Recurso | Estado en MVP |
|---------|:-------------:|
| Horno | Opcional |
| Cocina | Opcional |
| Empaque | Opcional |
| Repartidores | Opcional |
| Personal | Opcional |

**Evolucion:**

```
Negocio
+-- Capacidad produccion
+-- Capacidad horno
+-- Capacidad empaque
+-- Capacidad delivery
```

---

### DP-G03 — Periodo Configurable

**Decision propuesta:** JaldiShop permitira configurar capacidad por dia o por franja horaria.

<details>
<summary><strong>Clic para ver ejemplos</strong></summary>

**Ejemplo A - Panaderia:**

```
Sabado
Capacidad: 8
```

**Ejemplo B - Dark Kitchen:**

```
20:00 - 20:30 -> 15
20:30 - 21:00 -> 12
```

</details>

---

### DP-G04 — Reserva Temporal Durante Checkout

**Decision propuesta:** Cuando el cliente inicia el checkout, JaldiShop podra reservar temporalmente la capacidad requerida.

```
DISPONIBLE
    |
    v
RESERVADA
    |
    v
+--------------+
|              |
Pago OK     Falla / expira
|              |
v              v
CONFIRMADA   LIBERADA
```

> La duracion exacta de la reserva debera definirse posteriormente.

---

### DP-G05 — Proteccion Ante Concurrencia

**Decision propuesta:** El sistema debera garantizar que dos clientes no puedan comprometer simultaneamente el ultimo cupo disponible.

```
Capacidad disponible = 1

Cliente A ---+
             +-- ultimo cupo
Cliente B ---+

Resultado: Solo un pedido puede confirmar la capacidad.
```

> La estrategia tecnica concreta se definira durante arquitectura e implementacion.

---

### DP-G06 — Logistica Acotada

**Decision propuesta:** Cuando el negocio utilice delivery, el MVP podra considerar:

| Funcionalidad | Estado |
|---------------|:------:|
| Ubicacion del negocio | Incluir |
| Direccion del cliente | Incluir |
| Cobertura | Incluir |
| Distancia | Incluir |
| Modalidad delivery/recojo | Incluir |
| Costo basico | Incluir |
| Disponibilidad de la franja | Incluir |
| Optimizacion automatica de rutas | Excluir |
| Tracking GPS | Excluir |
| Asignacion inteligente de repartidores | Excluir |
| Prediccion de transito | Excluir |

---

## 7. Modelo Conceptual Resultante

Despues de consolidar A, B y C, el modelo preliminar puede representarse asi:

```
Negocio
   |
   v
Configura capacidad
   |
   v
+-----------+
| Tipo de   |
| periodo   |
+-----------+
 Dia     |     Franja
 |       |       |
 v       v       v
Capacidad  Capacidad
diaria     por franja
 |       |       |
 v       v       v
+-------------------+
| Capacidad base    |
+-------------------+
         |
         v
+-------------------+
| Existe excepcion  |
| temporal?         |
+-------------------+
  No     |     Si
   |     |      |
   v     v      v
Base  Excepcion
 \      /
  \    /
   v  v
+-------------------+
| Capacidad efectiva|
+-------------------+
         |
         v
+-------------------+
| Cliente solicita  |
| pedido            |
+-------------------+
         |
         v
+-------------------+
| Existe capacidad? |
+-------------------+
  No     |     Si
   |     |      |
   v     v      v
Bloquear  Reserva
periodo   temporal
             |
             v
+-------------------+
| Checkout / pago   |
+-------------------+
         |
         v
+-------------------+
| Pago confirmado?  |
+-------------------+
  No/expira  |  Si
   |         |    |
   v         v    v
Liberar   Confirmar
capacidad pedido
              |
              v
+-------------------+
| Capacidad         |
| comprometida      |
+-------------------+
         |
         v
+-------------------+
| Preparacion       |
+-------------------+
         |
         v
+-------------------+
| Pedido cancelado? |
+-------------------+
  No     |     Si
   |     |      |
   v     v      v
Continuar Evaluar
pedido    capacidad
          recuperable
              |
              v
+-------------------+
| Liberar capacidad |
| reutilizable      |
+-------------------+
```

---

## 8. Modelo Matematico Conceptual

**Para el MVP:**

```
Capacidad efectiva =
  capacidad base
  o capacidad excepcional
```

**Luego:**

```
Capacidad disponible =
  capacidad efectiva
  - capacidad comprometida
  - capacidad reservada
```

**Un podra continuar unicamente cuando:**

```
capacidad_requerida <= capacidad_disponible
```

**Para el modelo simple inicial:**

```
capacidad_requerida = cantidad configurada para el pedido
```

> El consumo ponderado queda fuera del Modelo v1.

---

## 9. Alcance Preliminar del Modelo de Capacidad v1

<div align="center">

### Incluido

</div>

| Elemento | Estado |
|----------|:------:|
| Capacidad base | Incluir |
| Dia | Incluir |
| Franjas horarias | Incluir |
| Capacidad disponible | Incluir |
| Capacidad comprometida | Incluir |
| Reserva temporal | Incluir |
| Saturacion | Incluir |
| Excepciones temporales | Incluir |
| Confirmacion mediante pedido/pago | Incluir |
| Liberacion de capacidad | Incluir |
| Reglas basicas de cancelacion | Incluir |
| Delivery basico cuando corresponda | Incluir |

<div align="center">

### No Incluido Inicialmente

</div>

| Elemento | Estado |
|----------|:------:|
| Capacidad ponderada | Excluir |
| Multiples recursos simultaneos | Excluir |
| Estaciones internas | Excluir |
| Busy Mode avanzado | Excluir |
| ETA dinamico | Excluir |
| Optimizacion de rutas | Excluir |
| Tracking GPS | Excluir |
| Asignacion inteligente de repartidores | Excluir |

---

## 10. Relacion con JaldiShop

El modelo busca mantener a JaldiShop como una **solucion general** para MYPE que trabajan bajo pedido.

No se pretende construir:

```
Sistema para panaderias
```

ni:

```
Sistema para Dark Kitchens
```

ni:

```
Sistema de delivery
```

> Los tres escenarios fueron utilizados para **descubrir reglas generales**.

El resultado buscado es:

```
JaldiShop
+-- Gestion de pedidos
    +-- Gestion de capacidad
        +-- Periodos
        +-- Limites
        +-- Reservas
        +-- Excepciones
        +-- Saturacion
```

---

## 11. Decisiones Pendientes de Aprobacion

El equipo debe aprobar o modificar:

| Decision | Tema |
|:--------:|------|
| DP-G01 | Capacidad simple |
| DP-G02 | Capacidad operativa principal |
| DP-G03 | Dia y franja horaria |
| DP-G04 | Reserva temporal |
| DP-G05 | Proteccion de concurrencia |
| DP-G06 | Alcance logistico |

> No es necesario volver a investigar los tres casos desde cero.

Cada integrante puede indicar:

```
APROBADO
```

o:

```
CAMBIAR DP-GXX

Motivo:
...

Propuesta:
...
```

---

## 12. Despues de la Aprobacion

Cuando las decisiones anteriores sean aprobadas, los hallazgos dejan de ser unicamente investigacion.

**Siguiente flujo:**

```
Casos A + B + C
      |
      v
Matriz de consolidacion
      |
      v
Aprobacion del equipo
      |
      v
Modelo de Capacidad v1
      |
      v
Alcance MVP
      |
      v
Requisitos
      |
      v
Modelo ER
      |
      v
Entregable 1
```

---

## 13. Estado

<div align="center">

| Estado | Detalle |
|:------:|---------|
| `CONSOLIDADO` | Aprobado e incorporado al Modelo de Capacidad v1 |

Las decisiones DP-G01 a DP-G06 fueron aprobadas y formalizadas en el **Modelo de Capacidad v1**.

</div>