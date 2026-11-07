# Fase 2B.2: investigación manual de Orders

Contrato: `POST /api/v1/admin/payments/attempts/{attemptId}/investigate-order`, sin cuerpo.
Requiere autenticación y rol ADMIN (regla existente `/api/v1/admin/**` y `@PreAuthorize`).
Devuelve HTTP 200 con `{ "outcome": "VERIFIED|INCONCLUSIVE|CONTRADICTION|TECHNICAL_ERROR", "reason": "CODIGO" }`.
Los errores de autenticación/autorización y UUID inválidos conservan el tratamiento HTTP existente.
No acepta IDs ni referencias arbitrarios enviados por un operador.

## Evidencia y correlación

Consulta exclusivamente el ID ya persistido mediante `MercadoPagoGateway.queryOrder`,
que usa `GET /v1/orders/{id}`. Suspende cualquier transacción del llamador durante HTTP.
Obtiene paymentId con la lectura inicial sin bloqueo pesimista. En una transacción
REQUIRES_NEW breve bloquea primero Payment y después relee PaymentAttempt con bloqueo;
revalida que su propietario siga siendo el pago esperado antes de asociar la orden.
Las rutas que solo agregan auditoría siguen el mismo orden y revalidan la propiedad.
Valida propiedad, proveedor Mercado Pago, modo ORDERS, identidad remota y ausencia de
asociaciones contradictorias de orden y transacciones. El snapshot V24 debe existir,
coincidir en modo, clave, versión, referencia histórica paymentId y total con Payment y GET.
No acepta pa1_. Valida moneda de las transacciones; sin evidencia de moneda, inconcluso.
Cuenta y entorno se comparan cuando están configurados; si falta evidencia remota
requerida, inconcluso. No infiere cuenta/entorno desde el token.

Una identidad verificada con checkout_url permite `recordOrder` sobre el intento recién
bloqueado y transición UNKNOWN -> CREATED, respetando el contrato y restricciones V25.
Sin checkout_url se audita identidad VERIFIED, pero permanece UNKNOWN: no se inventa URL.
No modifica Payment, estado financiero del intento, estado remoto financiero ni reembolsos.
Un resultado concurrente de creación confirmado gana; no se guarda la entidad leída antes del GET.
Solicitudes repetidas agregan evidencia, sin duplicar asociaciones ni efectuar POST remoto.

Reutiliza `remote_order_creation_events` para códigos MANUAL_* sin cuerpos, tokens,
URLs, referencias externas ni datos de clientes. Los cambios de estado conservan también
la auditoría automática V25. Fallos locales devuelven TECHNICAL_ERROR/LOCAL_FAILURE;
si la base de datos falla, no se garantiza persistir la auditoría de ese fallo.
No requiere migración.

## Investigación de capacidades oficiales (2026-11-06)

El adaptador existente solo consulta por ID. La referencia oficial documenta búsqueda
`GET /v1/orders` con external_reference, fechas obligatorias y paginación:
https://www.mercadopago.com.mx/developers/en/reference/online-payments/checkout-api/search-order/get
La guía de migración menciona una ruta distinta `/v1/orders/search`:
https://www.mercadopago.com.uy/developers/en/docs/checkout-api-orders/resources/migrate-payments-to-orders

Estas fuentes no prueban unicidad de external_reference, cobertura temporal completa ni
correlación unívoca con intentos históricos que comparten paymentId. Por ello no se
implementa búsqueda ni se atribuye una coincidencia a un intento por importe/referencia.
Sin ID persistido: INCONCLUSIVE/NO_UNAMBIGUOUS_ORDER_ID. Un 404 no prueba que el POST
original no creó la orden y nunca autoriza repetirlo. 429/5xx/red: TECHNICAL_ERROR.

## Verificación y pendientes de 2B.3

Pruebas enfocadas incluyen integración servicio/adaptador con MockRestServiceServer,
GET sin transacción, respuestas 200/404/429/500, referencia histórica, repetición,
propiedad contradictoria, snapshot ausente, importe, moneda ausente, y eventos
concurrentes de creación/aprobación simulados. La seguridad por método se verifica
con contexto Spring y roles CUSTOMER, MERCHANT y ADMIN.

`ManualOrderInvestigationPostgresIntegrationTest` agrega cobertura PostgreSQL real
con los repositorios JPA, JdbcTemplate y proxies transaccionales del servicio manual
y de persistencia del webhook. Usa TestPostgresConfig.fromEnvironment() y el cliente
HTTP real con MockRestServiceServer: ninguna petición sale a Mercado Pago.

Escenarios: asociación UNKNOWN -> CREATED con referencia histórica paymentId; rollback
de auditoría JDBC si falla el flush JPA; rollback de asociación JPA si falla una auditoría
diferida al commit; dos investigadores con GET simultáneos y una sola transición;
webhook con el pago bloqueado mientras la investigación espera (aprobación y rechazo);
aprobación/reembolso completado mientras GET está pendiente; GET sin transacción ni
conexión JDBC vinculada, incluso al suspender y restaurar la transacción del llamador;
ausencia de ID y GET 404/429/500 conservando UNKNOWN, sin POST ni nuevos intentos.

La carrera usa hilos y conexiones distintos, observa pg_blocking_pids y comprueba que
el webhook puede bloquear el intento con NOWAIT antes de continuar. Los fallos se
inyectan con una restricción o trigger temporal de prueba, limitados al UUID del intento
y eliminados en finally; no se agregan migraciones. Solo se limpian los registros propios.

Ejecución manual desde backend, con DB_URL, DB_USERNAME y DB_PASSWORD exportadas:

```bash
./mvnw -Dtest=ManualOrderInvestigationPostgresIntegrationTest test
```

DB_URL debe apuntar a PostgreSQL en loopback con puerto explícito y base
`jaldishop_integration_test`, sin parámetros, según TestPostgresConfig. El usuario de
pruebas debe poder aplicar Flyway y crear/eliminar los objetos de inyección de fallos.
No ejecutar junto con suites que borren globalmente las tablas de pagos.

Validación de cierre confirmada por el responsable: 22 pruebas unitarias y de seguridad
aprobadas y 12 pruebas PostgreSQL ejecutadas manualmente, con BUILD SUCCESS, sin
errores, fallos ni pruebas omitidas. Las pruebas PostgreSQL no se ejecutaron desde
el sandbox. No se hicieron conexiones a Neon/producción.
La carrera cubre la persistencia transaccional del webhook, sin recorrer su endpoint,
firma ni confirmación comercial. No sustituye pruebas de carga o carreras entre procesos.
Para 2B.3: verificar oficialmente búsqueda aplicable a la cuenta/país, cobertura y
paginación; definir identidad inequívoca por intento antes de ampliar recuperación;
resolver órdenes sin moneda/URL y eventual configuración histórica de cuenta/entorno.
No hay scheduler, recuperación automática, frontend ni nuevas funciones Preferences.
