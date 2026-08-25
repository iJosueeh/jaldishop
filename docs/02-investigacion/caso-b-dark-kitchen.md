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
