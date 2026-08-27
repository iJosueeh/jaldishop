<div align="center">

# CASO A

## Capacidad en Pasteleria

`Investigacion` `Modelo de Capacidad` `MYPE`

</div>

---

## 1. Objetivo

> Analizar como deberia funcionar el **modelo de capacidad** en una pasteleria que trabaja bajo pedido y cuya capacidad de produccion es limitada.

El caso busca determinar como el sistema puede evitar que el negocio acepte **mas pedidos de los que realmente puede producir**, incluso cuando aun dispone de ingredientes o productos.

---

## 2. Contexto del Caso

<blockquote>

**Situacion:** Una pasteleria recibe pedidos de diferentes tipos y tamanos de tortas mediante canales como WhatsApp e Instagram.

</blockquote>

El negocio puede conocer aproximadamente cuantas tortas es capaz de producir durante un dia, pero esta capacidad **no necesariamente esta representada como stock**.

| Situacion | Descripcion |
|-----------|-------------|
| Ingredientes disponibles | El negocio tiene insumos suficientes |
| Mensajes entrantes | Puede seguir recibiendo pedidos |
| **Capacidad limitada** | Su tiempo, personal o equipamiento solo le permite producir una cantidad determinada |

> **Conclusion clave:** Disponer de ingredientes **no significa** tener capacidad para aceptar otro pedido.

---

## 3. Problema Identificado

<div align="center">

### El negocio puede comprometer pedidos **por encima de su capacidad real** de produccion

</div>

**Ejemplo practico:**

```
Capacidad base del sabado:     8 unidades
----------------------------------------------
Pedido 1 (Cliente A):         -4 unidades
                               -----------
Capacidad restante:            4 unidades

Pedido 2 (Cliente B):         -4 unidades
                               -----------
Capacidad restante:            0 unidades

Pedido 3 (Cliente C):         X NO ACEPTABLE
```

Aunque un nuevo cliente solicite un producto del catalogo, el negocio **ya no deberia aceptar** otro pedido para ese periodo.

---

## 4. Concepto de Capacidad

<div align="center">

```
Capacidad Disponible = Capacidad Configurada - Capacidad Comprometida
```

</div>

| Concepto | Definicion |
|----------|------------|
| **Capacidad base** | Cuanto puede producir el negocio normalmente en un periodo |
| **Capacidad comprometida** | Cuanto ya esta reservado por pedidos confirmados |
| **Capacidad disponible** | Lo que queda libre para nuevos pedidos |

> **Importante:** La capacidad **no representa inventario**. Representa cuanto trabajo adicional puede comprometer el negocio sin superar sus posibilidades reales de produccion.

---

## 5. Capacidad Base

El negocio podra establecer una **capacidad habitual** para cada dia de la semana.

| Dia | Capacidad | Nivel |
|-----|:---------:|:-----:|
| Lunes | 6 | Normal |
| Martes | 6 | Normal |
| Miercoles | 6 | Normal |
| Jueves | 8 | Medio |
| Viernes | 10 | Alto |
| Sabado | 12 | Maximo |
| Domingo | 8 | Medio |

> **Nota:** Esta configuracion representa la operacion normal del negocio. La capacidad base **no deberia modificarse** permanentemente por situaciones excepcionales.

---

## 6. Excepciones Temporales

El negocio puede encontrarse en situaciones donde temporalmente pueda producir **mas o menos** de lo habitual.

<details>
<summary><strong>Clic para ver ejemplos de excepciones</strong></summary>

| Tipo | Ejemplo |
|------|---------|
| Produccion aumentada | Contratacion de personal adicional |
| Produccion reducida | Mantenimiento de un horno |
| Produccion reducida | Ausencia de un trabajador |
| Produccion aumentada | Alta demanda por fecha especial |
| Produccion reducida | Reduccion voluntaria de pedidos |
| Produccion reducida | Cierre parcial de produccion |

</details>

**Ejemplo de excepcion:**

```
Capacidad habitual del sabado:  12
-------------------------------------
Sabado 15 (excepcion):         16  (incremento)

Efecto: Solo afecta esa fecha especifica
        La capacidad base de los demas sabados NO cambia
```

---

## 7. Capacidad segun Tipo de Producto

Durante el analisis se identifico que **diferentes productos pueden requerir diferentes niveles de esfuerzo**.

> Una torta grande puede requerir mas tiempo y recursos que una pequena.

### 7.1 Modelo Basico

<div align="center">

**1 torta = 1 cupo**

</div>

| Pedido | Unidades | Consumo |
|--------|:--------:|:-------:|
| Torta grande | 2 | 2 cupos |
| Torta mediana | 1 | 1 cupo |
| Torta pequena | 1 | 1 cupo |
| **Total** | **4** | **4 cupos** |

> Ventaja: **Sencillo** de configurar y comprender.

---

### 7.2 Modelo Ponderado

El negocio asigna **diferentes consumos** de capacidad segun el producto o tamano.

| Producto | Consumo | Relacion |
|----------|:-------:|:--------:|
| Torta grande | 1.00 | 100% |
| Torta mediana | 0.50 | 50% |
| Torta pequena | 0.25 | 25% |

> Ventaja: Representa mejor el **esfuerzo real** de produccion.
>
> Desventaja: Requiere **mayor configuracion** por parte del negocio.

| Decision | Estado |
|----------|--------|
| Modelo basico | Incluir en MVP |
| Modelo ponderado | **Evaluar despues** de integrar Casos A, B y C |

---

## 8. Pedidos con Multiples Unidades

La capacidad se consume segun el **contenido completo del pedido**, no solo por su existencia.

```
Capacidad disponible: 8

Pedido A (4 tortas)
  -> Capacidad restante: 4

Pedido B (4 tortas)
  -> Capacidad restante: 0

Pedido C
  -> X Ya no puede confirmarse
```

> **Regla:** Un pedido grande **no debe** permitir indirectamente superar el limite establecido por el negocio.

---

## 9. Prioridad de Confirmacion

<blockquote>

### La capacidad se compromete con el cliente que confirma primero el pago correspondiente

</blockquote>

| Criterio | Razon |
|----------|-------|
| Pago confirmado | Representa un **compromiso efectivo** entre cliente y negocio |

### Pregunta pendiente

> **Duda:** Que ocurre mientras varios clientes estan intentando pagar por los ultimos cupos disponibles?
>
> Este problema debera analizarse al definir las reglas generales de pago y concurrencia.

---

## 10. Cancelaciones y Recuperacion de Capacidad

<blockquote>

### Capacidad cancelada **no significa automaticamente** capacidad recuperada

</blockquote>

La recuperacion dependera del **avance real del pedido**:

| Caso | Estado | Recuperacion |
|------|--------|:------------:|
| **Caso 1** | Produccion no iniciada | Total |
| **Caso 2** | Produccion iniciada | Parcial |
| **Caso 3** | Producto practicamente terminado | Minima o nula |

> La recuperacion debera depender del avance real del pedido.

---

## 11. Flujo General del Caso A

```
+-------------------------------------+
|   Cliente selecciona productos      |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
|   Selecciona fecha disponible       |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
|   Sistema calcula capacidad         |
|   requerida por el pedido           |
+------------------+------------------+
                   |
                   v
           +-------+-------+
           |  Existe        |
           |  capacidad?    |
           +-------+-------+
            Si     |     No
    +-----------+  |  +-----------+
    | Continua  |  |  | Mostrar   |
    | con pedido|  |  | otra fecha|
    +-----+-----+  |  +-----------+
          |         |
          v         |
+-------------------------------------+
|   Cliente procede al pago           |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
|   Sistema revalida capacidad        |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
|   Confirmar pedido                  |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
|   Comprometer capacidad             |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
|   Preparacion                       |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
|   Finalizacion                      |
+-------------------------------------+
```

---

## 12. Reglas Preliminares del Caso A

| Codigo | Regla |
|:------:|-------|
| **RN-A01** | El negocio debera disponer de una capacidad base que represente su capacidad habitual de produccion |
| **RN-A02** | La capacidad disponible sera determinada considerando la capacidad configurada y la capacidad ya comprometida para el periodo |
| **RN-A03** | Un pedido debera consumir capacidad segun la cantidad y/o peso configurado para los productos que contiene |
| **RN-A04** | Un pedido no podra confirmarse cuando la capacidad requerida sea superior a la capacidad disponible |
| **RN-A05** | Los pedidos con multiples unidades deberan considerar el consumo total de todas las unidades solicitadas |
| **RN-A06** | El negocio podra modificar temporalmente su capacidad para una fecha o periodo especifico sin alterar su capacidad base |
| **RN-A07** | Una excepcion temporal tendra prioridad sobre la capacidad base durante el periodo configurado |
| **RN-A08** | La capacidad debera revalidarse antes de confirmar definitivamente un pedido |
| **RN-A09** | La confirmacion del pago sera considerada inicialmente como el momento de compromiso efectivo del pedido |
| **RN-A10** | La cancelacion de un pedido podra devolver capacidad unicamente cuando esta pueda ser reutilizada operativamente |
| **RN-A11** | La cantidad de capacidad recuperada ante una cancelacion podra depender del avance de preparacion del pedido |

---

## 13. Decisiones Preliminares

### DP-A01 — Capacidad Base

> Se utilizara una capacidad base configurable por el negocio, con posibilidad de modificaciones temporales.

### DP-A02 — Prioridad por Pago

> Inicialmente se considera que el cliente que confirma primero el pago obtiene prioridad sobre la capacidad disponible.

**Nota:** Esta decision debera revisarse al disenar el comportamiento de reservas temporales y concurrencia.

### DP-A03 — Modelo Basico y Ponderado

| Modelo | Decision |
|--------|----------|
| Basico | Se considera mas sencillo para el MVP |
| Ponderado | Permanece como alternativa a evaluar despues de integrar los Casos A, B y C |

---

## 14. Dudas Pendientes

| Codigo | Pregunta | Area |
|:------:|----------|------|
| **D-A01** | La capacidad se configurara unicamente por dia o tambien por franjas horarias? | Configuracion |
| **D-A02** | El modelo ponderado por tipo/tamano de producto formara parte del MVP? | Alcance |
| **D-A03** | El negocio configurara manualmente cuanto consume cada producto? | Configuracion |
| **D-A04** | Durante cuanto tiempo deberia reservarse capacidad mientras el cliente realiza el pago? | Concurrencia |
| **D-A05** | Que ocurre cuando dos clientes intentan adquirir simultaneamente la ultima capacidad disponible? | Concurrencia |
| **D-A06** | Como se determinara exactamente que porcentaje de capacidad se recupera cuando una produccion ya comenzo? | Recuperacion |
| **D-A07** | El sistema controlara tambien disponibilidad de ingredientes o el MVP se limitara al control de capacidad? | Alcance |
| **D-A08** | La capacidad debera manejar numeros enteros o podra trabajar con valores ponderados/decimales? | Configuracion |

---

## 15. Conclusion del Caso A

<blockquote>

El Caso A demuestra que el problema de una MYPE que trabaja bajo pedido **no puede modelarse unicamente mediante stock**.

</blockquote>

Una pasteleria puede disponer de ingredientes y productos, pero **no necesariamente** del tiempo, personal o recursos necesarios para aceptar otro pedido.

Por ello, el sistema necesita representar la capacidad como un **recurso limitado** que puede ser:

- **Configurado** por el negocio
- **Comprometido** por pedidos confirmados
- **Modificado temporalmente** en situaciones especiales
- **Recuperado** bajo determinadas condiciones

El Caso A no representa todavia el modelo definitivo del sistema.

Sus conclusiones deberan compararse con:

| Caso | Descripcion |
|------|-------------|
| **Caso B** | Dark Kitchen |
| **Caso C** | Capacidad logistica |

La integracion de los tres casos permitira construir el **Modelo de Capacidad v1**.

---

## 16. Estado

<div align="center">

| Estado | Detalle |
|:------:|---------|
| `CONSOLIDADO` | Incorporado a matriz de consolidacion y Modelo de Capacidad v1 |

Las reglas RN-A fueron incorporadas a las decisiones generales DP-G y al **Modelo de Capacidad v1**.

</div>