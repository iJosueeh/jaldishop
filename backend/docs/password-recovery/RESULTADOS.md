# Resultados locales — 10 de noviembre de 2026

> Este documento conserva la primera ejecución. La revisión posterior y las correcciones están en [CERTIFICACION.md](CERTIFICACION.md).

La implementación compila y las pruebas focalizadas pasan. **La suite completa no queda aprobada:** este entorno prohíbe crear sockets incluso en loopback, por lo que no fue posible ejecutar las integraciones JDBC/HTTP con servidor real. No se usaron bases remotas para sortear esta restricción.

| Verificación | Resultado |
|---|---|
| Maven focalizado, incluyendo identity, seguridad HTTP, WebSocket/broker, dominio, mapper, rate limiting y controladores de catálogo afectados | **175 pruebas, 0 fallos, 0 errores, 0 omitidas** |
| Suite completa `mvnw verify` | **1420 pruebas: 1336 aprobadas, 0 fallos de aserción, 69 errores, 15 omitidas**; `BUILD FAILURE` |
| PostgreSQL 18.6 real, modo monousuario, cluster temporal nuevo | **PASS**; todas las migraciones V1–V27 aplicadas y comprobaciones SQL sin ERROR/FATAL/PANIC |
| Integración nueva `PasswordRecoveryPostgresIntegrationTest` | Compilada; falló en `@BeforeAll` al conectar al PostgreSQL local; sus escenarios JDBC siguen pendientes de ejecución |
| Colección Postman | Preparada, JSON validado; no ejecutada contra un servidor HTTP real en este entorno |
| `git diff --check` | Sin errores |

Corrección de la clasificación tras inspeccionar cada error: 23 fallos directos de sockets, 45 errores en cascada tras fallar contextos de Spring y 1 error por `PHASE2A_DB_URL` ausente. Las 15 omisiones eran condiciones de variables de entorno ausentes, no pruebas aprobadas. Véase [CERTIFICACION.md](CERTIFICACION.md) para evidencia por prueba y la nueva ejecución completa con todas esas condiciones habilitadas. No se añadieron exclusiones ni se deshabilitaron pruebas para obtener un resultado verde.

El ensayo directo de PostgreSQL sobre `127.0.0.1:55439` falló con `could not create IPv4 socket ... Operation not permitted`. La aplicación no se arrancó con credenciales desplegadas. La verificación SQL adicional usa otro cluster temporal sin sockets; no certifica concurrencia entre conexiones ni la integración JPA/JDBC de Spring. PostgreSQL 16 tampoco queda certificado por esa prueba SQL realizada sobre 18.6.

## Evidencia conservada

- [Resultados de la suite completa por clase](full-results.json).
- [Causas raíz de errores por clase](full-error-causes.json).
- [Resultados focalizados por clase](focused-results.json).
- [Salida PostgreSQL real sin sockets](postgres-sql.txt).
- Logs completos locales de trabajo: `/tmp/jaldi-recovery-full-final.log`, `/tmp/jaldi-recovery-focused-final.log`, `/tmp/jaldi-recovery-postgres-sql.log`. Estos paths son temporales.

## Cobertura ejecutada

Las pruebas focalizadas incluyen emisión aleatoria y hash, TTL exacto, envío después del commit, usuarios inexistentes/deshabilitados, consumo válido e inválido, rollback al fallar credenciales, codificación BCrypt y límite UTF-8. También comprueban login real del servicio con contraseña nueva, rechazo de la anterior, endpoints públicos, validación sin devolver secretos, límites por IP sin confiar en X-Forwarded-For, supresión por cuenta para correos conocidos/desconocidos y saturación de cola.

El payload del proveedor también se serializa en una prueba sin red y oculta su token y destinatario en `toString` para proteger los logs DEBUG de RestClient.

La seguridad usa JWT firmados reales en sus pruebas: JWT antiguo rechazado por `/refresh`, JWT actual aceptado, revalidación del servicio ante una carrera con reset, compatibilidad de JWT heredados sin `cv`, y rechazo en rutas protegidas tras el reset. En WebSocket se comprueban CONNECT/SUBSCRIBE y bloqueo de entregas a sesiones previamente suscritas mediante el broker en memoria.

La prueba SQL verifica preservación de todos los campos de usuarios anteriores a V27, unicidad, formato del hash, vencimiento en el límite exacto, hash inexistente, rollback del consumo al fallar la actualización, reemplazo de token, consumo único, cambio de contraseña/versión y borrado en cascada.

## Cobertura preparada que requiere repetir JDBC

`PasswordRecoveryPostgresIntegrationTest` contiene nueve ejecuciones previstas: flujo completo de cliente y comerciante, vencimiento/inexistencia, reemplazo, resets simultáneos con un solo ganador, solicitudes simultáneas con un solo token vigente, rollback real por error de actualización, guardado obsoleto de perfil y preservación de datos al migrar. Los escenarios no se ejecutaron porque la clase falló antes de abrir la conexión.

Para completar esa certificación debe ejecutarse en una máquina que permita sockets locales, contra la base descartable `jaldishop_integration_test`, y luego repetir `./mvnw verify`. La clase valida host loopback, puerto y nombre de base antes de migrar, y simula el correo: no envía mensajes reales.

## Comandos usados

Desde `backend/`, Maven 3 mediante el wrapper, Java 21 y el repositorio Maven local (`-o`). Se indicó `-Dspring.config.import=` para evitar importar `.env`, con variables de proceso que apuntaban exclusivamente a `jdbc:postgresql://127.0.0.1:55439/jaldishop_integration_test`, credenciales ficticias de prueba y correo deshabilitado.

```text
./mvnw -o -B -Dspring.config.import= -DargLine=-javaagent:<byte-buddy-agent-1.18.11.jar> verify
./mvnw -o -B -Dspring.config.import= -DargLine=-javaagent:<byte-buddy-agent-1.18.11.jar> -Dtest=<selección focalizada> test
```

Selección focalizada:

```text
MerchantCategoryControllerTest,MerchantProductControllerTest,MerchantProductVariantControllerTest,
PasswordRecoveryServiceTest,RequestPasswordRecoveryServiceTest,ConfiguredPasswordResetMailTest,
PasswordRecoverySecurityWebTest,JwtAuthenticationFilterTest,RefreshTokenServiceTest,
RegisterMerchantServiceTest,AuthControllerTest,SecurityUserStatusWebTest,
StompAuthChannelInterceptorTest,StompDeliveryAuthorizationBrokerTest,UserTest,
UserPersistenceMapperTest,RateLimitsTest,ClientIpResolverTest
```

El agente de Byte Buddy instalado se usa para Mockito sin depender del attach dinámico restringido por el entorno. No se actualizaron dependencias ni el POM.

Quedan deshabilitados los correos reales. No se modificaron frontend, Render, Neon ni funcionalidades de pagos, pedidos o inventario; no se hicieron commits, push ni despliegues.
