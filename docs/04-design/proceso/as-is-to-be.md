# Análisis AS-IS y TO-BE

## 1. Objetivo

Representar cómo una MYPE gestiona actualmente sus pedidos y su
capacidad de atención o producción, y cómo este proceso cambiaría
mediante la plataforma propuesta.

El problema central identificado es que las MYPE pueden recibir
pedidos mediante diferentes canales y gestionarlos manualmente,
sin disponer de un mecanismo que determine automáticamente si
todavía existe capacidad real para aceptar un nuevo pedido.

La propuesta busca centralizar la gestión de pedidos y controlar
la capacidad disponible.

---

## 2. Proceso AS-IS

El proceso AS-IS representa la situación actual del negocio antes
de utilizar la plataforma.

Actualmente, los pedidos pueden llegar mediante diferentes canales,
como WhatsApp, Instagram u otros medios.

El negocio debe revisar manualmente la solicitud, comprobar su
disponibilidad, coordinar el pago, registrar el pedido y comunicar
posteriormente su estado al cliente.

### Flujo AS-IS

```mermaid
flowchart TD
    A[Cliente conoce el negocio] --> B[Cliente contacta al negocio]
    B --> C{Canal de contacto}

    C -->|WhatsApp| D[Cliente envía consulta]
    C -->|Instagram| D
    C -->|Redes sociales / otro canal| D

    D --> E[Encargado responde manualmente]
    E --> F[Cliente indica lo que necesita]
    F --> G[Cliente indica cantidad, fecha y preferencias]

    G --> H{¿Puede el negocio cumplir el pedido?}

    H -->|No está seguro| I[Revisa inventario, agenda, libreta, Excel u otros registros]
    I --> J[Calcula manualmente cuánto puede aceptar]
    J --> K{¿Existe capacidad?}

    K -->|No| L[Informa que no puede aceptar el pedido]
    K -->|Sí| M[Informa disponibilidad al cliente]

    H -->|Sí| M

    M --> N[Cliente acepta las condiciones]
    N --> O[Cliente realiza pago o adelanto]
    O --> P[Cliente envía comprobante]
    P --> Q[Encargado verifica manualmente el pago]

    Q --> R{¿Pago confirmado?}
    R -->|No| S[Solicita información o espera confirmación]
    R -->|Sí| T[Encargado confirma el pedido]

    T --> U[Registra manualmente el pedido]
    U --> V[Organiza manualmente los pedidos]
    V --> W[Se prepara o procesa el pedido]
    W --> X[Se coordina entrega o recojo]

    X --> Y{Modalidad}
    Y -->|Recojo| Z[Cliente recoge el pedido]
    Y -->|Entrega| AA[Negocio coordina la entrega]

    Z --> AB[Pedido finalizado]
    AA --> AB
```

---

## 3. Problemas identificados en el AS-IS

El análisis del proceso actual permite identificar que la
información se encuentra dispersa y que gran parte de las
operaciones dependen de verificaciones manuales.

```mermaid
flowchart TD
    A[La MYPE recibe pedidos por diferentes canales]

    A --> B[WhatsApp]
    A --> C[Instagram / Redes sociales]
    A --> D[Otros medios]

    B --> E[Información de pedidos dispersa]
    C --> E
    D --> E

    E --> F[El encargado revisa manualmente los pedidos]
    F --> G[Control manual de disponibilidad]
    G --> H[No existe validación automática de capacidad]

    H --> I{Riesgos}

    I --> J[Olvidar pedidos pendientes]
    I --> K[Calcular incorrectamente la capacidad]
    I --> L[Duplicar o perder información]
    I --> M[Aceptar más pedidos de los que puede cumplir]
    I --> N[Demoras en responder al cliente]

    J --> O[Desorganización operativa]
    K --> O
    L --> O

    M --> P[Incumplimiento o retrasos]
    N --> Q[Mala experiencia del cliente]

    O --> R[Problemas en la gestión de pedidos]
    P --> R
    Q --> R
```

---

## 4. Proceso TO-BE

El proceso TO-BE representa cómo funcionaría la operación
utilizando la plataforma propuesta.

El objetivo es reducir las verificaciones manuales y permitir que
el sistema controle automáticamente la capacidad disponible antes
de confirmar un pedido.

### Flujo TO-BE

```mermaid
flowchart TD
    A[Cliente ingresa a la plataforma]
    A --> B[Cliente consulta catálogo de productos o servicios]
    B --> C[Cliente selecciona productos o servicios]
    C --> D[Cliente indica cantidad]
    D --> E[Cliente selecciona fecha requerida]

    E --> F[El sistema calcula la capacidad requerida]
    F --> G[El sistema consulta la capacidad disponible]

    G --> H{¿Existe capacidad disponible?}

    H -->|No| I[Plataforma muestra que no existe capacidad]
    I --> J[Plataforma muestra otras fechas o alternativas]
    J --> K{¿Cliente selecciona una alternativa?}

    K -->|Sí| E
    K -->|No| L[Pedido no continúa]

    H -->|Sí| M[Cliente procede al pago]
    M --> N[Plataforma procesa o registra el pago]

    N --> O{¿Pago confirmado?}

    O -->|No| P[Pedido queda pendiente o no se confirma]
    O -->|Sí| Q[Plataforma revalida capacidad disponible]

    Q --> R{¿Capacidad continúa disponible?}

    R -->|No| S[Plataforma informa conflicto de capacidad]
    R -->|Sí| T[Plataforma confirma el pedido]

    T --> U[Pedido se registra automáticamente]
    U --> V[Capacidad queda comprometida]
    V --> W[Negocio visualiza el pedido en su panel]

    W --> X[Negocio actualiza estado del pedido]

    X --> Y{Estado}

    Y -->|En preparación| Z[Cliente visualiza pedido en preparación]
    Y -->|Listo| AA[Cliente recibe aviso de pedido listo]
    Y -->|Entregado / Recogido| AB[Pedido finalizado]
```

> **Nota:** La forma exacta de reservar y revalidar capacidad
> durante el pago todavía se encuentra pendiente de definición.

---

## 5. Comparación AS-IS → TO-BE

| AS-IS | Transformación | TO-BE |
|---|---|---|
| Cliente contacta por diferentes canales | Centralización | Cliente utiliza una plataforma centralizada |
| Consulta manual | Digitalización | Consulta productos o servicios |
| Negocio revisa disponibilidad manualmente | Automatización | Sistema valida disponibilidad |
| Capacidad calculada manualmente | Control automático | Sistema calcula y controla capacidad |
| Pago verificado manualmente | Integración | Pago registrado o confirmado en el sistema |
| Pedido registrado en diferentes medios | Registro centralizado | Pedido registrado automáticamente |
| Estados comunicados manualmente | Gestión de estados | Estados actualizados en la plataforma |
| Cliente pregunta por seguimiento | Autoseguimiento | Cliente realiza seguimiento del pedido |

---

## 6. Qué gestiona la plataforma

A partir del flujo TO-BE se identifican inicialmente las siguientes
responsabilidades:

- Catálogo de productos o servicios.
- Gestión de pedidos.
- Control de capacidad.
- Validación de disponibilidad.
- Pagos.
- Registro centralizado de pedidos.
- Estados del pedido.
- Seguimiento del pedido por parte del cliente.

Estas responsabilidades todavía deberán contrastarse con el
alcance definitivo del MVP.

---

## 7. Relación con el modelo de capacidad

El análisis AS-IS/TO-BE identifica **dónde interviene la capacidad
dentro del proceso general**.

Los Casos A, B y C tienen otro objetivo: determinar **cómo debe
funcionar internamente esa capacidad**.

Por lo tanto:

AS-IS / TO-BE
→ Define el proceso.

Casos A + B + C
→ Definen el comportamiento de la capacidad.

Ambos análisis serán utilizados posteriormente para construir el
Modelo de Capacidad v1 y los requisitos del sistema.

---

## 8. Punto pendiente de revisión

La propuesta original considera que las MYPE ya captan clientes
principalmente mediante redes sociales.

Debe definirse posteriormente hasta qué punto el MVP:

1. Centralizará la compra directamente dentro de la plataforma.
2. Recibirá pedidos provenientes de canales externos.
3. Permitirá una combinación de ambos enfoques.

Esta decisión afectará el flujo TO-BE definitivo.

---

## 9. Estado

**Estado: análisis inicial completado.**

Los diagramas AS-IS y TO-BE representan el proceso planteado
actualmente y podrán modificarse después de definir el Modelo de
Capacidad v1 y el alcance del MVP.