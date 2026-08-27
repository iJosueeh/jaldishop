<div align="center">

# CASO B

## Capacidad en Dark Kitchen

`Investigacion` `Modelo de Capacidad` `Operaciones`

</div>

---

## 1. Objetivo

> Analizar como deberia funcionar el modelo de capacidad en una Dark Kitchen que recibe una alta concentracion de pedidos durante periodos cortos.

Este caso busca identificar como controlar la capacidad cuando el limite del negocio **no depende unicamente del stock disponible**, sino tambien de su capacidad operativa para preparar, empaquetar y despachar pedidos.

---

## 2. Contexto del Caso

<blockquote>

**Situacion:** Una Dark Kitchen puede recibir una gran cantidad de pedidos en periodos de alta demanda, especialmente durante determinadas franjas horarias.

</blockquote>

**Momentos de alta demanda:**

| Periodo | Ejemplo |
|---------|---------|
| Fines de semana | Viernes y sabados por la noche |
| Horarios de comida | Horarios de almuerzo y cena |
| Promociones | Ofertas especiales |
| Fechas especiales | Navidad, dia de la madre, etc. |

El negocio puede disponer de ingredientes suficientes para continuar vendiendo, pero su cocina **no necesariamente tiene la capacidad operativa** para procesar todos los pedidos al mismo tiempo.

> **Conclusion clave:** Tener productos o ingredientes disponibles **no significa** tener capacidad para aceptar otro pedido.

---

## 3. Problema Identificado

<div align="center">

### El problema principal es la **sobreventa operativa**

</div>

El negocio puede continuar recibiendo pedidos incluso cuando su capacidad real de preparacion ya se encuentra saturada.

**Consecuencias:**

| Consecuencia | Descripcion |
|--------------|-------------|
| Acumulacion de pedidos | Demasiados pedidos al mismo tiempo |
| Tiempos mayores | Preparacion mas lenta de lo normal |
| Retrasos | Pedidos no entregados a tiempo |
| Reclamos | Clientes insatisfechos |
| Cancelaciones | Pedidos anulados |
| Presion sobre personal | Sobrecarga de trabajo |
| Incumplimiento | No cumplir con el tiempo prometido |

> El problema no necesariamente esta en vender un producto sin stock, sino en **comprometer mas trabajo** del que la operacion puede procesar durante un periodo.

---

## 4. Cuellos de Botella

Durante el analisis se identifico que una Dark Kitchen puede tener diferentes recursos que limiten su capacidad.

| Recurso | Capacidad Aproximada |
|---------|:--------------------:|
| Cocina caliente | 6 platos / 15 min |
| Cocina fria | 5 platos / 15 min |
| Empaque | 10 pedidos / 15 min |

> **Nota:** El cuello de botella puede cambiar dependiendo del contenido de los pedidos.

---

## 5. Capacidad por Periodo

Para una Dark Kitchen, controlar unicamente la capacidad diaria puede ser insuficiente.

**Ejemplo:**

```
Capacidad diaria: 100 pedidos

Problema: 40 pedidos ingresan simultaneamente entre 8:00pm y 8:15pm
```

Por ello, el Caso B plantea utilizar **periodos mas pequenos**.

| Franja | Capacidad |
|--------|:---------:|
| 19:00 - 19:30 | 10 pedidos |
| 19:30 - 20:00 | 15 pedidos |
| 20:00 - 20:30 | 15 pedidos |
| 20:30 - 21:00 | 12 pedidos |

> La capacidad por franja permite **controlar mejor** la carga operativa.
>
> La capacidad diaria puede continuarse utilizando para planificacion general.

---

## 6. Capacidad Base

El negocio podra disponer de una **capacidad habitual** para sus periodos de operacion.

**Ejemplo - Viernes:**

| Franja | Capacidad Base |
|--------|:--------------:|
| 19:00 - 19:30 | 10 |
| 19:30 - 20:00 | 15 |
| 20:00 - 20:30 | 15 |
| 20:30 - 21:00 | 12 |

> Esta configuracion representa la operacion normal del negocio.

---

## 7. Consumo de Capacidad

El analisis del Caso B plantea que **no todos los pedidos generan la misma carga operativa**.

**Ejemplo - Pedido A:**

| Producto | Cantidad | Area |
|----------|:--------:|------|
| Ceviche | 3 | Cocina fria |
| Platos calientes | 3 | Cocina caliente |

```
Pedido
+-- Cocina fria
|   +-- 3 platos
|
+-- Cocina caliente
    +-- 3 platos
```

> Esto permite plantear un modelo donde diferentes productos consumen capacidad de diferentes recursos.

Sin embargo, este nivel de granularidad podria generar **demasiada configuracion** para una MYPE.

> **Nota:** El control detallado por estaciones se considera una posibilidad del dominio, pero no necesariamente una funcionalidad obligatoria del MVP.

---

## 8. Modelo Simple para el MVP

Como alternativa al control detallado por estaciones, se plantea un modelo mas sencillo:

<blockquote>

> Maximo N pedidos por franja.

</blockquote>

**Ejemplo:**

```
20:00 - 20:30
Capacidad maxima: 15 pedidos

Si existen 15 pedidos comprometidos:
  Capacidad disponible = 0

El sistema debera impedir que se confirmen nuevos pedidos para esa franja.
```

> Esta alternativa **reduce la configuracion** necesaria para el negocio y evita sobreingenieria durante el MVP.

---

## 9. Saturacion

Cuando una franja se aproxima o alcanza su limite, el sistema debe evitar continuar comprometiendo capacidad.

**Comportamientos posibles:**

| Modo | Descripcion |
|------|-------------|
| Busy Mode | Aviso de alta demanda |
| Throttle | Limitar pedidos gradualmente |
| Freeze | Cerrar temporalmente |

**Ejemplo de niveles:**

```
80%  -> advertencia de alta demanda
95%  -> limitar pedidos
100% -> cerrar temporalmente
```

> Estos comportamientos pueden ser utiles, pero **no son necesarios** para demostrar el modelo basico de capacidad.

**Para el MVP se plantea inicialmente:**

```
Capacidad disponible > 0
  -> aceptar pedido

Capacidad disponible = 0
  -> no aceptar nuevos pedidos para la franja
```

> Los modos avanzados de saturacion quedan como evolucion.

---

## 10. Prioridad entre Pedidos

Puede ocurrir que dos clientes intenten utilizar la ultima capacidad disponible.

```
Capacidad disponible = 1

Cliente A ---+
             +-- intenta obtener el ultimo cupo
Cliente B ---+
```

> El sistema debera garantizar que **unicamente uno** pueda comprometer esa capacidad.

La implementacion tecnica de esta regla se definira posteriormente durante arquitectura.

---

## 11. Reserva Temporal

Existe un periodo entre:

1. Comprobar que existe capacidad
2. Iniciar checkout
3. Realizar el pago
4. Recibir confirmacion del pago

Por ello, se plantea la posibilidad de crear una **reserva temporal** de capacidad.

```
DISPONIBLE
    |
    v
RESERVADA TEMPORALMENTE
    |
    v
+---------------+
|               |
Pago OK      Pago falla /
|            expira
|               |
v               v
CONFIRMADA    LIBERADA
```

> El tiempo exacto de duracion de la reserva todavia debe definirse.

---

## 12. Ajustes Temporales

La capacidad normal del negocio puede cambiar temporalmente.

<details>
<summary><strong>Clic para ver ejemplos de ajustes</strong></summary>

### Reduccion

| Motivo | Ejemplo |
|--------|---------|
| Ausencia de personal | Trabajador enfermo |
| Falla de equipo | Horno descompuesto |
| Mantenimiento | Limpieza programada |
| Menor disponibilidad | Dia festivo |

### Incremento

| Motivo | Ejemplo |
|--------|---------|
| Personal adicional | Contratacion temporal |
| Equipos adicionales | Alquiler de horno |
| Refuerzo especial | Fecha de alta demanda |

</details>

**Ejemplo:**

```
Capacidad base:
  20:00 - 20:30 -> 15

Excepcion:
  20:00 - 20:30 -> 8

Motivo: Falla de equipo
```

> La excepcion **no modifica permanentemente** la capacidad habitual.
>
> Cuando termina el periodo excepcional, vuelve a aplicarse la capacidad base.

---

## 13. Ciclo de Vida del Pedido

Un flujo conceptual del Caso B seria:

```
+-------------------------------------+
| Cliente selecciona productos        |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
| Selecciona fecha y franja           |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
| Calcular capacidad requerida        |
+------------------+------------------+
                   |
                   v
           +-------+-------+
           | Existe         |
           | capacidad?     |
           +-------+-------+
            No     |     Si
   +-----------+   |   +-----------+
   | Mostrar   |   |   | Continuar |
   | franja no |   |   | al        |
   | disponible|   |   | checkout  |
   +-----+-----+  |   +-----+-----+
         |         |         |
         v         v         v
    +---------+  +------------------+
    | Mostrar |  | Reservar         |
    | otras   |  | temporalmente    |
    | franjas |  | capacidad        |
    +---------+  +--------+---------+
                         |
                         v
                +--------+---------+
                | Cliente realiza  |
                | pago             |
                +--------+---------+
                         |
                         v
                +--------+---------+
                | Pago confirmado? |
                +--------+---------+
                  No     |     Si
             +-----------+-----------+
             |                       |
             v                       v
      +------------+         +------------+
      | Liberar    |         | Confirmar  |
      | reserva    |         | pedido     |
      +------------+         +-----+------+
                                    |
                                    v
                           +--------+--------+
                           | Comprometer     |
                           | capacidad       |
                           +--------+--------+
                                    |
                                    v
                           +--------+--------+
                           | Pedido          |
                           | recibido        |
                           +--------+--------+
                                    |
                                    v
                           +--------+--------+
                           | En preparacion  |
                           +--------+--------+
                                    |
                                    v
                           +--------+--------+
                           | Preparado       |
                           +--------+--------+
                                    |
                                    v
                           +--------+--------+
                           | Empaque         |
                           +--------+--------+
                                    |
                                    v
                           +--------+--------+
                           | Despacho/recojo |
                           +--------+--------+
                                    |
                                    v
                           +--------+--------+
                           | Finalizado      |
                           +-----------------+
```

---

## 14. Cancelaciones

La cancelacion debera considerar el estado del pedido.

| Momento | Capacidad | Recursos |
|---------|:---------:|----------|
| **Antes de iniciar preparacion** | Se libera completamente | No se consumen |
| **Durante preparacion** | Se libera parcialmente | Tiempo, ingredientes, trabajo |
| **Pedido preparado** | No se libera | Trabajo ya realizado |

> **Regla:** Liberar capacidad y recuperar recursos **no son necesariamente** la misma operacion.

---

## 15. Reglas Preliminares RN-B

| Codigo | Regla |
|:------:|-------|
| **RN-B01** | El negocio podra configurar una capacidad maxima para un periodo de operacion |
| **RN-B02** | La capacidad podra configurarse mediante franjas horarias cuando la operacion del negocio lo requiera |
| **RN-B03** | Un pedido no podra confirmarse cuando la capacidad requerida supere la capacidad disponible |
| **RN-B04** | Los pedidos consumiran capacidad del periodo en el que deberan ser atendidos |
| **RN-B05** | Cuando la capacidad disponible llegue a cero, no deberan confirmarse nuevos pedidos para dicho periodo |
| **RN-B06** | Cuando una franja se encuentre saturada, el sistema podra ofrecer otras franjas disponibles |
| **RN-B07** | El negocio podra aumentar o reducir temporalmente su capacidad sin modificar la configuracion base |
| **RN-B08** | Cuando varios pedidos compitan por la ultima capacidad disponible, el sistema debera garantizar que unicamente uno pueda comprometerla |
| **RN-B09** | La capacidad podra reservarse temporalmente durante el proceso de checkout |
| **RN-B10** | Una reserva temporal podra convertirse en capacidad comprometida cuando el pago sea confirmado |
| **RN-B11** | Una reserva debera liberarse cuando el pago falle o cuando la reserva expire |
| **RN-B12** | La recuperacion de capacidad despues de una cancelacion dependera del estado operativo del pedido |

---

## 16. Decisiones Preliminares DP-B

### DP-B01 — Franjas

> Para operaciones de alta rotacion, la capacidad por franja representa mejor el problema que utilizar unicamente capacidad diaria.

### DP-B02 — Modelo Detallado por Estaciones

> El control independiente de cocina fria, cocina caliente, empaque y otros recursos se considera valido conceptualmente.
>
> Sin embargo, debera evaluarse si su complejidad es adecuada para el MVP.

### DP-B03 — Modelo Simplificado

> Para el MVP se considera como alternativa principal: **Maximo N pedidos por periodo o franja**.

### DP-B04 — Modificaciones Temporales

> Las variaciones temporales de capacidad no modificaran permanentemente la configuracion base.

---

## 17. Elementos Candidatos a Evolucion

Los siguientes elementos surgieron durante el Caso B, pero **no son necesarios inicialmente**:

| Elemento | Tipo |
|----------|------|
| Capacidad independiente por estacion | Modelo detallado |
| Consumo ponderado segun producto | Modelo detallado |
| Busy Mode | Modo avanzado |
| Throttle automatico | Modo avanzado |
| Freeze automatico | Modo avanzado |
| ETA dinamico | Funcionalidad avanzada |
| Gestion avanzada del despacho | Funcionalidad avanzada |
| Recalibracion automatica de capacidad | Funcionalidad avanzada |

---

## 18. Dudas del Caso B

| Codigo | Pregunta | Area |
|:------:|----------|------|
| **D-B01** | El MVP debe controlar unicamente pedidos por franja o tambien productos/platos? | Alcance |
| **D-B02** | Las estaciones internas deben modelarse desde la primera version? | Modelo |
| **D-B03** | Cuanto tiempo debe durar una reserva temporal durante checkout? | Tecnico |
| **D-B04** | Que ocurre si el pago se confirma cuando la reserva ya expiro? | Tecnico |
| **D-B05** | Que estados definitivos tendra un pedido? | Modelo |
| **D-B06** | Como afectara una cancelacion a la capacidad cuando la preparacion ya comenzo? | Cancelaciones |
| **D-B07** | Los modos Busy/Throttle/Freeze aportan suficiente valor para justificar su complejidad en el MVP? | Alcance |

---

## 19. Conclusion

<blockquote>

El Caso B demuestra que la capacidad de una MYPE puede variar considerablemente durante el dia.

</blockquote>

Una Dark Kitchen puede disponer de ingredientes suficientes y, sin embargo, **no tener capacidad operativa** para aceptar mas pedidos durante una franja determinada.

Tambien demuestra que es posible construir modelos muy detallados mediante estaciones y pesos de consumo, pero estos modelos incrementan considerablemente la complejidad.

Por ello, el caso permite plantear dos niveles:

```
MVP
+-- capacidad simple por periodo/franja

Evolucion
+-- capacidad ponderada
    +-- multiples recursos
        +-- estaciones
```

Las conclusiones deberan consolidarse con:

| Caso | Descripcion |
|------|-------------|
| **Caso A** | Panaderia |
| **Caso C** | Capacidad logistica |

---

## 20. Estado

<div align="center">

| Estado | Detalle |
|:------:|---------|
| `CONSOLIDADO` | Incorporado a matriz de consolidacion y Modelo de Capacidad v1 |

Las reglas RN-B fueron incorporadas a las decisiones generales DP-G y al **Modelo de Capacidad v1**.

</div>