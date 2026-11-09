# Rate limiting y revisión SQL focalizada

## Alcance y revisión

No existía rate limiting o bloqueo de intentos local. AuthController ofrece login,
registro de clientes y comerciantes; login verifica BCrypt y emite JWT. Los
endpoints públicos están enumerados en SecurityConfig. No se pudo confirmar el
número de instancias ni la cadena de proxies del despliegue Render desde el repo.

Revisión SQL: las consultas @Query JPQL/nativas de los repositorios usan parámetros
nombrados o SQL constante. Los usos JdbcTemplate en CheckoutCreationRequestStore,
ManualOrderInvestigationService y PaymentWebhookPersistenceService usan `?` con
argumentos separados. No se encontraron createNativeQuery/createQuery ni SQL
construido con entradas externas en producción. Las concatenaciones de @Query
son constantes; concatenaciones MANUAL_/WEBHOOK_ son valores parametrizados.
No se identificaron consultas vulnerables ni casos pendientes por construcción
SQL dinámica. No se reescribieron repositorios.

StoreJpaRepository usa parámetros y LOCATE con public.unaccent: %, _ y barra
inversa siguen siendo literales. StoreSearchPostgresIntegrationTest ya cubre
acentos, comodines y `' OR 1=1 --`. Las páginas usan PageRequest y no interpolan
identificadores u ordenamiento SQL externos. La búsqueda pública valida limit.

## Implementación

Ventanas fijas desde la primera solicitud, sincronizadas, en memoria JVM:

| Política | Ruta | Valor inicial |
|---|---|---|
| login-account | POST /api/v1/auth/login | 5 / 60 s por email normalizado |
| login-ip | POST /api/v1/auth/login | 30 / 60 s por IP |
| register-ip | POST /api/v1/auth/register y /register/merchant | 3 / 600 s compartidos por IP |
| search-ip | GET /api/v1/stores/search y /featured | 60 / 60 s por IP |

Se cuentan solicitudes, incluso logins exitosos. No hay bloqueo permanente.
No se aplica límite general autenticado: no hay necesidad demostrada y podría
interferir con compras. Webhooks, checkout y conciliación quedan fuera.
429 conserva ApiError (code RATE_LIMIT_EXCEEDED, message, errors) y Retry-After
expresa segundos enteros hasta la expiración. No se añaden logs de credenciales.

Bucket4j es compatible con Java moderno, pero aún requeriría gestionar claves,
expiración y capacidad. Para estas cuatro políticas se eligió una ventana fija
sin dependencia adicional. Puede aceptar ráfagas en bordes de ventanas.

## Configuración y operación

En application.properties: app.security.rate-limit.<política>.limit y
.window-seconds. Variables RATE_LIMIT_LOGIN_ACCOUNT_LIMIT,
RATE_LIMIT_LOGIN_ACCOUNT_WINDOW_SECONDS, RATE_LIMIT_LOGIN_IP_LIMIT,
RATE_LIMIT_LOGIN_IP_WINDOW_SECONDS, RATE_LIMIT_REGISTER_IP_LIMIT,
RATE_LIMIT_REGISTER_IP_WINDOW_SECONDS, RATE_LIMIT_SEARCH_IP_LIMIT,
RATE_LIMIT_SEARCH_IP_WINDOW_SECONDS y RATE_LIMIT_MAX_ENTRIES.

Máximo inicial: 10000 claves entre todas las políticas. Al recibir solicitudes
se eliminan ventanas expiradas. En inactividad pueden quedar claves expiradas,
siempre acotadas. Al llenarse se rechazan nuevas claves hasta la primera
expiración; no se expulsan contadores activos que permitirían evadir límites.
Cada instancia tiene estado propio; reinicios, despliegues y cold starts lo
reinician. Antes de escalar a varias instancias reevaluar almacenamiento compartido.

server.forward-headers-strategy=none evita interpretar cabeceras arbitrarias.
Sólo se usa getRemoteAddr; X-Forwarded-For y Forwarded no se interpretan aquí.
En Render puede ser la IP del proxy: varios visitantes compartirían límites.
Antes de producción confirmar instancias y cadena de proxies y configurar una
resolución basada en proxies confiables; no habilitar lectura del primer XFF ni
confiar globalmente en redes privadas sin verificar el acceso al origen.
No se cambió infraestructura ni se probó tráfico contra servicios externos.

## Verificación local

Compilación y package con skipTests; suite focalizada RateLimitsTest,
AuthControllerTest, JwtAuthenticationFilterTest, SecurityUserStatusWebTest y
StoreSearchQueryTest. Mockito requiere -javaagent con su jar local en este
entorno para evitar el bloqueo de auto-adjunción. Las pruebas nuevas cubren
429, Retry-After, normalización/independencia de cuentas, IP falsificada,
expiración, capacidad, concurrencia, registro compartido y exclusión del webhook.
La integración PostgreSQL requiere STORE_SEARCH_TEST_PG_BIN con un PostgreSQL
local ejecutable; nunca usar URLs Render/Neon. No se ejecutó aquí: no hay una
instalación local funcional disponible.
