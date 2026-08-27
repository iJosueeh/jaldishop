<<<<<<< HEAD
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
=======
# Caso B: Capacidad de Dark Kitchen
## Documentación Funcional — Modelo Conceptual de Control de Capacidad Operativa

| Campo | Detalle |
| :--- | :--- |
| **Proyecto** | Plataforma e-commerce para MYPEs gastronómicas |
| **Alcance** | Modelo conceptual, no técnico, orientado a análisis de negocio y diseño funcional. |
| **Objetivo de la Tarjeta** | Descubrir cómo controlar capacidad cuando el negocio recibe pedidos concentrados en periodos cortos. |
| **Criterio de Terminado** | Modelo entendible del Caso B con ejemplos, reglas preliminares y decisiones acotadas. |

---

## 1. Funcionamiento Operativo Estándar

### 1.1 Flujo operativo: de la orden al despacho
Una Dark Kitchen (cocina oculta o virtual) opera sin sala de atención al público: todo pedido nace en un canal digital (app propia, marketplace de delivery o ambos) y termina en la puerta con un repartidor. El flujo estándar comprende seis etapas secuenciales:

1. **Recepción de la orden:** el pedido ingresa al sistema (propio o vía agregadores) y se valida contra el catálogo y el estado del local (abierto, con stock, dentro de horario).
2. **Confirmación y encolamiento:** la orden se acepta y se traslada a la cocina como una comanda, visualizada en un Kitchen Display System (KDS) o impresa/anunciada verbalmente.
3. **Preparación (estaciones de cocina):** el pedido se distribuye entre estaciones (fría, caliente, plancha, horno) según los productos que contiene. Aquí se consume el Tiempo de Procesamiento de Alimentos (*Food Processing Time* - FPT), la variable más incierta del proceso.
4. **Empaque:** los productos terminados confluyen en la mesa de empaque, donde se consolidan en un solo paquete por pedido.
5. **Despacho:** el pedido empacado se asigna a un repartidor (propio o de plataforma) y sale del local.
6. **Entrega y cierre:** el pedido se marca como entregado y se libera del sistema como orden activa.

> Este flujo es lineal para un pedido individual, pero en la práctica múltiples órdenes lo recorren en paralelo, compitiendo por los mismos recursos físicos y humanos en cada etapa.

### 1.2 Principal cuello de botella operativo
No existe un único cuello de botella fijo: el límite real es el eslabón más débil de la cadena en un momento dado, y puede desplazarse según el contexto:

* **Estaciones de cocina (capacidad física de cocción):** número de hornillas, planchas, hornos y mesas de trabajo, combinado con la dotación de personal (cocineros y asistentes).
* **Mesa de empaque:** el espacio físico para consolidar pedidos terminados es finito; si la cocina produce más rápido de lo que se empaca, el producto se acumula y pierde temperatura/calidad.
* **Área de despacho:** la acumulación de repartidores esperando su pedido genera congestión física y presión de tiempo.

> **Nota de diseño:** El sistema debe modelar la capacidad considerando las restricciones de las estaciones críticas y no asumir una capacidad infinita de despacho.

---

## 2. Medición y Dimensionamiento de la Capacidad

### 2.1 Cómo se mide la capacidad del sistema
La capacidad se mide como la cantidad máxima de unidades de producto (o de carga de preparación) que una cocina puede procesar en una ventana de tiempo determinada, sin degradar tiempos de entrega ni calidad:

$$\text{Capacidad consumida} = \sum (\text{demanda de platos del pedido}) \le \text{Capacidad máxima de producción de la cocina}$$

La unidad de medida operativa no es "el pedido" en abstracto, sino la suma de los productos/platos que contiene, ponderados por su complejidad de preparación.

### 2.2 Capacidad por día vs. capacidad por franja horaria

| Enfoque | ¿Qué mide? | Utilidad | Limitación |
| :--- | :--- | :--- | :--- |
| **Capacidad diaria** | Total de platos/pedidos que la cocina produce en toda la jornada. | Planificación de compras, insumos, personal fijo y proyección. | No refleja la demanda concentrada en horas pico puntuales. |
| **Capacidad por franja horaria (Ventana)** | Total de platos/pedidos en bloques cortos (ej. cada 15 o 30 min). | Detección y control de saturación en tiempo real, ajuste dinámico de ETA. | Requiere configuración operativa inicial por parte del comerciante. |

* **Recomendación funcional:** Operar con capacidad por franja horaria como unidad de control operativo en tiempo real y usar la capacidad diaria para analítica y planificación.

### 2.3 Ejemplo numérico de cálculo de capacidad operativa
Supongamos una Dark Kitchen con los siguientes recursos:
* **Cocina Caliente:** 2 estaciones calientes: 1 plato estándar cada 5 min $\rightarrow$ 12 platos/h por estación $\rightarrow$ 24 platos/h en total.
* **Cocina Fría:** 1 estación fría (ceviches/ensaladas): 1 plato cada 3 min $\rightarrow$ 20 platos/h.
* **Área de Empaque:** 1 pedido empacado cada 90 segundos $\rightarrow$ 40 pedidos/h.

Si se define una franja de control de 15 minutos:
* **Cocina Caliente:** $24 / 4 =$ **6 platos / 15 min**
* **Cocina Fría:** $20 / 4 =$ **5 platos / 15 min**
* **Empaque:** $40 / 4 =$ **10 pedidos / 15 min**

El límite real de la franja es el menor de los valores requeridos según la mezcla de pedidos entrantes. Si llegan pedidos concentrados en cocina caliente, el techo efectivo es 6 platos, aunque empaque tenga capacidad libre.

### 2.4 Complejidad: impacto de pedidos multi-producto o multi-unidad
Un pedido con 3 ceviches y 3 platos calientes no equivale a "una orden más"; equivale a 3 unidades de carga en cocina fría y 3 en cocina caliente. El sistema debe computar la carga acumulada por estación para no sobrepasar el umbral de ninguna línea de preparación.

---

## 3. Dinámica de Concurrencia y Saturación

### 3.1 Concurrencia: compras simultáneas en la misma franja
1. **Evaluación de Saldo:** La orden intenta reservar capacidad según sus líneas de producto al momento del checkout.
2. **Confirmación Inmediata:** Si hay saldo suficiente en las estaciones involucradas, la orden se confirma y se descuenta el saldo inmediatamente.
3. **Resolución Determinística:** Si dos órdenes compiten por el último saldo, se resuelve por *timestamp* de confirmación de pago, evitando sobreventa.
4. **Desplazamiento Asistido:** Si no hay saldo en la franja actual, el sistema ofrece una franja posterior o ajusta el ETA antes de rechazar.

### 3.2 Estado de saturación: medidas al alcanzar la capacidad máxima

| Nivel | Mecanismo | Efecto Operativo y en Cliente |
| :--- | :--- | :--- |
| **1 — Alerta Temprana** | Ajuste Dinámico de ETA (*Busy Mode*) | Suma minutos adicionales al tiempo de preparación; el ETA visible para cliente y repartidor se recalcula automáticamente. |
| **2 — Restricción Parcial** | *Throttle* de nuevas franjas | Deja de ofrecer la franja saturada y desplaza los nuevos pedidos a ventanas posteriores disponibles. |
| **3 — Bloqueo Temporal** | Pausa de recepción (*Freeze*) | Detiene el ingreso de órdenes nuevas durante un intervalo (ej. 15 min) para evacuar la cola de cocina. |
| **4 — Contención en Despacho** | Límite de órdenes por ruta | Limita a un máximo de 3 pedidos por repartidor/ruta para evitar enfriamiento y demoras en trayecto. |

### 3.3 Ajustes dinámicos de capacidad
* **Reducción temporal:** Por ausencia de personal, falla de equipo (ej. horno averiado) o corte eléctrico $\rightarrow$ se reduce manualmente el techo de la estación o local.
* **Aumento temporal:** Por personal de refuerzo en horas punta o apertura de mesa auxiliar $\rightarrow$ se incrementa el techo de capacidad puntualmente.
* **Registro de auditoría:** Todo ajuste dinámico debe quedar registrado con motivo breve para trazabilidad.

---

## 4. Ciclo de Vida del Pedido y Gestión de Capacidad

### 4.1 Momento en que la capacidad se consume y se libera
* **Consumo (Bloqueo):** Se reserva en el momento en que el pedido es confirmado y pagado (orden aceptada), evitando bloqueos por carritos abandonados.
* **Liberación parcial por estación:** Cuando una estación termina su plato, libera su capacidad interna, aunque el pedido siga en empaque.
* **Liberación total:** Al marcarse el pedido como despachado/entregado, se libera totalmente del pool de órdenes activas.

### 4.2 Gestión de cancelaciones
* **Cancelación ANTES de iniciar preparación:** La capacidad reservada se libera al 100% de inmediato sin merma de insumos.
* **Cancelación DURANTE la preparación:** Se libera el saldo lógico para nuevos pedidos, pero se registra el tiempo de estación e insumos incurridos como pérdida operativa (*Cancelado en curso*).

---

## 5. Configuración del Comerciante y Reglas de Negocio

### 5.1 Parámetros configurables por el comerciante
* **Capacidad por estación:** Número de estaciones activas y rendimiento estimado (unidades/tiempo).
* **Capacidad por franja horaria:** Duración de la franja (15, 30 o 60 min) y techo de pedidos permitidos.
* **Dotación de personal por turno:** Personal en cocina, asistentes y empaque para ponderar el ritmo.
* **Umbrales de Busy Mode:** % de ocupación (ej. 80%) a partir del cual se añaden minutos al ETA.
* **Umbral de pausa automática:** % de ocupación (ej. 95-100%) para pausar temporalmente nuevos ingresos.
* **Duración por defecto de pausa:** Intervalo estándar sugerido (ej. 15 minutos).
* **Límite de pedidos por repartidor:** Tope de órdenes en simultáneo por ruta (por defecto 3).
* **Prioridad de canales:** Regla de distribución de saldo entre app propia y marketplaces.

### 5.2 Reglas de Negocio Preliminares (SI... ENTONCES...)
* **SI** la ocupación de una estación alcanza el umbral de *Busy Mode* (ej. 80%), **ENTONCES** el sistema suma minutos adicionales al tiempo de preparación y actualiza el ETA visible.
* **SI** el volumen de pedidos activos alcanza el umbral crítico (ej. 95-100%), **ENTONCES** el sistema activa/sugiere una pausa temporal de recepción de órdenes.
* **SI** una ruta de despacho alcanza 3 pedidos asignados, **ENTONCES** el sistema bloquea nuevas asignaciones a esa ruta y busca otro repartidor/ruta.
* **SI** el sistema detecta retraso reiterado en el tiempo de preparación de una estación, **ENTONCES** aplica una recalibración preventiva del ETA para pedidos subsiguientes.
* **SI** se detecta una falla técnica o corte de conexión con el TPV, **ENTONCES** envía alerta al administrador y activa protocolo de contingencia/modo offline.
* **SI** un pedido es cancelado antes de iniciar preparación, **ENTONCES** libera de inmediato el 100% de la capacidad reservada.
* **SI** un pedido es cancelado durante la preparación, **ENTONCES** libera el saldo de capacidad lógica pero registra los costos e insumos como pérdida operativa.
* **SI** un pedido contiene múltiples platos de distintas estaciones, **ENTONCES** descuenta la carga correspondiente en cada estación de forma independiente.
* **SI** dos órdenes compiten por el último saldo disponible en la franja, **ENTONCES** se resuelve por el *timestamp* de pago confirmado.

---

## 6. Dudas y Decisiones Clave Acotadas (Foco MVP MYPE)

1. **Nivel de Granularidad de Capacidad (MVP vs. Fase 2):** ¿Se debe implementar el control por platos y estaciones independientes desde el día 1, o empezar con un límite simplificado de "número total de pedidos por franja" (ej. máx 8 pedidos cada 30 min) para mantener la usabilidad amigable en una microempresa?
2. **Tamaño de la Franja Horaria por Defecto:** ¿Es más manejable para un negocio pequeño operar con franjas de 30 minutos (menor fricción de configuración) o franjas de 15 minutos (mayor precisión pero requiere alta disciplina de actualización)?
3. **Nivel de Automatización del Busy Mode y Pausa:** ¿El sistema debe activar el Busy Mode y las pausas automáticamente al cruzar el umbral del 80%/95%, o debe enviar una notificación push recomendando al encargado pulsar un botón de confirmación?
4. **Reparto de Capacidad entre Canales:** Si el negocio vende por su app propia y marketplaces externos, ¿la capacidad se administra en un único *pool compartido* donde quien llega primero consume saldo, o se reservan cupos exclusivos por canal?
>>>>>>> 5853d36ba001f9da87346c7fc907f00696afa753
