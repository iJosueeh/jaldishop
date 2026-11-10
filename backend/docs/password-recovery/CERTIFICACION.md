# Certificación local de password-recovery — 2026-11-10

**Certificación parcial.** Se auditó cada uno de los 69 errores originales, se reejecutaron pruebas focalizadas y Maven completo, y se intentaron PostgreSQL nativo y Podman aislado. No se modificaron implementación, pruebas existentes, POM, frontend ni servicios desplegados. Los hashes de los archivos de `src/main`, `src/test` y `pom.xml` coinciden con los del inicio de esta validación.

El informe anterior fue impreciso al atribuir todos los errores a sockets: **uno se debía a una variable de configuración ausente**. Tampoco debe confundirse ejecutar SQL con certificar el historial de Flyway.

## Clasificación de los 69 errores originales

Se inspeccionaron los elementos `<error>` de Surefire y sus cadenas `Caused by`, no solo el resumen por clase. La evidencia normalizada está en [errores-originales.json](certificacion/errores-originales.json); los XML completos iniciales quedaron en un directorio temporal privado cuya ruta se conserva en [resultados.json](certificacion/resultados.json).

| Registros | Causa observada | Evidencia |
|---:|---|---|
| 18 | Apertura de conexión PostgreSQL/JDBC rechazada por el entorno | `java.net.SocketException: Operation not permitted`; incluye arranque Flyway, fixtures y limpieza |
| 5 | Creación de servidores HTTP simulados locales rechazada | `SocketException` en `sun.nio.ch.ServerSocketChannelImpl` / `sun.net.httpserver`, antes de ejecutar negocio |
| 45 | Fallos en cascada por inicialización fallida de Spring | `ApplicationContext failure threshold (1) exceeded`; cada registro está vinculado al fallo directo de su clase |
| 1 | Configuración local incompleta, distinta del bloqueo de red | `MercadoPagoOrdersApiEndToEndIntegrationTest`: `PHASE2A_DB_URL` ausente; `TestPostgresConfig` lanza `IllegalStateException`, luego falla la inicialización estática |
| **69** | **Total de errores originales** | **0 fallos de aserción en aquella ejecución** |

Los 45 fallos en cascada se reparten en `ActuatorSecurityWebTest` (5), `ChangeOrderStatusConcurrencyIntegrationTest` (3), `ManualOrderInvestigationPostgresIntegrationTest` (15), `PaymentReconciliationPostgresIntegrationTest` (6) y `WebSocketStompRealIntegrationTest` (16). No son 45 reproducciones independientes de un error funcional.

Los cinco servidores simulados que fallaron pertenecen a `EndToEndBackendFlowValidationIntegrationTest`, `PaymentFlowEndToEndIntegrationTest`, `OrderReconciliationPostgresIntegrationTest`, `PaymentRefundPostgresIntegrationTest` y `PaymentSchedulerExecutionPostgresIntegrationTest`.

**Regresiones:** esos registros no prueban una regresión de password-recovery: las conexiones/servidores fallan antes de ejecutar la lógica que se pretendía verificar. Las aserciones ejecutables pasan. Esto tampoco permite descartar una regresión que solo aparezca con JDBC real o bajo concurrencia entre conexiones: esa parte sigue pendiente.

Se detectó además un problema previo de aislamiento de pruebas: `ActuatorSecurityWebTest` excluye nombres antiguos de autoconfiguraciones de Spring Boot, inexistentes en el classpath Boot 4.1.1. Las clases reales tienen otros paquetes; por eso la prueba intenta inicializar Hibernate/Flyway y depende de una base. Se comprobó que las exclusiones ya eran iguales en `HEAD`. La [evidencia del classpath](certificacion/exclusiones-actuator.json) muestra los nombres ausentes y los presentes. No se corrigió ni se modificó esa prueba en esta validación.

## Las 15 pruebas omitidas

Las razones exactas de `<skipped message>` están en [omisiones-originales.json](certificacion/omisiones-originales.json).

| Clase | Omisiones originales | Condición ausente | Resultado al habilitarla ahora |
|---|---:|---|---|
| `CustomerAddressPostgresIntegrationTest` | 3 | `ADDRESS_TEST_PG_BIN` | `initdb` funciona; falla `new ServerSocket` en línea 56 |
| `StoreCustomerPostgresIntegrationTest` | 3 | `STORE_CUSTOMERS_TEST_PG_BIN` | `initdb` funciona; falla `new ServerSocket` en línea 49 |
| `StoreSearchPostgresIntegrationTest` | 8 | `STORE_SEARCH_TEST_PG_BIN` | `initdb` funciona; falla `new ServerSocket` en línea 60 |
| `RemoteOrderCreationMigrationTest` | 1 | `PHASE2A_DB_URL` | Llega a JDBC; falla la apertura del socket en línea 17 |
| **Total** | **15** | | **Ya no omitidas por condición; sus cuerpos no quedaron aprobados** |

Se suministraron las variables en el proceso de validación y rutas válidas de binarios PostgreSQL locales. No se eliminaron condiciones ni se añadieron exclusiones. Las cuatro clases fallan al inicializar y generan cuatro registros de error de clase; eso explica por qué el contador de pruebas reportadas no coincide con el número de métodos previamente omitidos. Los métodos parametrizados también pueden producir más ejecuciones cuando su inicialización funciona.

Las exclusiones Maven preexistentes para clases internas y el runner manual `SingleAttemptIsolatedReconciliationTest` se conservaron. Ese runner manual no forma parte de las 15 omisiones anteriores y no se ejecutó contra ningún proveedor.

## Infraestructura intentada

- Prueba directa de `socket(AF_INET, SOCK_STREAM)`: `PermissionError(1, 'Operation not permitted')`.
- Prueba directa de sockets Unix: el mismo rechazo. Un socket Unix no sirve como alternativa dentro de este entorno.
- PostgreSQL 18.6 nativo, cluster nuevo y temporal: `initdb` terminó con código 0; el servidor terminó con código 1: `could not create IPv4 socket for address "127.0.0.1": Operation not permitted`, seguido de `FATAL: could not create any TCP/IP sockets`. Ver [arranque PostgreSQL](certificacion/postgres-arranque.txt).
- Podman normal: no pudo escribir su runtime bajo `/run/user/1000`.
- Podman con almacenamiento y runtime nuevos dentro de `/tmp`, sin tocar contenedores ni volúmenes existentes: código 125; `newuidmap: write to uid_map failed: Operation not permitted`, `cannot set up namespace`. No se descargaron imágenes ni se crearon bases remotas.
- No hay ejecutable Docker disponible en esta sesión.

No se intentó eludir estas restricciones mediante privilegios o mecanismos alternativos. Tampoco se sustituyó PostgreSQL por H2 ni se inventó una prueba de concurrencia basada en mocks.

## Resultado JDBC y cobertura de seguridad

La ejecución aislada `-Dtest=PasswordRecoveryPostgresIntegrationTest test` terminó con **1 error de inicialización, 0 aprobadas, 0 omitidas**. El fallo ocurre en `migratePreviousVersion`, antes de que Flyway obtenga conexión. Sus nueve ejecuciones previstas permanecen bloqueadas: cliente, comerciante, expiración/inexistencia, reemplazo, reset concurrente, emisión concurrente, rollback de actualización, perfil obsoleto y preservación de datos.

| Condición solicitada | Resultado verificable aquí | Límite de la evidencia |
|---|---|---|
| Consumo único con conexiones concurrentes | **Bloqueado** | SQL serial pasa; prueba JDBC concurrente no pudo inicializar |
| Solicitudes simultáneas de recuperación | **Bloqueado** | Falta ejecutar los dos workers contra PostgreSQL accesible |
| JWT anterior rechazado y JWT actual válido | **PASS** | JWT firmados reales, filtro y MockMvc; repositorio simulado |
| Protección de `/refresh`, incluida carrera filtro/servicio | **PASS** | Comparación de versión probada; persistencia concurrente pendiente |
| JWT heredado sin `cv` | **PASS** | Aceptado antes del reset y rechazado después, en MockMvc |
| Sesiones revocadas WebSocket | **PASS en broker en memoria** | CONNECT, SUBSCRIBE y entregas existentes; handshake TCP real bloqueado |
| Tokens vencidos, inexistentes y reutilizados | **PASS unitario y SQL serial** | Falta comprobar esos escenarios por JDBC/Spring real |
| Contraseña nueva aceptada y anterior rechazada | **PASS de servicio con BCrypt real** | Flujo PostgreSQL de cliente y comerciante pendiente |
| Respuesta genérica / enumeración | **PASS de controller y servicios** | Respuestas 202 idénticas en MockMvc, cola/límites por correos conocidos y desconocidos; no es una medición de tiempos de un servidor real |
| Rate limiting | **PASS en memoria** | Cuenta, IP, capacidad y headers falsificados; no certifica contadores distribuidos ni ingress desplegado |

La selección de esta revisión ejecutó **100 pruebas, 0 fallos, 0 errores, 0 omitidas**. Incluye identity, seguridad HTTP, WebSocket/broker, rate limiting y buzón; es una selección distinta de las 175 pruebas focalizadas del informe previo. Los resultados por clase se conservan en [resultados.json](certificacion/resultados.json).

## V27 y Flyway

V27 es aditiva: agrega la versión de credenciales con valor 0 y una tabla de tokens con PK/FK de usuario, hash único validado y expiración. No borra ni reescribe usuarios ni altera migraciones anteriores.

Se ejecutaron de nuevo **los 26 scripts disponibles desde V1 hasta V27** en un cluster PostgreSQL 18.6 desechable, en modo monousuario. No existe un archivo V9 en el historial del repositorio; no se creó ni se rellenó ese hueco. La [lista de scripts y sus SHA-256](certificacion/migraciones.json) identifica exactamente lo ejecutado.

La comprobación terminó con **PASS**, sin ERROR/FATAL/PANIC: preservación de campos de usuarios previos, unicidad, formato del hash, límite de expiración, hash inexistente, rollback, reemplazo, consumo único, incremento de versión y borrado en cascada. Ver [salida SQL](certificacion/postgres-sql.txt).

**Esto no certifica `flyway_schema_history`.** El programa SQL aplica directamente los scripts; Flyway requiere JDBC y no pudo abrir una conexión. La migración gestionada por Flyway y `validate()` sobre una base nueva con todo el historial siguen pendientes. No se fabricó una tabla de historial para simular esa certificación. PostgreSQL 16 tampoco se considera certificado por una prueba SQL ejecutada sobre 18.6.

## Buzón, logs y endpoints

- Las pruebas existentes comprueban permisos 0700 del directorio y 0600 de los `.eml`, nombres sin token/correo y ausencia de escritura en modo deshabilitado.
- La configuración versionada mantiene `PASSWORD_RESET_MAIL_MODE=disabled` y `PASSWORD_RESET_REAL_DELIVERY_ENABLED=false` como valores por defecto. El perfil `prod` sin `local`/`test` rechaza el modo de buzón local. No se activó Resend ni se ejecutó un envío real.
- Se ejercitó el **logging DEBUG real de RestClient** con una petición y respuesta en memoria. El cuerpo contenía un secreto de prueba generado en memoria y se comprobó su serialización; el log de escritura mostraba `PasswordResetMail[REDACTED]` y no el secreto. Los DTO también ocultan su contenido. Ver [resultado de logs](certificacion/logs-redactados.txt).
- Un probe con `SecurityConfig` real y perfil `prod` en MockMvc obtuvo `401` sin autenticación y `404` con JWT válido para rutas de buzón/listado de tokens. También comparó respuestas 202 idénticas para dos correos. El servicio de solicitud y repositorio estaban simulados; no fue un servidor desplegado. Ver [resultado de rutas](certificacion/rutas-y-respuesta-generica.txt).
- La inspección del código no encontró endpoints de descarga/listado del buzón, handlers de archivos externos ni configuración estática que apunte al directorio de correo. Los dos endpoints de recuperación no devuelven tokens. El buzón vive fuera de los recursos estáticos empaquetados.

El `.eml` contiene intencionalmente el token en texto, como cualquier mensaje de recuperación; su privacidad depende de esos permisos y de no publicar el directorio. La verificación anterior corresponde al código/configuración del repositorio, no al filesystem o al ingress de producción, que no se inspeccionaron ni modificaron. La política de confiar en IPs de Render permanece como estaba; su activación requiere verificación de ingress aparte. Los límites y la cola son por proceso y se reinician al reiniciar la aplicación.

## Maven completo repetido

Comando base desde `backend/`:

```text
./mvnw -o -B -Dspring.config.import= \
  -DargLine=-javaagent:<byte-buddy-agent-1.18.11.jar> verify
```

Se usaron variables de proceso exclusivamente de prueba:

```text
DB_URL=jdbc:postgresql://127.0.0.1:55439/jaldishop_integration_test
PHASE2A_DB_URL=jdbc:postgresql://127.0.0.1:55439/jaldishop_fase2a_test
DB_USERNAME=validation_test
DB_PASSWORD=<valor ficticio local>
JWT_SECRET=<clave exclusiva de pruebas>
PASSWORD_RESET_MAIL_MODE=disabled
PASSWORD_RESET_REAL_DELIVERY_ENABLED=false
ADDRESS_TEST_PG_BIN=<binarios PostgreSQL locales>
STORE_CUSTOMERS_TEST_PG_BIN=<binarios PostgreSQL locales>
STORE_SEARCH_TEST_PG_BIN=<binarios PostgreSQL locales>
```

Resultado: **BUILD FAILURE; 1411 registros, 1338 aprobados, 0 fallos de aserción, 73 errores, 0 omitidas**.

| Errores nuevos por causa | Registros |
|---|---:|
| Socket JDBC bloqueado | 19 |
| Servidor HTTP simulado bloqueado | 6 |
| Reserva de puerto mediante `ServerSocket` bloqueada | 3 |
| Cascada por fallo previo de contexto Spring | 45 |
| **Total** | **73** |

La configuración faltante de `PHASE2A_DB_URL` quedó resuelta en esta ejecución; esa clase ahora falla al crear su servidor HTTP simulado. Las cuatro clases previamente omitidas aportan cuatro errores de inicialización. Ninguna de esas pruebas se convirtió en un resultado aprobado. El [detalle de los 73 errores](certificacion/errores-nueva-suite.json) conserva cadenas causales y líneas relevantes por prueba.

Los XML y logs completos se preservaron bajo el directorio privado indicado en `certificacion/resultados.json`; no se copiaron propiedades de sistema o variables sensibles al repositorio.

## Infraestructura que falta para terminar

1. Un host de pruebas local que permita crear y usar sockets TCP de loopback desde Java, PostgreSQL y los servidores HTTP simulados. Si se usa Podman rootless, debe permitir namespaces de usuario y `newuidmap`; alternativamente basta un servicio PostgreSQL local aislado.
2. PostgreSQL desechable, preferentemente la versión 16 utilizada por CI, con extensiones `btree_gist` y `unaccent` y permiso para instalarlas. Dos bases exclusivas: `jaldishop_integration_test` y `jaldishop_fase2a_test`; nunca Neon ni datos desplegados.
3. Java 21, Maven y las variables de prueba anteriores; binarios PostgreSQL para las tres pruebas que crean su propio cluster. Importación de `.env` deshabilitada y correo deshabilitado.
4. Repetir `PasswordRecoveryPostgresIntegrationTest`, el WebSocket real, las cuatro clases antes omitidas y `mvnw verify`. Verificar con Flyway `migrate` y `validate` el historial completo hasta V27 en una base nueva.

No se detectaron regresiones funcionales en las aserciones que sí corrieron. Persisten como riesgos no certificados la transacción JPA/JDBC real, las carreras entre conexiones, el historial Flyway gestionado por el motor y el transporte WebSocket real. Se detuvo el trabajo sin commits, push, publicación o despliegue, sin modificar pagos, pedidos, inventario, Render o Neon.
