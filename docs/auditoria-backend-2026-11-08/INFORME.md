# Auditoría del backend JaldiShop — 8 de noviembre de 2026

**Decisión: BLOQUEADO para integrar el árbol actual en main.** Compila, pero la suite completa contiene errores, el QA experimental es incompatible con V26 y las pruebas críticas de persistencia/concurrencia no pudieron ejecutarse. No se hicieron correcciones de producto ni se modificaron pruebas. No se ejecutaron commit, merge, push, deploy ni operaciones en Neon, Render o Mercado Pago real.

## 1. Estado e inventario Git

- Repositorio: `/home/ijosueeh/Documentos/Proyectos/jaldishop`.
- Rama: `feature/mercadopago-orders-api`.
- HEAD: `362e903b922904c032ef56bc00cfe7660e39b4fe`.
- main local: `e9352eddc4eb42b8e4083a470f482c45924829b9` (último commit del 27 de octubre). No se hizo fetch; no se certifica que coincida con el main remoto actual.
- Divergencia `main...HEAD`: 0 commits exclusivos de main, 28 exclusivos de la rama. Los commits se listan en [commits.txt](commits.txt).
- Diferencia de archivos rastreados contra main, incluyendo modificaciones locales: **764 archivos, +96.218 / −4.677 líneas**. No incluye archivos nuevos sin seguimiento.
- Estado inicial: **55 archivos rastreados modificados y 115 archivos nuevos: 170 entradas**. Ningún cambio estaba preparado en el índice. De los rastreados modificados, 20 están en backend.
- Hay movimientos de paquetes en todos los módulos, inventario, cancelaciones, E2E y documentación, además de Mercado Pago. No es un diff limitado a las cuatro correcciones.

Inventarios completos, con una fila por archivo:

- [inventario-local.tsv](inventario-local.tsv): los 170 archivos locales previos a esta auditoría, estado y clasificación.
- [diferencias-main.tsv](diferencias-main.tsv): las 764 diferencias rastreadas contra main, con rutas anteriores/nuevas cuando Git identifica movimientos.
- [diff-check.txt](diff-check.txt): advertencias de espacios finales y líneas vacías al final. `git diff --check main` no pasa; no se corrigieron.

Clasificación del estado local inicial:

| Grupo | Archivos |
|---|---:|
| Direcciones | 8 |
| HTTP / seguridad / rutas | 4 |
| Mercado Pago y pruebas | 8 |
| Tiendas / búsqueda / Merchant Customers / V26 | 10 |
| QA experimental, scripts, fixtures y colecciones de QA | 77 |
| Frontend Merchant | 42 |
| Postman / entornos | 5 |
| Documentación previa | 12 |
| Temporales / herramientas Postman | 3 |
| .gitignore | 1 |

La clasificación indica el ámbito de revisión; no autoriza incluir automáticamente todos los archivos de un grupo en un commit. Este directorio de informe es la única incorporación nueva de la auditoría al repositorio.

### Archivos sensibles y artefactos

Se revisaron archivos rastreados y nuevos no ignorados con patrones de JWT, tokens de Mercado Pago/GitHub, claves privadas y URLs con credenciales, sin imprimir valores. Los dos candidatos encontrados son datos sintéticos negativos de pruebas: JWT deliberadamente inválido en `WebSocketStompRealIntegrationTest.java:217` y URL de ejemplo en `QaRemotePolicyTest.java:81`. No se identificaron credenciales reales con esos patrones. Esto no equivale a un escaneo de secretos de todo el historial.

Los campos de token/password de los tres environments JSON de Postman están vacíos. `postman/jaldishop.render.env.json` es configuración de destino sensible desde el punto de vista operativo: no se utilizó. Revisar su inclusión separadamente, junto con las URLs de despliegue. `.env` está ignorado; no se cargó ni se copió a la ejecución aislada. Revisar siempre archivos `.env*` que no cubra la regla exacta `.env`.

Excluir de commits de producto `postman/jaldishop.postman_collection.json.bak`, logs, `target/`, clusters temporales y reportes crudos de ejecución. Las herramientas de `postman/tools/` requieren revisión separada; no son necesariamente basura. Los datos QA y documentos acumulados tampoco deben añadirse con `git add .`.

### Dependencia de inherited-branch

El código nuevo `shared/config/qa`, el perfil `application-qa.properties` y `db/qa` están sin seguimiento. Maven los compila/empaqueta desde el árbol actual. Las configuraciones de datasource y seguridad QA tienen `@Profile("qa")`; no se encontraron imports de esas clases en los módulos funcionales fuera de ese paquete. No se detectó dependencia de arranque ordinario hacia inherited-branch.

Sí existe acoplamiento accidental en la validación: QA inspecciona el mismo directorio de migraciones de producto y exige un número fijo. Su CLI incluye sanitización/cuarentena; debe tratarse como herramienta operativa independiente y revisarse antes de distribuirse dentro del JAR. No se ejecutó esa CLI.

## 2. Ejecución y resultados exactos

Entorno: Java Temurin 21.0.13; Spring Boot 4.1.1; Maven offline con dependencias ya disponibles. Se creó una copia de backend bajo `/tmp/jaldi-audit-20261108/backend`, excluyendo `.env*` y `target`. Se compiló desde cero allí: **572 archivos fuente, BUILD SUCCESS**, 8,442 s. El compilador avisa de APIs obsoletas; no impide la compilación.

Las ejecuciones de pruebas usaron un entorno reducido sin DB_URL/PHASE2A_DB_URL ni credenciales heredadas, `spring.config.import=` y un javaagent Byte Buddy local. La URL general de Mercado Pago se fijó a loopback puerto 9. Los clientes de pago de las pruebas usan mocks/servidores de prueba; no se habilitó la prueba manual que lee una base persistente. Los JSON `*-command.json` contienen los argumentos exactos; logs completos y XML originales están en `/tmp/jaldi-audit-20261108`.

| Ejecución | Total reportado | Aprobadas | Fallos de aserción | Errores | Omitidas | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Selección sin clases que requieren DB externa al test | 1.346 | 1.312 | 0 | 20 | 14 | BUILD FAILURE |
| **Suite completa, sin filtro de clases** | **1.426** | **1.327** | **0** | **83** | **16** | **BUILD FAILURE** |
| Focalizadas: cuatro correcciones + guarda de DB | 100 | 100 | 0 | 0 | 0 | BUILD SUCCESS |
| Tres clases JDBC/JPA habilitadas expresamente | 3 | 0 | 0 | 3 | 0 | BUILD FAILURE de preparación |
| Regresión SQL con PostgreSQL 18.6 monousuario | 21 casos SQL | 21 | 0 | 0 | 0 | PASS SQL; no JDBC/JPA |

Los resultados de ejecuciones diferentes se superponen y **no deben sumarse**. Surefire puede reportar un solo error de preparación por clase aunque ésta contenga varios métodos: los tres errores JDBC no significan que tres escenarios de negocio llegaron a ejecutarse.

La selección inicial excluyó las clases listadas en [unit-exclusions.json](unit-exclusions.json), incluida `TestPostgresConfigTest` por la selección conservadora de referencias a configuración. Sus 14 casos sí se ejecutaron en la suite completa y en las focalizadas. Se conserva esta distinción para que la primera selección no se confunda con toda la suite. No se cambiaron anotaciones ni se deshabilitaron pruebas.

Anexos de resultados por clase: [suite completa](full-results.json), [primera selección](unit-results.json), [focalizadas](focused-results.json), [preparación PostgreSQL](postgres-results.json).

La cifra histórica de 1.322 aprobadas, 20 errores y 2 omitidas no se reutilizó como evidencia actual. El árbol y la selección actual incluyen pruebas nuevas y más condiciones de integración.

### Causa demostrada de los 20 errores de QaInheritedBranchTest

1. Hay **25 scripts**: V1–V8 y V10–V26. V9 tampoco existe en main; el salto es histórico y no se añadió en esta auditoría.
2. `QaInheritedSchema.java:32` exige `expected.size() == 24` y presencia de V25.
3. V26 incrementa el conjunto de 24 a 25. Los nombres son válidos, no hay versiones duplicadas y los checksums se calcularon sin error con el algoritmo de la versión instalada de Flyway.
4. La invocación offline de `QaInheritedSchema.expected()` reproduce `IllegalStateException`, sin JDBC; véase `/tmp/jaldi-audit-20261108/schema-guard.log` y `MigrationAudit.java`.
5. Las trazas de los 20 errores llegan a esa guarda desde `prepared()` o desde la preparación offline de acciones. Las pruebas usan JDBC mock. No es un fallo de credenciales reales, red ni PostgreSQL.

Es una incompatibilidad real entre QA experimental y la nueva migración. **No debe borrarse V26, cambiar expectativas o excluir QaInheritedBranchTest para declarar verde el árbol actual.** Corresponde corregir el contrato del componente QA con revisión y pruebas, o definir una entrega de producto que no lo incluya, verificando nuevamente el artefacto resultante. Ninguna de esas modificaciones se realizó aquí.

### Los otros 63 errores y las 16 omisiones

Los 63 errores restantes corresponden a preparación JDBC/Spring: faltan DB_URL/PHASE2A_DB_URL y credenciales de bases desechables. Los errores derivados de inicializadores/contextos fallidos no prueban fallos de negocio. No se sustituyeron por credenciales persistentes.

Las 16 omisiones son: 8 casos de búsqueda PostgreSQL, 3 de Merchant Customers PostgreSQL, 3 de direcciones PostgreSQL, 1 de `RemoteOrderCreationMigrationTest` condicionado a PHASE2A_DB_URL y 1 de `SingleAttemptIsolatedReconciliationTest`, condicionado a autorización manual. Se habilitaron después las tres primeras clases, obteniendo los errores de socket descritos abajo. La prueba manual no se autorizó ni ejecutó.

### Regresión por área en la suite completa

| Área | Aprobadas | Errores | Omitidas |
|---|---:|---:|---:|
| Identidad / autenticación / permisos | 132 | 1 | 0 |
| Checkout | 44 | 2 | 0 |
| Pagos | 330 | 28 | 2 |
| Inventario | 32 | 3 | 0 |
| Capacidad | 184 | 0 | 0 |
| Pedidos | 75 | 8 | 0 |
| Realtime / WebSocket | 26 | 18 | 0 |
| Direcciones | 33 | 0 | 3 |
| Tiendas / Merchant Customers | 103 | 0 | 11 |

Son subconjuntos del total; shared y otros módulos se detallan en el JSON. Los errores de esta tabla son de infraestructura de pruebas. Los 20 de QA están en shared. No se observaron fallos de aserción en lo que logró ejecutarse; queda pendiente la regresión con persistencia real.

## 3. Cuatro correcciones e integración PostgreSQL

### Direcciones duplicadas

La identidad se compara por UBIGEO y calle normalizada por espacios y minúsculas `Locale.ROOT`; se conserva la distinción de interiores incluida en la calle. Los servicios de creación y actualización verifican duplicados y responden `ADDRESS_ALREADY_EXISTS`, convertido a HTTP 409. Se permite editar metadatos de duplicados históricos cuando no cambia la ubicación.

Crear, actualizar, eliminar y cambiar predeterminada toman el bloqueo de la fila del usuario mediante `SELECT ... FOR UPDATE`, dentro de transacciones. Esto proporciona serialización entre esos caminos de aplicación; **no hay índice único de ubicación normalizada en la base**. El índice único de V15 protege solo una dirección predeterminada por usuario. Escrituras que evadan esos servicios no están cubiertas por la unicidad lógica.

Las pruebas focalizadas de dominio, servicios y controlador pasan. Las carreras de 8 creaciones iguales, límite de cinco direcciones y única predeterminada existen en `CustomerAddressPostgresIntegrationTest`, pero **no se ejecutaron** por el bloqueo de sockets. No se afirma que la concurrencia esté certificada.

### Búsqueda de tiendas

La consulta JPQL usa `public.unaccent`, minúsculas y `LOCATE`, preservando `%`, `_` y barra invertida como texto literal. El servicio público filtra ACTIVE. La migración V26 crea la extensión en public sin reescribir nombres/slugs. La validación de la consulta con Hibernate y las pruebas HTTP internas pasan.

El script existente `qa/public-store-search/verify-postgres.py` creó un cluster nuevo con `initdb` y usó `postgres --single`, sin servidor TCP. Aplicó los 25 scripts ordenados hasta V26 y ejecutó 21 casos SQL, incluyendo reproducción del comportamiento previo, acentos, caracteres literales, estado y límites. Resultado: **exit 0, marcador PASS, sin ERROR/FATAL/PANIC**. Versión real: PostgreSQL 18.6. El CI declara PostgreSQL 16; esta prueba adicional no certifica esa versión ni el SQL emitido por JPA.

Pendiente: permisos CREATE EXTENSION y que `unaccent` esté instalado efectivamente en public en el destino. Si una instalación previa lo tiene en otro esquema, `IF NOT EXISTS` no lo traslada. No se inspeccionó ningún destino persistente.

### Normalización HTTP

El handler conserva `Allow` en 405, añade manejo de `NoHandlerFoundException` y responde 409 a direcciones duplicadas. SecurityConfig añade JSON 400 para rechazo del firewall. Pasan las pruebas de rutas de favoritos, handlers y controladores. La suite de seguridad también ejecuta los casos de autenticación y roles con mocks/MockMvc. HTTP real no quedó validado por la restricción TCP.

### Merchant Customers e Instant

La proyección devuelve `Instant` y el adaptador deja de exigir conversiones desde `Timestamp`. Pasan la cadena de proyección Spring → adaptador → servicio → controlador protegido → JSON, fechas precisas/null, aislamiento de tienda y permisos en pruebas internas.

La conversión real de `TIMESTAMPTZ` a `Instant` por Hibernate/JDBC está cubierta en la clase PostgreSQL pendiente; no se sustituyó por una afirmación basada solo en mocks. También siguen pendientes las agregaciones y el aislamiento con datos reales de esa prueba. El contrato actual cuenta pedidos cancelados en ordersCount/lastOrderAt pero excluye sus importes de totalSpent; la prueba lo documenta. No se cambió esa semántica.

### Flyway, checksums y entorno desechable

- [flyway-checksums.csv](flyway-checksums.csv): checksum Flyway offline de cada uno de los 25 scripts. V26: **778257244**.
- [migration-manifest.json](migration-manifest.json): SHA-256 y comparación con HEAD/main. Los scripts históricos presentes en main son idénticos byte a byte; V1–V25 no tienen modificaciones locales frente a HEAD. V16–V25 pertenecen a los commits de la rama; V26 es nueva sin seguimiento.
- Aplicación SQL en cluster nuevo: PASS con PostgreSQL 18.6 monousuario.
- **Flyway migrate/validate sobre JDBC y comprobación de `flyway_schema_history`: PENDIENTE**. La ejecución monousuario no genera historial Flyway ni comprueba checksums contra una base migrada.
- Una prueba directa de socket IPv4 falla con `PermissionError errno 1`. Se localizaron binarios PostgreSQL 18 disponibles localmente y se habilitaron las tres clases con `ADDRESS_TEST_PG_BIN`, `STORE_SEARCH_TEST_PG_BIN` y `STORE_CUSTOMERS_TEST_PG_BIN`; las tres completan initdb pero fallan en `new ServerSocket` con `java.net.SocketException: Operation not permitted` antes de abrir JDBC.
- No se solicitó eludir las restricciones del entorno. Los datos previos de `/tmp` no se reutilizaron; solo bibliotecas/binarios existentes. Las instancias de validación se crearon en directorios temporales nuevos.

### Seguridad de las pruebas y riesgos financieros pendientes

Las pruebas generales usan `TestPostgresConfig`, que exige loopback, puerto explícito, nombre `jaldishop_integration_test` o `jaldishop_fase2a_test`, y rechaza destinos remotos/parámetros. Esa guarda por sí sola **no prueba que una base con ese nombre sea desechable**. Algunas fixtures hacen DELETE/TRUNCATE y requieren control adicional de propiedad/ciclo de vida. En esta ejecución no recibieron conexiones.

La prueba manual `SingleAttemptIsolatedReconciliationTest` puede leer configuración persistente si se autoriza explícitamente; quedó omitida. Las tres clases nuevas administran su propio cluster y no leen `.env`. El único acceso PostgreSQL efectivo fue al cluster monousuario nuevo; **ninguna prueba ejecutada modificó una base persistente**.

Se revisaron la validación compartida de identidad remota, referencia del intento, rechazo de múltiples pagos ambiguos, preservación de estados financieros y cambios locales del adaptador Orders. Los tests disponibles de conciliación, webhook, investigación y respuesta canónica/legacy pasan en memoria. El nuevo paidAmount se conserva como evidencia; no se observó consumidor financiero de ese campo, por lo que no se afirma una validación adicional del importe efectivamente abonado. Falta demostración de transacciones, leases, idempotencia, compensación de inventario/capacidad y concurrencia con PostgreSQL. No se ejecutaron pagos ni reembolsos reales.

La validación de collector/live_mode depende de `expectedCollectorId`/`expectedLiveMode`, propiedades opcionales: cuando son null esos controles no se aplican. Revisar su configuración explícita en una fase de despliegue autorizada; no se leyeron secretos de destino. `spring.flyway.validate-on-migrate` tiene default false en la configuración actual: ejecutar la futura validación con true, sin reparar historial automáticamente.

## 4. Auditoría Admin — exclusivamente estática

Todos los controladores siguientes tienen `@PreAuthorize("hasRole('ADMIN')")`; la cadena HTTP también exige ADMIN para `/api/v1/admin/**`.

| Ruta base | Operaciones disponibles |
|---|---|
| `/api/v1/admin/users` | GET lista; GET `/{id}`; PATCH `/{id}/suspend`; PATCH `/{id}/activate`; PATCH `/{id}/roles` |
| `/api/v1/admin/stores` | GET lista; GET `/{id}`; PATCH `/{id}/suspend`; PATCH `/{id}/activate` |
| `/api/v1/admin/payments/attempts` | POST `/{attemptId}/reconcile-order`; POST `/{attemptId}/investigate-order` |

Los endpoints de pagos son nuevos frente a main y pertenecen a los commits de Mercado Pago. Son una ampliación de superficie administrativa financiera que debe identificarse explícitamente al revisar ese bloque; no son cambios implementados en esta auditoría. Los controladores generales de usuarios/tiendas conservan sus operaciones frente a main; sus diferencias revisadas son cambios de imports por el refactor de paquetes.

### Suspensión, activación y permisos

- `ChangeUserStatusService` y `ChangeStoreStatusService` son transaccionales y llaman a `repository.save`: hay persistencia implementada, aunque su ejecución real quedó pendiente.
- Usuario: se impide autosuspensión cuando se identifica al administrador; se rechazan estados ya activos/suspendidos. `UpdateUserRolesService` impide quitarse el propio rol ADMIN. `JwtAuthenticationFilter` consulta el usuario y sus roles actuales en cada petición y rechaza usuarios no activos; ante fallo de infraestructura responde 503. Esto evita confiar solo en roles antiguos del JWT para HTTP.
- Tienda: se rechazan suspensiones/activaciones repetidas. Búsqueda pública y lectura pública por slug exigen ACTIVE; checkout y confirmación vuelven a comprobar ACTIVE. La suspensión no elimina el negocio ni compensa automáticamente pedidos/pagos en curso.
- `StoreContextService` valida rol y propiedad, pero no exige ACTIVE de forma general: una tienda suspendida todavía puede acceder a caminos Merchant que solo usan ese contexto. Debe definirse qué operaciones de gestión/atención se permiten en suspensión; no se interpreta todo acceso como vulnerabilidad ni se cambió su política.

### Hallazgos y trabajo posterior

1. **Configuraciones que aparentan persistir — confirmado.** `frontend-merchant/src/app/core/services/admin.service.ts:320` actualiza una signal; `saveSettings():324` solo muestra un toast de éxito. No hay llamada HTTP ni almacenamiento duradero. Registro de merchants, aprobación de tiendas, mantenimiento, tiempo de sesión, 2FA y avisos configurables en esa pantalla no constituyen controles efectivos por guardar ahí. No se encontró controlador backend de configuración de plataforma. No confiar en el toggle 2FA como protección implementada.
2. **Auditoría administrativa pendiente.** En los servicios revisados no se observa un registro persistente de actor, motivo y antes/después para estos cambios. Activación de usuario y cambios de tienda no reciben actor en sus comandos. Planificar trazabilidad y autorización operativa para los endpoints financieros.
3. **Política de transiciones pendiente.** Activar asigna ACTIVE desde cualquier estado no ACTIVE; no hay una matriz explícita de reapertura/rehabilitación. Definir cómo tratar CLOSED/INACTIVE y qué efectos debe producir la suspensión en operaciones ya iniciadas.
4. **Consistencia concurrente pendiente.** Las protecciones locales de autosuspensión/autorretiro de ADMIN no son una garantía global contra dos administradores que se suspendan o retiren roles mutuamente en paralelo. Revisar el invariante de al menos un administrador activo y la concurrencia de cambios de permisos; no se implementó nada.
5. **Escalabilidad pendiente.** Las listas de usuarios/tiendas devuelven List, sin paginación en esos controladores; las métricas frontend derivadas de esas listas crecerán con el volumen. Planificar paginación y métricas de servidor.

No se implementaron ajustes Admin, 2FA, persistencia de configuración, endpoints nuevos ni migraciones de administración.

## 5. Decisión y condiciones de salida

**BLOQUEADO**, por el estado concreto del candidato actual:

- La suite completa falla. Los 20 errores de QaInheritedBranchTest tienen causa demostrada, pero el defecto sigue presente en código que se compila/empaqueta desde este árbol.
- No se han ejecutado las pruebas críticas JDBC/JPA, concurrencia e historial Flyway. Los mocks y PostgreSQL monousuario aportan evidencia parcial; no bastan para certificar la integración financiera y de reservas.
- El candidato mezcla 28 commits de varios módulos, 170 cambios locales y QA experimental. Se inventarió todo el conjunto y se revisaron estáticamente los caminos críticos descritos, los cambios locales de backend y Admin; **no se certifica revisión línea por línea de las 96.218 líneas añadidas**, ni validación de los 42 archivos locales frontend. Eso debe completarse sobre una entrega delimitada.

No se etiqueta como APROBADO CON LIMITACIONES porque el candidato incluye una incompatibilidad reproducible de código QA, además de las limitaciones del entorno. Que los fallos sean explicables no convierte la suite en aprobada.

Para reevaluar:

1. Delimitar un árbol de entrega identificable, separar QA experimental y frontend, o corregir el contrato QA con pruebas válidas. Conservar siempre el trabajo existente.
2. En un runner con sockets, crear PostgreSQL desechable de la versión objetivo, con directorio/volumen nuevo, identificador de ejecución y credenciales exclusivas; no aceptar solo un nombre de DB como evidencia de aislamiento.
3. Crear separadamente las bases general y Phase2A exigidas por las guardas. Forzar `FLYWAY_VALIDATE_ON_MIGRATE=true`; migrar desde vacío y validar los checksums contra `flyway_schema_history`. Probar también la actualización del esquema base de main con sus scripts inalterados.
4. Ejecutar todos los tests JDBC, E2E HTTP/WebSocket y los 14 casos de las tres clases nuevas. Aportar binarios PG compatibles con la versión objetivo para las clases que crean clusters. Verificar extensión y esquema `public.unaccent`, Instant real, duplicados y límite/predeterminada bajo concurrencia.
5. Ejecutar nuevamente la suite completa sobre el SHA/árbol exacto candidato, con mocks locales de pagos y sin autorización de la prueba manual persistente. Documentar esa prueba fuera de la suite de integración. No aceptar errores ni omisiones críticas sin ejecución alternativa demostrada.
6. Completar revisión de diff del candidato y resolver las advertencias de `diff --check` en una tarea autorizada. La configuración CI incluye `continue-on-error` para subir reportes, pero tiene un paso posterior que verifica el outcome y falla; no se observó un bypass automático de errores en ese punto.

## 6. Organización propuesta de commits y actualización segura de main

**Es una propuesta; no se ejecutó ningún comando de integración.** No reescribir los 28 commits existentes ni hacer staging masivo del árbol actual.

Para el trabajo local todavía sin confirmar, proponer unidades revisables:

1. `fix(address): reject duplicate customer locations`: dominio, creación/actualización, contrato 409 y pruebas de direcciones. El hunk de 409 del handler comparte archivo con el bloque HTTP: preparar por hunk para que cada commit sea coherente.
2. `fix(store): normalize public search with unaccent`: consulta, **V26 en el mismo bloque**, pruebas de consulta/controlador/PostgreSQL. No publicar la consulta sin la extensión.
3. `fix(http): preserve routing error contracts`: resto del handler, `RequestRejectedHandler`, rutas y pruebas HTTP. Respetar la dependencia del contrato 409 del bloque de direcciones.
4. `fix(store): map merchant customer timestamps as Instant`: proyección, adaptador y pruebas de proyección/HTTP/JDBC.
5. `fix(payment): validate canonical and legacy orders responses`: DTOs, normalización y pruebas de investigación, conciliación, webhook y cliente; incluir los fixtures necesarios de `src/test/resources/mercadopago`. Revisión financiera propia.
6. **Rama/entrega separada de QA experimental**: `shared/config/qa`, perfil, fixtures SQL, herramientas y pruebas QA. Separar su incompatibilidad con el catálogo de migraciones; no moverla silenciosamente fuera de la suite para hacer pasar el candidato.
7. Frontend Merchant, notificaciones/STOMP y sus dependencias: entrega independiente con su propia validación. Postman/documentación: commits específicos, sin backup, datos sensibles ni resultados crudos. El informe actual puede ir en un commit documental independiente cuando se autorice.

Los grupos locales dependen de refactors y funcionalidades que ya están en la historia de esta rama. No hacer cherry-pick aislado a main asumiendo que los paquetes/contratos existen allí. Inventario, cancelaciones, capacidades, refactor de paquetes y Mercado Pago deben aparecer como dependencias explícitas en el alcance que se quiera integrar.

Procedimiento futuro, después de resolver los bloqueos y obtener autorización de la fase de publicación:

1. Respaldar y registrar el estado exacto completo, incluidos archivos nuevos, sin reset/clean/stash destructivo. Confirmar los commits aprobados únicamente por rutas/hunks revisados y conservar el resto del trabajo en su rama/checkout.
2. Actualizar referencias remotas y comparar de nuevo con el main remoto real; el análisis actual solo conoce el main local del 27 de octubre.
3. Crear un worktree limpio de integración desde ese main actualizado. Aplicar el conjunto revisado y su cadena de dependencias; resolver conflictos allí, preservando el checkout con trabajo acumulado.
4. Revisar el diff efectivo final, escanear secretos, verificar que QA experimental/temporales no se filtraron al JAR y ejecutar desde cero los gates sobre el commit final en ese worktree.
5. Preparar PR con alcance, migraciones, evidencia exacta y pendientes Admin separados. Exigir los checks y revisión humana antes del merge autorizado; si main cambia, repetir las validaciones afectadas sobre el nuevo candidato.
6. La aplicación de migraciones, publicación y despliegue requieren la fase independiente anunciada por el usuario. No hacer Flyway repair ni editar checksums/historial para forzar compatibilidad.

## 7. Evidencia y preservación

- Los argumentos exactos están en [full-command.json](full-command.json), [unit-command.json](unit-command.json), [focused-command.json](focused-command.json) y [postgres-command.json](postgres-command.json). Maven escribió los reportes en `target/surefire-reports`; se copiaron por ejecución/clase para conservar evidencia. Los paths de reportDirectory solicitados en los comandos no fueron efectivos con la configuración actual; los anexos proceden del directorio real.
- Logs completos: `/tmp/jaldi-audit-20261108/fresh-compile.log`, `unit.log`, `full.log`, `focused.log`, `postgres.log`, `single-user-search.log`, `schema-guard.log`. Los XML se preservaron en `unit-reports`, `full-reports`, `focused-reports` y `postgres-reports` dentro de ese directorio temporal. No se añaden al repositorio.
- Antes de crear este informe, `git status` y el parche binario de cambios rastreados coincidieron exactamente con las capturas iniciales. Los **923 archivos de backend** copiados, excluyendo target y secretos, se compararon byte a byte con el original: **cero diferencias**. La compilación inicial en el checkout solo actualizó artefactos ignorados de target; las ejecuciones posteriores se realizaron en la copia.
- La única diferencia adicional de fuentes/control documental al cerrar es `docs/auditoria-backend-2026-11-08/`. Se conserva todo el trabajo previo.

**La auditoría termina aquí. Publicación e integración no autorizadas ni ejecutadas.**
