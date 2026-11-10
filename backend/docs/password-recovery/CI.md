# Certificación de Password Recovery en CI

Estado al preparar el PR: rama local, sin push ni PR abierto. GitHub Actions todavía no se ha ejecutado para estos commits. La publicación está pendiente de verificar o confirmar que la integración externa de Render no crea despliegues automáticos ni previews.

## Alcance de los workflows

`ci.yml` conserva sus disparadores de push a `main`/`develop` y PR hacia esas ramas. El backend usa PostgreSQL 16 desechable, dos bases exclusivas de integración, Java 21 y binarios PostgreSQL locales del runner. Ejecuta `clean verify`, con importación de `.env` y correo real deshabilitados. Su control existente exige informes y cero omisiones; el fallo de Maven se propaga al job. No se cambian los jobs de frontend.

`password-recovery.yml` se activa solo por `pull_request` hacia `main`, cuando cambia backend o ese workflow. Tiene permisos `contents: read`, sin deploy, hooks, secretos de producción ni `pull_request_target`. Su PostgreSQL 16 pertenece a un servicio nuevo del job y no comparte datos con el job general.

El workflow `deploy-docs.yml` solo se activa por push a `main`; no fue modificado. Estos disparadores no permiten verificar la configuración de autodespliegue o previews del servicio externo Render. No existe un Blueprint `render.yaml` en la raíz del repositorio y no hay conexión de lectura confirmada al proveedor al preparar esta publicación.

## Pruebas críticas obligatorias

El job aislado ejecuta las pruebas de recuperación, seguridad HTTP, revocación STOMP y correo deshabilitado. `verify-ci-reports.py` rechaza informes ausentes, cero tests, fallos, errores, omisiones y escenarios críticos ausentes aunque el resultado agregado parezca verde.

| Escenario | Evidencia obligatoria |
|---|---|
| Cliente y comerciante: contraseña nueva/anterior, hash y revocación | Dos ejecuciones de `fullFlowWorksForCustomerAndMerchantWithJwtRevocation` |
| Un solo ganador del reset concurrente | `concurrentResetsHaveExactlyOneWinner`, dos workers con transacciones/conexiones independientes |
| Emisiones simultáneas con un token vigente | `concurrentRequestsLeaveOnlyOneUsableToken` |
| Expiración, inexistencia, reemplazo y reutilización | Métodos explícitos del test JDBC y verificaciones dentro del flujo completo |
| Fallo de actualización revierte el consumo | `failedCredentialUpdateRollsBackTokenConsumption` |
| Perfil obsoleto no revierte contraseña ni versión | `staleProfileSaveCannotRestorePasswordOrRevokedSessions` |
| Flyway V27 y datos previos preservados | `additiveMigrationPreservesAllPreexistingCredentials` invoca `Flyway.validate()` |
| Historial Flyway completo | Consulta posterior compara versiones/scripts/success con todos los archivos de migración, incluyendo V27 |
| JWT/refresh y contrato público | Métodos de `PasswordRecoverySecurityWebTest`, incluyendo JWT heredados y carrera filtro/servicio |
| Límites y protección contra enumeración | Políticas de IP/cuenta, respuestas genéricas y cola limitada en pruebas HTTP/de servicio |
| CONNECT antiguo rechazado por WebSocket TCP real | `oldJwtCannotConnectAfterPasswordReset`, con JWT nuevo aceptado |
| Suscripción TCP existente deja de recibir | `alreadySubscribedSessionStopsReceivingAfterPasswordReset`; un cliente con JWT nuevo sí recibe el mismo evento |
| Revocación en interceptor y broker en memoria | Métodos explícitos de `StompAuthChannelInterceptorTest` y `StompDeliveryAuthorizationBrokerTest` |

El nuevo test WebSocket solo crea sus propios clientes de prueba. Se conecta a `ws://127.0.0.1:<puerto>/ws`; el origen HTTP reservado `http://recovery.test` es un header permitido únicamente por la configuración de ese test. No envía tráfico a ese dominio ni modifica la política CORS de producción. El correo está simulado y no se activa Resend.

La base del job aislado debe estar vacía al inicio para que la prueba pueda migrar a V26, capturar las credenciales previas y luego aplicar V27. El servicio de GitHub Actions garantiza ese aislamiento. No se acepta reutilizar una base habitual de desarrollo.

## Resultados locales y límites

La preparación local recompila todas las fuentes de pruebas, incluidas las nuevas pruebas TCP. La selección focalizada ejecutable ha pasado con 100 pruebas, cero fallos/errores/omisiones. El gate de reportes se comprobó con casos sintéticos de éxito, concurrencia ausente, omisión, error y fallo Maven.

Los YAML se validaron estructuralmente, junto con el Python embebido del workflow. Esto no sustituye ejecutar GitHub Actions. El sandbox local sigue bloqueando sockets y namespaces; por ello el test WebSocket real y la integración JDBC permanecen pendientes de ejecución en CI.

La última suite general ejecutada antes de preparar el PR reportó 1411 registros: 1338 aprobados, 73 errores y cero omitidos. Los errores se clasificaron en 19 aperturas JDBC, 6 servidores HTTP simulados, 3 reservas de puerto y 45 errores en cascada de contextos Spring. No se deben trasladar automáticamente esas causas a un fallo en GitHub Actions: cada error allí debe analizarse a partir de su propio informe.

La prueba SQL sin sockets pasó sobre un cluster PostgreSQL 18.6 desechable. No certifica por sí misma el historial Flyway ni la concurrencia de Spring. Los reportes y logs generados durante el diagnóstico local no forman parte del PR.

## Interpretación de los resultados de GitHub

Se deben revisar ambos jobs backend, los XML publicados como artefactos y el listado de métodos críticos. Un check verde parcial no certifica escenarios que no se ejecutaron. La suite general incluye también las integraciones WebSocket preexistentes y los tests ajenos al módulo, sin deshabilitarlos.

Si hay fallos, se documentarán separando inicialización/infraestructura, configuración de pruebas preexistente y regresiones del cambio. Los controles de seguridad y las aserciones no se debilitarán para obtener un resultado verde.

El PR no queda certificado para merge hasta que pasen JDBC/concurrencia real, Flyway completo, JWT/refresh, WebSocket real y la suite general, o se documenten explícitamente los bloqueos preexistentes que requieran una decisión de revisión. No se hará merge ni despliegue desde este trabajo.
