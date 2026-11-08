> Arranque y unitarias actuales: [guía del backend](../../README.md). Los comandos de sandbox y resultados anteriores se conservan como historial; no son requisitos del equipo.

# Customer Address: duplicados

Cambio local en `feature/mercadopago-orders-api`. Solo backend; sin commit, push ni deploy.

## Auditoría y causa raíz

El diagnóstico inicial coincide con el código local: `CreateCustomerAddressService` comprueba máximo cinco y UBIGEO, pero no equivalencia; `UpdateCustomerAddressService` tampoco. El repositorio carece de consulta específica, pero `findByUserId` ya permite comprobar la libreta acotada. V15 contiene únicamente la unicidad parcial de `user_id WHERE is_default = true`; no protege la ubicación. Los tests originales no cubrían duplicados.

Se revisaron modelo, entidad JPA, DTO de alta/actualización, mapper, repositorios, los cinco servicios, controlador, excepciones, V15, las migraciones y documentación del módulo. Los únicos escritores de esta entidad en producción están en los servicios de direcciones. Todos mantienen el bloqueo de la fila del usuario dentro de la transacción.

No se encontró una regla documentada previa de equivalencia física. Hay documentación de dominio anterior que declara `DireccionUsuario` fuera de alcance; no describe el contrato actual de la libreta. La documentación de QA registra el contrato HTTP actual y los errores de negocio 422, pero no define unicidad física.

La entidad, los DTO y V15 no tienen campo de interior independiente. `department` significa departamento geográfico. El formulario actual pide calle, número y departamento/interior en `streetAddress`, con ejemplo `Av. Larco 1234, Dpto. 402, Urb. San Antonio` (`frontend-marketplace/src/features/customer/components/AddressModal.tsx`, consultado sin modificarlo). `reference` es una indicación opcional de entrega.

Había cambios locales de pagos, frontend, Postman y configuración QA. Se conservaron; este cambio no edita esos archivos ni utiliza su configuración de base de datos.

## Regla implementada

La identidad se compara dentro del mismo usuario con:

- UBIGEO idéntico, después del `trim` que ya usa el modelo.
- `streetAddress.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT)` idéntico.

Se conservan tildes, puntuación, números, abreviaturas, orden y el texto del interior. `Dpto. 201` y `Dpto. 202` son diferentes. No se aplican geocodificación, similitud, eliminación de acentos, expansión de abreviaturas ni composición Unicode. `\\s` es la clase ASCII de Java; no se equiparan espacios Unicode como NBSP. La normalización solo sirve para comparar: la presentación almacenada sigue el comportamiento previo del modelo.

Alias, destinatario, teléfono, referencia, coordenadas e `isDefault` no identifican el domicilio. Los nombres geográficos se validan contra el catálogo UBIGEO y no forman una segunda identidad.

POST rechaza equivalentes antes de cambiar predeterminadas o guardar. Conserva la precedencia existente: al alcanzar cinco direcciones devuelve `ADDRESS_LIMIT_EXCEEDED` (422), incluso si el cuerpo también es duplicado; UBIGEO inválido conserva su validación. PUT comprueba colisiones cuando cambia la ubicación y excluye su propio ID. Si la ubicación sigue siendo equivalente, permite editar metadatos y formato, incluso si hay duplicados históricos. No se eliminan ni fusionan registros.

Se usa `BusinessRuleException("ADDRESS_ALREADY_EXISTS", "Ya tienes guardada esta dirección.")`. El manejador existente devuelve HTTP 409 solo para este código; las demás `BusinessRuleException` conservan 422. Se conserva `ApiError(code, message, errors)`; `errors` no contiene errores de campos para este conflicto.

## Persistencia, compatibilidad y migración

No se modifica V15 ni se añade una migración. El bloqueo `SELECT ... FROM users ... FOR UPDATE` sigue precediendo lecturas/escrituras; a aislamiento PostgreSQL READ COMMITTED las transacciones del mismo usuario se serializan y la siguiente consulta ve el resultado confirmado. Se conservan máximo cinco y la lógica existente de predeterminadas.

Una restricción SQL sería necesaria para garantizar unicidad también frente a SQL directo u otros escritores que no respeten estos servicios. Se difiere: un índice único inmediato puede fallar por duplicados históricos; eliminar registros o elegir supervivientes automáticamente violaría el alcance. Además, Java y SQL deben compartir exactamente las reglas de espacios y mayúsculas, sin depender accidentalmente de collations distintas. El cambio actual protege POST/PUT, no impone una garantía global en la base.

Para introducir esa garantía después: inventariar grupos equivalentes en una copia desechable con la misma normalización; revisar interiores y referencias; acordar una resolución explícita sin pérdida de datos; después añadir una nueva migración con precondiciones y probarla con datos limpios e históricos. No desplegar un índice que omita silenciosamente datos históricos.

Limitación del modelo: dos interiores deben distinguirse en `streetAddress`. Si alguien guardó el interior solamente en `reference`, el modelo no permite identificarlo de forma inequívoca; esas altas pueden ser rechazadas como equivalentes. No se extraen interiores de texto libre ni se altera el contrato de DTO en este parche. Los duplicados históricos siguen apareciendo en GET.

## Archivos del cambio

Rutas relativas al backend:

- `src/main/java/com/jaldishop/backend/address/domain/model/CustomerAddress.java`: equivalencia compartida.
- `src/main/java/com/jaldishop/backend/address/application/service/CreateCustomerAddressService.java`: comprobación en POST.
- `src/main/java/com/jaldishop/backend/address/application/service/UpdateCustomerAddressService.java`: comprobación en PUT.
- `src/main/java/com/jaldishop/backend/shared/exception/GlobalExceptionHandler.java`: mapeo específico 409.
- `src/test/java/com/jaldishop/backend/address/application/service/CreateCustomerAddressServiceTest.java`.
- `src/test/java/com/jaldishop/backend/address/application/service/UpdateCustomerAddressServiceTest.java`.
- `src/test/java/com/jaldishop/backend/address/web/controller/CustomerAddressControllerTest.java`.
- `src/test/java/com/jaldishop/backend/address/domain/model/CustomerAddressTest.java` (nuevo).
- `src/test/java/com/jaldishop/backend/address/application/service/CustomerAddressPostgresIntegrationTest.java` (nuevo).
- `qa/customer-address-duplicates/`: este informe y colección Bruno (9 requests, configuración y entorno sin credenciales).

## Pruebas y resultados

Ejecución focalizada: **51 pruebas pasaron, cero fallos, errores u omitidas**:

| Clase | Casos |
| --- | ---: |
| CreateCustomerAddressServiceTest | 8 |
| UpdateCustomerAddressServiceTest | 4 |
| DeleteCustomerAddressServiceTest | 2 |
| SetDefaultCustomerAddressServiceTest | 2 |
| CustomerAddressControllerTest | 6 |
| CustomerAddressTest | 7 |
| GlobalExceptionHandlerTest | 22 |

Incluyen creación nueva, POST idéntico, mayúsculas/espacios/tabuladores, interior distinto, ámbito por usuario, PUT duplicado sin efectos secundarios, actualización de la ubicación propia, límite cinco, comportamiento de predeterminadas y HTTP 409 en POST/PUT. Las pruebas de dominio comprueban que tildes, puntuación, abreviaturas, números y UBIGEO distintos no colisionan.

Desde `backend/`:

```bash
./mvnw -q \
  -Dtest=CreateCustomerAddressServiceTest,UpdateCustomerAddressServiceTest,DeleteCustomerAddressServiceTest,SetDefaultCustomerAddressServiceTest,CustomerAddressControllerTest,CustomerAddressTest,GlobalExceptionHandlerTest \
  test
```

La ejecución histórica usó un agente Mockito por restricciones del entorno; no es un requisito del equipo. No se cambió el POM. Log de aquella ejecución: `/tmp/jaldi-address-final-tests.log`.

**PostgreSQL real:** se inicializó un clúster nuevo en `/tmp/jaldi-address-pgdata`, usando binarios locales PostgreSQL 18.6. En modo monousuario se ejecutó V15 sin cambios, una tabla mínima `users(id)` y datos sintéticos. Resultado: dos domicilios iguales aceptados, una sola predeterminada y rechazo por unique de una segunda predeterminada. SQL y evidencia: `/tmp/jaldi-address-v15.sql`, `/tmp/jaldi-address-v15.log`. No se aplicó el resto de migraciones ni se accedió a una base persistente.

**Concurrencia real pendiente:** se intentó ejecutar `CustomerAddressPostgresIntegrationTest`. El arranque falla con `java.net.SocketException: Operation not permitted` al reservar un puerto local. PostgreSQL también fue rechazado al crear su socket TCP. Los tres escenarios de integración no llegaron a ejecutarse; no se declara probada la concurrencia. Log: `/tmp/jaldi-address-postgres-test.log`.

La clase crea su propio clúster temporal con `initdb`, aplica V15 y configura únicamente el módulo address con JPA/transacciones reales. No lee `.env`, `DB_URL`, perfiles ni configuración de la aplicación. Al habilitarla, un fallo de infraestructura falla la prueba. Sin `ADDRESS_TEST_PG_BIN` se omite explícitamente. Escenarios: equivalencia y usuarios distintos, ocho POST iguales concurrentes (un éxito), y ocho POST distintos concurrentes (cinco éxitos y tres límites), con una única predeterminada.

Para repetir en un entorno que permita sockets locales, desde `backend/`, con PostgreSQL instalado y sin ejecutar como root:

```bash
ADDRESS_TEST_PG_BIN=/ruta/a/postgresql/bin \
  ./mvnw -Dtest=CustomerAddressPostgresIntegrationTest test
```

Si los binarios requieren librerías adicionales, configurar `LD_LIBRARY_PATH` para esas librerías. La prueba arranca y detiene únicamente su proceso PostgreSQL y JUnit limpia su directorio temporal.

No se ejecutó la suite completa: contiene integraciones de módulos ajenos y exige bases externas configuradas. No se conectó a Neon ni Render, no se ejecutaron migraciones remotas y no se alteraron checkout ni Mercado Pago.

## Bruno: validación manual

Abrir esta carpeta como colección Bruno. Seleccionar `Local disposable`, indicar tokens de dos CUSTOMER distintos, inicialmente sin direcciones. `baseUrl` debe apuntar exclusivamente a un backend conectado a una base desechable verificada. Esta colección crea datos; no utilizarla contra bases persistentes. Los casos no se ejecutaron aquí porque no pudo levantarse el backend con PostgreSQL por la restricción de sockets.

Ejecutar en orden:

| Caso | Resultado esperado |
| --- | --- |
| 01 Crear dirección | POST 201; guarda `firstAddressId` |
| 02 Repetir POST | 409 `ADDRESS_ALREADY_EXISTS` |
| 03 Mayúsculas y espacios, otros metadatos | 409 `ADDRESS_ALREADY_EXISTS` |
| 04 Otro interior | POST 201; guarda `secondAddressId` |
| 05 Mismo domicilio, otro usuario | POST 201 |
| 06 GET | 200; dos direcciones, máximo cinco, una predeterminada |
| 07 PUT segunda a ubicación de primera | 409 `ADDRESS_ALREADY_EXISTS` |
| 08 PUT primera, misma ubicación normalizada | 200; alias actualizado |
| 09 GET final | 200; siguen dos direcciones y una predeterminada |

No se incluyen DELETE ni limpieza automática. Para repetir, usar dos usuarios nuevos en el entorno desechable.
