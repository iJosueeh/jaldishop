> Arranque y unitarias actuales: [guía del backend](../../README.md). Los comandos de sandbox y resultados anteriores se conservan como historial; no son requisitos del equipo.

# Corrección de GET /api/v1/merchant/customers

Rama: `feature/mercadopago-orders-api`. Solo backend; cambios locales previos conservados.

## Causa y cambio

`users.created_at` y `orders.confirmed_at` son `TIMESTAMPTZ` (V1). La consulta nativa devuelve `u.created_at` y `MAX(o.confirmed_at)` sin cast. En Render, Hibernate entrega `Instant`; Spring Data intenta satisfacer los getters `Timestamp` y falla antes de que el adaptador pueda ejecutar `.toInstant()`.

La prueba `StoreCustomerRepositoryAdapterTest` usa una proyección real de `SpelAwareProxyProjectionFactory`, respaldada por valores `Instant`, y reprodujo exactamente `Cannot project java.time.Instant to java.sql.Timestamp` antes del cambio. No se mockean los getters de la proyección.

También se inspeccionó el bytecode de Hibernate 7.4.5.Final instalado: `PostgreSQLDialect.resolveSqlTypeDescriptor` reconoce `timestamptz` reportado como JDBC TIMESTAMP y lo resuelve como `TIMESTAMP_UTC` (3003); `TimestampUtcAsInstantJdbcType` recomienda `Instant`. Esto confirma el mecanismo y coincide con el error productivo; no sustituye ejecutar la consulta por JDBC.

Cambios productivos:

- `StoreCustomerProjection`: `customerSince` y `lastOrderAt` retornan `Instant`.
- `StoreCustomerRepositoryAdapter`: pasa ambos valores directamente al dominio.

El servicio, dominio, mapper, DTO y controlador fueron inspeccionados y no requieren cambios. Dominio y HTTP ya utilizan `Instant`. Se conserva JSON ISO 8601 UTC, precisión fraccionaria y `null` cuando no hay último pedido. No se encontraron otras proyecciones productivas con `Timestamp` ni otro tipo temporal con ese riesgo en la búsqueda del backend.

## Archivos del parche

- Modificado: `src/main/java/com/jaldishop/backend/store/infrastructure/persistence/projection/StoreCustomerProjection.java`.
- Modificado: `src/main/java/com/jaldishop/backend/store/infrastructure/persistence/adapter/StoreCustomerRepositoryAdapter.java`.
- Nuevo: `src/test/java/com/jaldishop/backend/store/infrastructure/persistence/adapter/StoreCustomerRepositoryAdapterTest.java`.
- Nuevo: `src/test/java/com/jaldishop/backend/store/infrastructure/persistence/repository/StoreCustomerPostgresIntegrationTest.java`.
- Nuevo: `src/test/java/com/jaldishop/backend/store/web/controller/MerchantCustomerProjectionHttpTest.java`.
- Nueva carpeta: `qa/merchant-customers/` (este informe, colección Bruno, entorno sin credenciales y doce solicitudes).

## Semántica y dinero

La consulta permanece intacta:

- Clientes asociados mediante `user_roles`, `role_id = 1`, para la tienda del merchant autenticado. Tener un pedido sin esa asociación no incorpora al usuario a la lista.
- `customerSince` sigue siendo la creación global del usuario (`users.created_at`), no una fecha de asociación a la tienda.
- El JOIN de pedidos restringe simultáneamente usuario y tienda.
- `ordersCount` cuenta todos los pedidos; `totalSpentAmount` suma `orders.total_amount` salvo los cancelados; `lastOrderAt` toma el máximo de todos los pedidos, incluidos los cancelados.
- No se cambió cálculo financiero, monedas, reembolsos ni Mercado Pago. La diferencia entre conteo/última fecha y suma es comportamiento existente, no una corrección financiera implícita.
- La tienda se obtiene mediante `StoreContextService` / `GetMyStoreService` a partir del principal. El endpoint no acepta una tienda alternativa.

## Pruebas y ejecución

Pruebas añadidas:

- `StoreCustomerRepositoryAdapterTest`: proyección Spring real, dos fechas con microsegundos, nulos y defaults, búsqueda recortada, montos y conteos.
- `MerchantCustomerProjectionHttpTest`: proyección Spring -> adaptador -> servicio -> controlador con seguridad de métodos -> mapper -> JSON. Solo IO de repositorios simulado. HTTP 200, nueve campos, fechas precisas, último pedido nulo, conteos/importes, tienda del principal, rechazo CUSTOMER y sin autenticación, y 404 del merchant sin tienda. El código HTTP vigente de este último es `RESOURCE_NOT_FOUND`.
- `StoreCustomerPostgresIntegrationTest`: clúster nuevo en `@TempDir`, puerto local efímero y cierre al terminar. No lee `.env`, perfiles, URLs externas ni clústeres existentes. Tablas mínimas con tipos SQL de producción; no valida todas las migraciones. Comprueba materialización nativa de Hibernate y proyección JPA, fechas con offset equivalentes a UTC y microsegundos, cliente sin pedidos, cancelados, importes/conteos, búsquedas vacías/filtradas/sin coincidencias, clientes exclusivos/compartidos, roles ajenos, pedidos sin asociación y aislamiento entre tiendas.

Reproducción PostgreSQL fuera del sandbox, desde `backend`:

```bash
STORE_CUSTOMERS_TEST_PG_BIN=/ruta/postgresql/bin ./mvnw -o \
  -Dtest=StoreCustomerPostgresIntegrationTest test
```

Requiere `initdb` y `postgres` de la misma instalación, con bibliotecas disponibles y un usuario distinto de root. Si no están cacheadas las dependencias, omitir `-o`. Sin la variable, los tres casos PostgreSQL quedan omitidos explícitamente.

En este sandbox se inicializó un clúster PostgreSQL 18.6 nuevo en `/tmp/jaldi-customers-pg-probe/data`. Al arrancarlo en loopback, PostgreSQL terminó con `could not create IPv4 socket ... Operation not permitted` y `FATAL: could not create any TCP/IP sockets`. **La validación JDBC/JPA sobre PostgreSQL real queda pendiente.** No se conectó a Neon ni Render ni se alteraron datos persistentes.

La ejecución histórica en un entorno restringido necesitó un agente Mockito local. Ese ajuste no forma parte del flujo del equipo. Comando equivalente para un JDK 21 normal:

```bash
./mvnw -o \
  -Dtest='*Test,!*IntegrationTest,!InventoryJpaRepositorySpringDataTest' verify
```

Este comando ejecuta la suite disponible sin PostgreSQL; excluye las clases de integración y la prueba Spring Data de inventario que requiere una base real. `BackendApplicationTests` no está incluida en el patrón `*Test`; es una aserción simple y se ejecutó por separado, sin levantar contexto ni base de datos. Los runners manuales con condiciones propias permanecen desactivados. Los resultados finales se detallan al final de este documento.

## Bruno: verificación posterior

Abrir esta carpeta como colección y seleccionar `Local disposable`. Solo contiene GET. Usar exclusivamente una API local con base desechable; completar los tokens e IDs en variables locales, sin guardar credenciales en el repositorio. No se ejecutaron estos casos contra un servidor durante esta corrección.

Preparar los siguientes datos en una base local desechable mediante fixtures de negocio; esta colección no los crea. El preparador QA fue retirado y el clúster de la prueba JPA no prepara la API utilizada por Bruno:

- Merchant A con tienda A; merchant B con tienda B; un CUSTOMER; un MERCHANT sin tienda.
- Carlos Garcia, email `carlos@example.test`, teléfono `987654321`, creado `2026-09-01T10:00:00.123456Z`, asociado como CUSTOMER a A y B.
- Ana Lopez, email `ana@example.test`, creada `2026-09-02T10:00:00.123456Z`, asociada a A, sin pedidos.
- Foreign Only, asociado como CUSTOMER únicamente a B. Sin más clientes asociados a A.
- Carlos en A: COMPLETED 10.25 el 10/09 a las 10:00Z, CONFIRMED 20.50 el 11/09 a las 10:00Z, CANCELLED 99.99 el `2026-09-15T15:30:00.654321Z`.
- Carlos en B: COMPLETED 900.00 el `2026-10-01T10:00:00Z`.

Completar `customerWithOrdersId`, `customerWithoutOrdersId`, `foreignCustomerId`, `storeBId` y los cuatro tokens. Los totales son fixtures, no cambios de reglas del negocio. La prueba JPA automatizada genera su propio conjunto equivalente, independiente de Bruno.

Casos incluidos:

1. Lista y fechas, contrato de nueve campos, montos y conteos.
2. Query vacía.
3. Query filtrada, mayúsculas y espacios.
4. Cliente sin pedidos, fecha de alta y último pedido nulo.
5. Sin coincidencias, array vacío.
6. Cliente exclusivo de B no visible en A.
7. Cliente compartido: B ve solamente sus pedidos/importes.
8. Intento de elegir `storeId` ajeno con token A conserva alcance de A.
9. Query de 101 caracteres: 400 `VALIDATION_ERROR`.
10. Sin token: 401.
11. CUSTOMER: 403.
12. MERCHANT sin tienda: 404 `RESOURCE_NOT_FOUND`.

Repetir el caso 1 tras los casos de error para comprobar recuperación.

## Resultados de esta ejecución

- Antes del parche: regresión con proyección Spring real reproduce exactamente la excepción productiva.
- Después del parche: ejecución focalizada de 77 casos, **74 aprobados, 3 omitidos** (los tres PostgreSQL), cero fallos y cero errores, `BUILD SUCCESS`. Incluye clientes, mapper, contexto de tienda, errores globales, JWT, autenticación y condiciones de schedulers. Log: `/tmp/jaldi-customers-final-focused.log`.
- `BackendApplicationTests`: 1 caso aprobado, `BUILD SUCCESS`, ejecutado por separado. Log: `/tmp/jaldi-customers-app-test.log`.
- Suite amplia: **1.344 casos, 1.322 aprobados, 20 errores, 2 omitidos**, cero fallos de aserción. Los 20 errores pertenecen a `QaInheritedBranchTest`, que se detiene con `QA blocked: missing authorization or unverified configuration/identity`. No se cambiaron esas pruebas ni la guardia de QA. `verify` se detuvo en tests y no llegó al empaquetado. Log: `/tmp/jaldi-customers-regressions.log`.
- Baseline de QA en copia temporal: restaurando únicamente las dos clases productivas del parche a su versión original de `HEAD`, `QaInheritedBranchTest` reproduce los mismos **20 errores entre 38 casos**. Confirma que esos errores son independientes del cambio de tipos. Log: `/tmp/jaldi-customers-qa-baseline.log`.
- PostgreSQL: `initdb` exitoso; arranque TCP rechazado por sandbox. JDBC/JPA real, agregados SQL e aislamiento SQL quedan pendientes de ejecutar fuera del sandbox. Logs: `/tmp/jaldi-customers-initdb.log`, `/tmp/jaldi-customers-postgres-probe.log`.
- Bruno: doce casos preparados, ejecución HTTP contra servidor pendiente.
- `git diff --check` en los archivos productivos del parche: correcto. El chequeo global detecta un salto de línea final preexistente en frontend; se conserva.
- No se modificó frontend, Mercado Pago, configuración productiva ni migraciones. No se hicieron commit, push, merge ni deploy.

Comando focalizado final:

```bash
./mvnw -o \
  -Dtest='StoreCustomerRepositoryAdapterTest,MerchantCustomerProjectionHttpTest,MerchantCustomerControllerTest,GetStoreCustomersServiceTest,StoreCustomerResponseMapperTest,StoreContextServiceTest,StoreCustomerPostgresIntegrationTest,GlobalExceptionHandlerTest,SecurityUserStatusWebTest,JwtAuthenticationFilterTest,RestAuthenticationEntryPointTest,PaymentSchedulerConditionsIntegrationTest' test
```
