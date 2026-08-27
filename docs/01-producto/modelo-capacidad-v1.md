<div align="center">

# MODELO DE CAPACIDAD v1

## JaldiShop

`Producto` `Modelo` `Capacidad` `MVP`

</div>

---

## 1. Proposito

> Definir una primera version general del modelo de capacidad de JaldiShop a partir de los Casos A, B y C.

El objetivo es **evitar que una MYPE confirme mas pedidos** de los que realmente puede cumplir durante un periodo determinado.

---

## 2. Principio Central

<blockquote>

> JaldiShop no tratara la capacidad como sinonimo de inventario.

</blockquote>

Un negocio puede disponer de productos o insumos y, aun asi, **no tener tiempo, personal o disponibilidad operativa** para aceptar otro pedido.

> La capacidad representa **cuanto trabajo adicional** puede comprometer el negocio dentro de un periodo.

---

## 3. Componentes del Modelo

### 3.1 Capacidad Base

> Valor habitual que el negocio puede atender o producir.

---

### 3.2 Periodo

La capacidad se asigna a un periodo.

**Para el MVP se contemplan:**

| Tipo de Periodo | Descripcion |
|-----------------|-------------|
| **Dia** | Capacidad diaria completa |
| **Franja horaria** | Periodos mas pequeños (ej: 2 horas) |

---

### 3.3 Capacidad Efectiva

Es la capacidad aplicable realmente a un periodo despues de considerar una posible excepcion temporal.

```
capacidad_efectiva = capacidad_base o capacidad_excepcional
```

---

### 3.4 Capacidad Comprometida

> Representa la capacidad asociada a pedidos confirmados o reservas temporales vigentes.

---

### 3.5 Capacidad Disponible

```
capacidad_disponible = capacidad_efectiva - capacidad_comprometida
```

> Un pedido **no podra continuar** cuando la capacidad requerida sea superior a la disponible.

---

## 4. Excepciones Temporales

El negocio podra **aumentar, reducir o cerrar temporalmente** su capacidad para una fecha o franja especifica sin modificar su configuracion habitual.

| Tipo de Excepcion | Ejemplo |
|-------------------|---------|
| Incremento | Personal adicional |
| Reduccion | Ausencia de personal |
| Reduccion | Falla de equipo |
| Incremento | Fecha de alta demanda |
| Cierre | Cierre parcial |

> La excepcion tendra **prioridad** sobre la capacidad base durante su vigencia.

---

## 5. Ciclo de Capacidad de un Pedido

```
1. Cliente selecciona productos
          |
          v
2. Selecciona fecha y franja
          |
          v
3. Sistema consulta capacidad disponible
          |
          v
    +-----------+
    | Existe     |
    | capacidad? |
    +-----------+
     No     |     Si
      |     |      |
      v     v      v
  Bloquear  Continuar
  opcion    al checkout
                |
                v
4. Sistema crea reserva temporal
                |
                v
5. Cliente realiza pago
                |
                v
    +-----------+
    | Pago       |
    | confirmado?|
    +-----------+
     No     |     Si
      |     |      |
      v     v      v
  Liberar  Reserva
  reserva  pasa a
           comprometida
                |
                v
6. Cancelacion posterior
   evalua recuperacion
```

---

## 6. Concurrencia

<blockquote>

> El sistema debera impedir que dos clientes comprometan simultaneamente el ultimo cupo disponible.

</blockquote>

| Aspecto | Detalle |
|---------|---------|
| Regla | Perteneciente al dominio |
| Implementacion | Se definira durante arquitectura |

---

## 7. Cancelaciones

<blockquote>

> Cancelar un pedido **no implica necesariamente** recuperar toda la capacidad.

</blockquote>

La recuperacion dependera del estado operativo:

| Estado del Pedido | Recuperacion |
|-------------------|:------------:|
| Antes de iniciar preparacion | Completa |
| Preparacion iniciada | Parcial o no reutilizable |
| Entrega iniciada | Recurso logistico no se libera |

> Las reglas definitivas de recuperacion se detallaran al definir **estados y politicas de cancelacion**.

---

## 8. Delivery en el MVP

Cuando el negocio ofrezca delivery, el alcance inicial podra considerar:

<div align="center">

### Incluido en MVP

</div>

| Funcionalidad | Estado |
|---------------|:------:|
| Direccion del cliente | Incluir |
| Ubicacion del negocio | Incluir |
| Cobertura | Incluir |
| Distancia | Incluir |
| Costo basico de entrega | Incluir |
| Modalidad delivery/recojo | Incluir |
| Disponibilidad por franja | Incluir |

<div align="center">

### Fuera del MVP Inicial

</div>

| Funcionalidad | Estado |
|---------------|:------:|
| Optimizacion automatica de rutas | Excluir |
| Tracking GPS en vivo | Excluir |
| Asignacion inteligente de repartidores | Excluir |
| Prediccion de transito | Excluir |

---

## 9. Fuera del Modelo v1

Para evitar sobreingenieria en el primer MVP, se dejan como **evolucion**:

| Elemento | Tipo |
|----------|------|
| Pesos de capacidad por producto | Modelo avanzado |
| Capacidad simultanea por multiples recursos | Modelo avanzado |
| Estaciones internas de cocina | Recurso especifico |
| Busy Mode avanzado | Modo avanzado |
| ETA dinamico | Funcionalidad avanzada |
| Optimizacion logistica | Funcionalidad avanzada |

---

## 10. Relacion con los Entregables

<blockquote>

> Este modelo debe servir como base para el **modelado ER** y la **API** del primer entregable.

</blockquote>

No reemplaza las entidades obligatorias del proyecto:

| Entidad Obligatoria | Descripcion |
|---------------------|-------------|
| Usuarios | Gestion de usuarios |
| Productos | Catalogo de productos |
| Categorias | Organizacion de productos |
| Inventario | Control de stock |
| Carrito | Carrito de compras |
| Ordenes | Gestion de pedidos |
| Pagos | Procesamiento de pagos |

> JaldiShop agrega el **diferenciador**: la gestion de capacidad.

---

## 11. Estado

<div align="center">

| Version | Estado |
|:-------:|--------|
| `v1` | `APROBADO` por el equipo |

El siguiente paso es traducir el modelo a **requisitos de negocio** y comenzar el **modelo ER** del Entregable 1.

</div>
