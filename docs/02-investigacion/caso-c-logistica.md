<div align="center">

# CASO C

## Capacidad Logistica y Delivery

`Investigacion` `Modelo de Capacidad` `Logistica`

</div>

---

## 1. Objetivo

> Analizar como deberia funcionar el **modelo de capacidad** en negocios donde aceptar un pedido no depende unicamente de la capacidad de produccion, sino tambien de la capacidad disponible para realizar la entrega.

Este caso busca determinar como el sistema deberia gestionar pedidos que requieren **delivery** considerando:

- Destinos
- Franjas horarias
- Cobertura
- Distancia
- Disponibilidad logistica

---

## 2. Contexto del Caso

<blockquote>

**Situacion:** El Caso C parte de negocios que trabajan bajo pedido y realizan entregas a domicilio.

</blockquote>

Un ejemplo representativo es un emprendimiento de desayunos, regalos personalizados o productos preparados que puede disponer de **capacidad suficiente para producir** un pedido, pero tener **limitaciones para entregarlo** en el horario solicitado.

**Ejemplo practico:**

```
El negocio puede producir:        10 pedidos
Pedidos terminados a tiempo:     10 pedidos
Capacidad de entrega 7-8am:       4 entregas
                                   --------
Resultado:                         6 pedidos sin entregar a tiempo
```

> **Conclusion clave:** La produccion **no es el unico** recurso limitado.

---

## 3. Problema Identificado

El negocio puede aceptar pedidos que **productivamente** puede realizar, pero que **logicamente** no puede entregar dentro de la fecha o franja comprometida.

Se identifican inicialmente **dos dimensiones**:

| Dimension | Descripcion |
|-----------|-------------|
| **Capacidad productiva** | Cuanto puede producir o preparar el negocio |
| **Capacidad logistica** | Cuantas operaciones de entrega puede asumir el negocio |

> Existe capacidad para producir el pedido, pero **no existe capacidad** para entregarlo a las 8:00 a.m.

---

## 4. Separacion de Capacidades

Para el Caso C se plantea gestionar inicialmente la capacidad productiva y logistica de **forma independiente**.

**Ejemplo practico:**

```
Estado inicial:
  Capacidad productiva disponible:    8 unidades
  Capacidad logistica 7:00-8:00:      2 cupos

Cliente solicita:
  - 3 desayunos
  - Una unica direccion
  - Delivery entre 7:00 y 8:00

Consumo:
  - Capacidad productiva:  3 unidades
  - Capacidad logistica:   1 cupo

Estado final:
  Capacidad productiva disponible:    5 unidades
  Capacidad logistica disponible:     1 cupo
```

> **Nota:** Esta separacion debera contrastarse con los Casos A y B antes de convertirse en parte del modelo general.

---

## 5. Capacidad por Destino

La cantidad de productos **no deberia equivaler necesariamente** a la cantidad de operaciones logisticas.

### Mismo Destino

| Producto | Entrega |
|----------|:-------:|
| Desayuno A | Direccion X |
| Desayuno B | Direccion X |
| Desayuno C | Direccion X |

```
Produccion: 3 unidades
Logistica:  1 cupo (misma direccion)
```

### Multiples Destinos

| Producto | Entrega |
|----------|:-------:|
| Desayuno A | Direccion 1 |
| Desayuno B | Direccion 2 |
| Desayuno C | Direccion 3 |

```
Produccion: 3 unidades
Logistica:  3 cupos (diferentes direcciones)
```

> **Regla:** El destino se utiliza como **unidad basica** para representar una operacion logistica.

---

## 6. Recojo en Establecimiento

Cuando el cliente selecciona **recojo en el establecimiento**, el pedido no requiere un recurso de delivery.

```
Pedido: 3 productos
Modalidad: Recojo

Consumo:
  Capacidad productiva:           3 unidades
  Capacidad logistica delivery:   0 cupos
```

> El pedido continua estando limitado por la **capacidad de produccion**.

---

## 7. Capacidad Logistica Base

El negocio podra definir una **capacidad logistica habitual**.

| Franja | Capacidad |
|--------|:---------:|
| 07:00 - 08:00 | 4 entregas |
| 08:00 - 09:00 | 6 entregas |
| 09:00 - 10:00 | 8 entregas |

> Esta capacidad representa la operacion normal del negocio.
>
> El objetivo es impedir que se continuen aceptando entregas cuando el periodo ya se encuentra **saturado**.

---

## 8. Excepciones Temporales

La capacidad base podra modificarse temporalmente **sin alterar** la configuracion habitual.

<details>
<summary><strong>Clic para ver ejemplos de excepciones</strong></summary>

| Tipo | Capacidad Habitual | Capacidad Excepcional | Motivo |
|------|:------------------:|:---------------------:|--------|
| Aumento | 4 | 8 | Fecha especial con recursos adicionales |
| Reduccion | 6 | 2 | Disponibilidad reducida de personal |

</details>

**Ejemplo:**

```
Capacidad habitual sabado 7:00-8:00:   4 entregas

Fecha especial (excepcion):            8 entregas

Efecto: Solo afecta esa fecha especifica
        La capacidad base de los demas sabados NO cambia
```

> Durante el periodo de la excepcion, esta tendra **prioridad** sobre la configuracion base.

---

## 9. Cobertura y Ubicacion

Cuando un pedido utiliza delivery, el sistema debera comprobar si la direccion del cliente pertenece a la **cobertura definida** por el negocio.

| Distancia del Cliente | Cobertura: 10 km | Resultado |
|:---------------------:|:----------------:|:---------:|
| 6.4 km | Dentro | Delivery disponible |
| 13 km | Fuera | Delivery **no disponible** |

> El cliente podra seleccionar otra direccion valida o utilizar **recojo en establecimiento** cuando este disponible.

---

## 10. Uso de Maps

La integracion con un servicio de mapas tendria como proposito inicial:

| Funcion | Descripcion |
|---------|-------------|
| Ubicacion del cliente | Obtener o seleccionar la ubicacion |
| Ubicacion del negocio | Identificar la ubicacion del negocio |
| Distancia | Calcular o consultar distancia |
| Cobertura | Validar cobertura de delivery |
| Costo | Apoyar el calculo del costo de entrega |

> **Nota:** No se plantea inicialmente desarrollar un sistema propio de **optimizacion automatica de rutas**.

---

## 11. Costo de Delivery

El negocio podria establecer diferentes costos de delivery segun su configuracion.

| Distancia | Costo |
|-----------|:-----:|
| 0 - 3 km | S/ 5 |
| 3 - 6 km | S/ 8 |
| 6 - 10 km | S/ 12 |

<details>
<summary><strong>Formas de calculo del costo</strong></summary>

- Distancia
- Zonas
- Tarifa fija
- Una combinacion de los anteriores

</details>

> **Importante:** Encontrarse dentro de la cobertura **no significa** que exista capacidad logistica disponible para la franja solicitada.

---

## 12. Flujo Completo del Caso C

### Flujo Principal

| Paso | Descripcion |
|:----:|-------------|
| 1 | El cliente selecciona los productos y cantidades |
| 2 | Selecciona una fecha |
| 3 | El sistema verifica la capacidad productiva |
| 4 | Si no existe capacidad suficiente, se informa la indisponibilidad |
| 5 | Si existe capacidad, el cliente selecciona la modalidad de entrega |

### Si Selecciona Recojo

| Paso | Descripcion |
|:----:|-------------|
| 6 | No se consume capacidad logistica de delivery |
| 7 | El cliente continua al resumen y pago |

### Si Selecciona Delivery

| Paso | Descripcion |
|:----:|-------------|
| 6 | El cliente registra o selecciona una direccion |
| 7 | El sistema obtiene la ubicacion |
| 8 | Se valida si pertenece a la cobertura del negocio |
| 9 | Si esta fuera de cobertura, no se permite continuar con delivery |
| 10 | Si esta dentro, se determina la distancia |
| 11 | Se determina el costo de delivery segun la configuracion |
| 12 | Se muestran las franjas disponibles |
| 13 | El cliente selecciona una franja |
| 14 | El sistema comprueba la capacidad logistica |
| 15 | Si la franja esta llena, se muestran otras alternativas |
| 16 | Si existe capacidad, el cliente continua |

### Confirmacion

| Paso | Descripcion |
|:----:|-------------|
| 17 | Se presenta el resumen del pedido |
| 18 | El cliente realiza el pago |
| 19 | El sistema confirma el resultado del pago |
| 20 | Antes de confirmar definitivamente, se revalida la capacidad |
| 21 | Si continua disponible, se confirma el pedido |
| 22 | Se compromete la capacidad correspondiente |
| 23 | El negocio inicia la preparacion |

### Finalizacion

| Modalidad | Pasos |
|-----------|-------|
| **Recojo** | Pedido listo -> Cliente recoge -> Finalizado |
| **Delivery** | Pedido listo -> Entrega asignada -> En ruta -> Entregado -> Finalizado |

---

## 13. Flujo Resumido

```
+-------------------------------------+
|   Cliente selecciona productos      |
+------------------+------------------+
                   |
                   v
+-------------------------------------+
|   Selecciona fecha                  |
+------------------+------------------+
                   |
                   v
           +-------+-------+
           | Capacidad     |
           | productiva?   |
           +-------+-------+
            No     |     Si
   +-----------+   |   +-----------+
   | No        |   |   | Modalidad |
   | disponible|   |   +-----+-----+
   +-----------+   |         |
                   |    +----+----+
                   |    |         |
                   v    v         v
              +--------+  +------------+
              | Recojo  |  | Delivery   |
              +----+----+  +-----+------+
                   |             |
                   |             v
                   |      +------+------+
                   |      | Direccion   |
                   |      +------+------+
                   |             |
                   |             v
                   |      +------+------+
                   |      | Cobertura?  |
                   |      +------+------+
                   |        Si     |    No
                   |   +----------+ +-----------+
                   |   |           | | No        |
                   |   v           | | disponible|
                   | Distancia     | +-----------+
                   |   |           |
                   |   v           |
                   | Costo         |
                   |   |           |
                   |   v           |
                   | Franja        |
                   |   |           |
                   |   v           |
                   | Capacidad     |
                   | logistica?    |
                   |  Si    |   No |
                   |   |    |   +--+
                   |   |    +-> Otra
                   |   |         franja
                   +---+---------+
                       |
                       v
                 +-----+-----+
                 |  Resumen   |
                 +-----+-----+
                       |
                       v
                 +-----+-----+
                 |    Pago    |
                 +-----+-----+
                       |
                       v
              +--------+--------+
              | Revalidar       |
              | capacidad       |
              +--------+--------+
                       |
                       v
              +--------+--------+
              | Confirmar       |
              | pedido          |
              +--------+--------+
                       |
                       v
              +--------+--------+
              | Preparacion     |
              +--------+--------+
                       |
                 +-----+-----+
                 |           |
                 v           v
            +--------+  +--------+
            | Recojo |  | Delivery|
            +---+----+  +---+----+
                |           |
                v           v
            +--------+  +--------+
            |Recogido|  |En ruta |
            +---+----+  +---+----+
                |           |
                |           v
                |        +--------+
                |        |Entregado|
                |        +---+----+
                |            |
                +-----+------+
                      |
                      v
                +-----+-----+
                | Finalizado |
                +------------+
```

---

## 14. Cancelaciones y Recuperacion de Capacidad

La recuperacion de capacidad productiva y logistica debera analizarse de **forma independiente**.

| Momento | Capacidad Productiva | Capacidad Logistica |
|---------|:--------------------:|:-------------------:|
| **Antes del despacho** | Depende del avance de preparacion | Cupo puede liberarse |
| **Preparado, sin despacho** | No se recupera necesariamente | Cupo puede liberarse |
| **Delivery en ruta** | No se recupera necesariamente | **No se libera** automaticamente |

> **Razon:** El recurso logistico ya comenzo a utilizarse.

---

## 15. Reglas Preliminares RN-C

| Codigo | Regla |
|:------:|-------|
| **RN-C01** | El sistema gestionara de forma independiente la capacidad productiva y la capacidad logistica |
| **RN-C02** | Un pedido con delivery solo podra confirmarse cuando exista capacidad productiva suficiente y capacidad logistica disponible para la fecha y franja seleccionadas |
| **RN-C03** | Varias unidades enviadas conjuntamente a una misma direccion consumiran un solo cupo logistico |
| **RN-C04** | Cada destino diferente dentro de un mismo pedido sera considerado una operacion logistica independiente y consumira su correspondiente cupo |
| **RN-C05** | Los pedidos con modalidad de recojo en establecimiento no consumiran capacidad logistica de delivery |
| **RN-C06** | El negocio podra configurar una capacidad logistica base por fecha, dia y/o franja horaria |
| **RN-C07** | El negocio podra aumentar, reducir o cerrar temporalmente la capacidad mediante una excepcion. Mientras la excepcion este vigente, tendra prioridad sobre la capacidad base |
| **RN-C08** | El negocio podra establecer la cobertura permitida para delivery. Una direccion fuera de la cobertura no podra utilizar dicha modalidad |
| **RN-C09** | El sistema utilizara la ubicacion del establecimiento y del destino para determinar la distancia y validar la cobertura |
| **RN-C10** | El costo de delivery podra establecerse segun zona o rango de distancia configurado por el negocio |
| **RN-C11** | Una cancelacion realizada antes de iniciar el despacho liberara el cupo logistico comprometido |
| **RN-C12** | Una cancelacion cuando el despacho ya se encuentre en ruta no liberara automaticamente el cupo logistico |

---

## 16. Decisiones Preliminares

### DP-C01 — Consumo por Destino

> Para el modelo inicial del Caso C se considera: **1 destino = 1 cupo logistico**.

La posibilidad de que un destino consuma diferente capacidad segun distancia o tiempo sera evaluada durante la integracion de los Casos A, B y C.

### DP-C02 — Optimizacion de Rutas

> La optimizacion automatica de rutas **no se considera** inicialmente parte del MVP.

| Funcion de Maps | Estado |
|-----------------|--------|
| Ubicacion | Incluir |
| Cobertura | Incluir |
| Distancia | Incluir |
| Apoyo al costo | Incluir |
| Optimizacion de rutas | **Excluir** del MVP |

---

## 17. Dudas Pendientes

| Codigo | Pregunta | Area |
|:------:|----------|------|
| **D-C01** | El control de ingredientes e insumos formara parte del MVP o el sistema se limitara al control de capacidad? | Alcance |
| **D-C02** | La capacidad productiva y logistica seran recursos independientes en el modelo general de capacidad? | Modelo |
| **D-C03** | Un destino siempre consumira un cupo logistico o podra consumir mas capacidad segun distancia o tiempo? | Modelo |
| **D-C04** | Como se representaran multiples motorizados? (cantidad de cupos o gestion individual) | Modelo |
| **D-C05** | La capacidad logistica se medira mediante numero de entregas, tiempo disponible o una combinacion? | Medicion |
| **D-C06** | Como se calculara definitivamente el costo de delivery? | Costos |
| **D-C07** | Que ocurre cuando dos clientes intentan adquirir simultaneamente el ultimo cupo disponible? | Concurrencia |
| **D-C08** | Se reservara temporalmente capacidad mientras un cliente realiza el pago? | Concurrencia |
| **D-C09** | Que ocurre si el pago se procesa correctamente pero al revalidar la capacidad ya no se encuentra disponible? | Concurrencia |
| **D-C10** | Que proveedor de Maps utilizara el proyecto? | Integracion |
| **D-C11** | Que proveedor de pagos utilizara el proyecto? | Integracion |
| **D-C12** | Que consecuencias tendra una cancelacion cuando la produccion ya se encuentra avanzada? | Cancelaciones |
| **D-C13** | El negocio podra modificar una entrega confirmada (direccion, franja, modalidad)? | Modificaciones |
| **D-C14** | Hasta que momento podra el cliente modificar o cancelar un pedido sin intervencion del negocio? | Modificaciones |
| **D-C15** | Se confirma que la optimizacion automatica de rutas permanecera fuera del MVP? | Alcance |

---

## 18. Aspectos a Revisar por el Equipo

<blockquote>

Durante la revision del Caso C se solicita evaluar principalmente:

</blockquote>

| # | Aspecto a Evaluar |
|:-:|-------------------|
| 1 | Si la separacion entre capacidad productiva y logistica tiene sentido para el modelo general |
| 2 | Si el criterio inicial de un destino igual a un cupo logistico es suficientemente representativo |
| 3 | Si la capacidad logistica deberia depender unicamente de cantidad de entregas o tambien del tiempo y distancia |
| 4 | Como deberian representarse multiples recursos de delivery |
| 5 | Si las reglas RN-C contienen comportamientos innecesarios o faltantes |
| 6 | Que elementos deben permanecer especificos del Caso C y cuales deberian convertirse en reglas generales |

---

## 19. Conclusion del Caso C

<blockquote>

El Caso C evidencia que la capacidad de un negocio puede depender de **mas de un recurso**.

</blockquote>

Un negocio puede disponer de tiempo y recursos para producir un pedido, pero **no necesariamente** tener disponibilidad logistica para entregarlo en la ubicacion y horario solicitado.

Por ello, el analisis propone inicialmente **separar** la capacidad productiva de la capacidad logistica.

La capacidad logistica puede depender de:

| Factor | Descripcion |
|--------|-------------|
| **Destinos** | Diferentes direcciones de entrega |
| **Franjas horarias** | Periodos de tiempo disponibles |
| **Cobertura** | Zona geografica atendida |
| **Disponibilidad temporal** | Excepciones y variaciones |
| **Modalidad de entrega** | Delivery o recojo |

Estas conclusiones todavia no representan el modelo definitivo del sistema.

Deberan contrastarse con:

| Caso | Descripcion |
|------|-------------|
| **Caso A** | Pasteleria |
| **Caso B** | Dark Kitchen |

La integracion de los tres casos permitira determinar que conceptos son generales y construir el **Modelo de Capacidad v1**.

---

## 20. Estado

<div align="center">

| Estado | Detalle |
|:------:|---------|
| `CONSOLIDADO` | Incorporado a matriz de consolidacion y Modelo de Capacidad v1 |

Las reglas RN-C fueron incorporadas a las decisiones generales DP-G y al **Modelo de Capacidad v1**.

</div>