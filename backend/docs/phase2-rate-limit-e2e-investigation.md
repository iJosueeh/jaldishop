# Diagnóstico del aislamiento E2E de rate limiting

## Evidencia y causa

Base local y head del PR #82: `cdfe712`. Árbol inicialmente limpio. Se revisó
el log del job Backend de [CI 248](https://github.com/iJosueeh/jaldishop/actions/runs/42696265035):
1465 pruebas, 2 failures y 4 errors. Las suites de ambos frontends pasaron en
esa ejecución. Los reportes Surefire locales anteriores sólo muestran un
fallo de inicialización por ausencia de PHASE2A_DB_URL; no son los reportes de CI.

`MercadoPagoOrdersApiEndToEndIntegrationTest` ejecutó 21 casos en 3,622 segundos.
Sus casos usan el mismo USER_ID. Spring conserva como singleton tanto las
políticas AuthenticatedRateLimits como el motor RateLimits y sus mapas JVM.
`@BeforeEach` limpiaba datos SQL y el servidor HTTP simulado, pero no contadores.
La cuota de seis nuevas creaciones por minuto se acumula entre casos aunque
cada uno restaure los datos de BD. No es una configuración de producción
incorrecta: las pruebas independientes compartían la identidad y estado de cuota.

Los cuatro errors salen de RateLimits.java:61, la comprobación de cuota del
contador existente, y no de la comprobación de capacidad del almacén:

- concurrentDifferentBodiesForSameAttemptHaveOnlyOneWinner
- testE2E01_successfulOrderCreationWithOrdersApi
- testE2E02_legacyPreferencesFlowRegression
- twoAttemptsOfSamePaymentEmitDistinctDurableReferences

Las aserciones de P4201/P4202 se investigaron por separado:

- P4201: trigger de constraint diferido tras INSERT en checkout_creation_requests.
  Debe fallar el commit del snapshot, impedir el POST y revertir ese INSERT.
- P4202: trigger antes de UPDATE de payment_attempts cuando se guarda el ID remoto.
  Debe preservar el intento pendiente y el snapshot, impidiendo otro POST remoto.
- Ambos casos capturan `Exception`, que también acepta RateLimitExceeded. Si el
  contador ya está agotado, la preparación falla antes de alcanzar el trigger;
  PostgresTriggerAssertions no encuentra el SQLException esperado y falla.
- El log anterior no conserva la excepción original de esos dos casos. Por ello,
  el enmascaramiento por cuota es una explicación fundada, todavía pendiente de
  confirmación ejecutando ambos casos contra PostgreSQL. No se ha demostrado un
  defecto transaccional ni se han cambiado los contratos de pago para resolverlo.

## Corrección acotada a pruebas

La clase E2E reemplaza únicamente el bean RateLimits mediante MockitoBean.
Antes de cada escenario delega sus dos APIs de admisión en una instancia nueva
del motor real, construida con el Environment de Spring. Conserva seis nuevas
creaciones por 60 segundos. Todos los hilos del caso comparten esa instancia;
no hay contadores por hilo, bypass, cuotas infladas ni espera para expirar ventanas.
Los métodos E2E se ejecutan explícitamente en el mismo hilo para no solapar la
limpieza SQL y el reinicio de contadores entre casos. Los executors y pruebas
de concurrencia dentro de cada caso conservan sus hilos y el mismo motor.
Los servicios de pago, transacciones, repositorios, triggers y proveedor HTTP
simulado siguen ejecutando sus implementaciones habituales.

No se añade una API de reset a producción. No se alteran RateLimits,
AuthenticatedRateLimits, servicios transaccionales, migraciones, idempotencia,
tratamiento de errores remotos ni configuración de producción.

Se conservan todas las aserciones SQLState/mensaje, rollback, snapshot y número
de POST. P4201/P4202 rechazan explícitamente RateLimitExceeded y exigen un
SQLException en la cadena causal con SQLState y mensaje exactos. Capturar
Exception no es suficiente para aprobar el caso. El helper mejora su mensaje
de fallo para identificar la excepción recibida. Una regresión parametrizada
confirma que RateLimitExceeded no satisface ninguna de ambas aserciones.

Se añade un caso E2E con la cuota real agotada. MockMvc utiliza el contexto web,
filtro JWT y seguridad reales, con un JWT firmado para el usuario de la fixture:
POST /api/v1/checkout/mercado-pago/preference devuelve HTTP 429,
RATE_LIMIT_EXCEEDED y Retry-After entre 1 y 60 segundos. SQL verifica que no se
crean pagos, intentos ni snapshots y no se protege la reserva. La recuperación
HTTP de la sesión anterior devuelve 201 y los mismos paymentId, preferenceId e
initPoint; los recuentos SQL y el único POST al proveedor simulado se conservan.

## Pruebas restauradas

No se encontraron omisiones nuevas de los seis casos en el head local/remoto:
ya estaban habilitados. Se retiraron de POM las exclusiones anteriores de:

- CustomerAddressPostgresIntegrationTest
- StoreSearchPostgresIntegrationTest
- StoreCustomerPostgresIntegrationTest

CI deja de duplicar exclusiones en la línea Maven y ejecuta `./mvnw clean test`.
Ya configura las tres variables *_TEST_PG_BIN y PostgreSQL 16 con extensiones
necesarias para estas suites. El validador de Surefire sigue rechazando fallos,
errores, informes ausentes y pruebas omitidas/abortadas.

Se conserva la exclusión de SingleAttemptIsolatedReconciliationTest: es un
runner manual que lee .env.sandbox.local y consulta Mercado Pago. No pertenece
a la suite automatizada sin operaciones externas. También se conserva la
exclusión preexistente de clases `$`.

## Validación local y límites

- Compilación de todas las fuentes y pruebas mediante Maven offline.
- 44 pruebas focalizadas pasaron, cero fallos/errores/omisiones:
  AuthenticatedRateLimitsTest, RateLimitsTest, AuthenticatedRateLimitWebTest,
  CreatePaymentPreferenceServiceTest y PostgresTriggerAssertionsTest.
- Suite de unitarias/web disponible: 1253 pasaron, cero fallos/errores. Catorce
  casos de las tres suites PostgreSQL quedaron omitidos por sus condiciones de
  entorno; no se considera una validación completa del backend.
- La regresión de contadores reproduce seis admisiones y rechazo al reconstruir
  sólo la fachada sobre el mismo motor, y verifica un presupuesto independiente
  al crear un motor nuevo. Se conservan pruebas concurrentes y de cabeceras HTTP.
- Se inicializó correctamente un clúster PostgreSQL 18.6 desechable desde los
  binarios locales disponibles. El arranque falla al crear el socket loopback:
  `Operation not permitted`. No se utilizó ninguna BD existente ni externa.
- Se intentó la clase E2E completa con URLs y credenciales ficticias limitadas
  al proceso diagnóstico. Falló antes de ejecutar los casos:
  `startMockServer -> SocketException: Operation not permitted` (BeforeAll).
  No se pudieron repetir los seis casos individualmente ni ejecutar la suite
  completa con PostgreSQL. Faltan sockets locales permitidos; los triggers
  siguen pendientes de verificación real. Este error de infraestructura no se
  presenta como una regresión funcional ni como un resultado E2E aprobado.

## Ejecución en Fedora con PostgreSQL de pruebas

Requisitos: JDK 21, Maven Wrapper, PostgreSQL con extensiones btree_gist y
unaccent, sockets loopback permitidos y usuario de sistema sin privilegios root
para initdb. Las bases dedicadas jaldishop_integration_test y
jaldishop_fase2a_test deben existir, ser desechables y permitir las migraciones,
limpieza de fixtures y creación/eliminación de triggers. Las URLs deben apuntar
a loopback, con puerto explícito y sin parámetros, como exige TestPostgresConfig.
No usar bases con datos que deban conservarse.

Exportar previamente estas variables con la configuración local de pruebas.
Se indican sólo sus nombres, sin credenciales:

- DB_URL
- PHASE2A_DB_URL
- DB_USERNAME
- DB_PASSWORD
- JWT_SECRET
- ADDRESS_TEST_PG_BIN
- STORE_SEARCH_TEST_PG_BIN
- STORE_CUSTOMERS_TEST_PG_BIN

Las tres variables *_TEST_PG_BIN indican directorios con initdb y postgres de
la instalación local; esas suites crean sus propios clústeres desechables.
FLYWAY_VALIDATE_ON_MIGRATE es opcional. El E2E configura Mercado Pago con un
servidor HTTP simulado en loopback; no necesita credenciales reales del proveedor.

Desde el repositorio de esta sesión, con las variables ya exportadas:

```sh
cd /home/ijosueeh/Documentos/Proyectos/jaldishop/backend

# Agente de la versión Mockito ya resuelta en este proyecto, sin auto-adjunción.
test -r "$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar"

# Los seis casos afectados, cada uno en una ejecución individual.
(
for caso in \
  concurrentDifferentBodiesForSameAttemptHaveOnlyOneWinner \
  testE2E01_successfulOrderCreationWithOrdersApi \
  testE2E02_legacyPreferencesFlowRegression \
  twoAttemptsOfSamePaymentEmitDistinctDurableReferences \
  deferredCommitFailurePreventsPostAndRollsBackRequest \
  failureSavingRemoteSuccessDoesNotAuthorizeAnotherPost
do
  ./mvnw "-DargLine=-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" \
    "-Dtest=MercadoPagoOrdersApiEndToEndIntegrationTest#$caso" test || exit "$?"
done
)

# Clase E2E completa, incluidos el contrato HTTP 429 y los triggers.
./mvnw "-DargLine=-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" \
  -Dtest=MercadoPagoOrdersApiEndToEndIntegrationTest test

# Suite backend automatizada completa, incluidas las tres suites restauradas.
./mvnw "-DargLine=-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" \
  clean test
```

Si falta el jar del agente, `./mvnw dependency:go-offline` descarga las
dependencias del proyecto antes de esos comandos; requiere acceso a Maven Central.

Requieren PostgreSQL real todos los casos de MercadoPagoOrdersApiEndToEndIntegrationTest
(incluidos P4201/P4202 y agotamiento HTTP), las tres suites restauradas y las
demás integraciones de persistencia, concurrencia, checkout, pedidos,
conciliación, reembolsos y WebSocket de la suite completa. Las pruebas
AuthenticatedRateLimitsTest, RateLimitsTest, AuthenticatedRateLimitWebTest,
CreatePaymentPreferenceServiceTest y PostgresTriggerAssertionsTest no requieren BD.

En CI usar las bases dedicadas jaldishop_integration_test y jaldishop_fase2a_test
y las variables de binarios ya definidas. La ejecución completa debe incluir los
22 casos de la clase E2E, las tres suites restauradas y cero omisiones automáticas.
No se hicieron pagos reales, push, merge ni despliegue durante esta investigación.
