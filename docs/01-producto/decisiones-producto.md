<div align="center">

# DECISIONES DE PRODUCTO

## JaldiShop

`Producto` `Decisiones` `MVP`

</div>

---

## Proposito

> Registrar las decisiones propuestas despues de consolidar los Casos A, B y C y contrastarlas con el alcance academico de los entregables.

---

## DP-G01 — Modelo Simple de Capacidad

<blockquote>

**Decision propuesta:** Utilizar capacidad simple en el MVP.

</blockquote>

El consumo ponderado por producto o complejidad queda como evolucion posterior.

**Motivo:** Permite demostrar el diferenciador de JaldiShop sin convertir el MVP en un motor complejo de planificacion.

---

## DP-G02 — Capacidad Operativa Principal

<blockquote>

**Decision propuesta:** Manejar una capacidad operativa principal por configuracion.

</blockquote>

Multiples recursos simultaneos como horno, cocina, empaque o repartidores quedan para una version posterior.

---

## DP-G03 — Dia y Franja Horaria

<blockquote>

**Decision propuesta:** Permitir capacidad diaria y por franja horaria.

</blockquote>

Esto cubre negocios con produccion anticipada y negocios con demanda concentrada durante determinadas horas.

---

## DP-G04 — Reserva Temporal Durante Checkout

<blockquote>

**Decision propuesta:** Reservar temporalmente la capacidad cuando el cliente entra al proceso de checkout.

</blockquote>

| Evento | Accion |
|--------|--------|
| Pago confirmado | Reserva se confirma |
| Pago falla | Reserva se libera |
| Reserva expira | Reserva se libera |

> El tiempo exacto de expiracion queda pendiente de definicion tecnica.

---

## DP-G05 — Proteccion Ante Concurrencia

<blockquote>

**Decision propuesta:** El backend debera garantizar que dos pedidos no puedan comprometer el mismo ultimo cupo.

</blockquote>

> La solucion tecnica se definira durante arquitectura.

---

## DP-G06 — Alcance Logistico

<blockquote>

**Decision propuesta:** Incluir unicamente logistica basica en el MVP.

</blockquote>

| Funcionalidad | Estado |
|---------------|:------:|
| Ubicacion | Incluir |
| Cobertura | Incluir |
| Distancia | Incluir |
| Delivery/recojo | Incluir |
| Costo basico | Incluir |
| Disponibilidad por franja | Incluir |
| Optimizacion de rutas | Excluir |
| Tracking GPS | Excluir |
| Asignacion inteligente | Excluir |

---

## Pendientes Despues de Aprobar Estas Decisiones

| Pendiente | Descripcion |
|-----------|-------------|
| Estados del pedido | Definir estados definitivos |
| Politica de cancelacion | Definir reglas exactas |
| Duracion de reservas | Definir tiempo de expiracion |
| Proveedor de pagos | Seleccionar integracion |
| Proveedor de mapas | Seleccionar integracion |
| Modelo ER | Traducir modelo a requisitos |

---

## Estado

<div align="center">

| Estado | Detalle |
|:------:|---------|
| `APROBADO` | Incorporado al Modelo de Capacidad v1 |

Las decisiones DP-G01 a DP-G06 fueron aprobadas y formalizadas en el **Modelo de Capacidad v1**.

</div>
