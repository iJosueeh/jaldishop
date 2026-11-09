# Rate limiting y revisión SQL focalizada

## Alcance y revisión

No existía rate limiting o bloqueo de intentos local. AuthController ofrece login,
registro de clientes y comerciantes; login verifica BCrypt y emite JWT. Los
endpoints públicos están enumerados en SecurityConfig. El usuario confirmó Render Free, Docker, jaldishop-api.onrender.com y sin CDN
adicional. Free no admite más de una instancia según la documentación oficial.

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

server.forward-headers-strategy=none mantiene intacta la IP del socket.
Fuera del despliegue Render identificado por las variables de entorno RENDER=true
Y RENDER_EXTERNAL_HOSTNAME=jaldishop-api.onrender.com se ignoran todas las
cabeceras de IP, incluido CF-Connecting-IP. Host y cabeceras HTTP no activan el modo.

En ese despliegue Free público se usa exclusivamente CF-Connecting-IP, generado
por Cloudflare; no se interpreta XFF, Forwarded, True-Client-IP ni X-Real-IP.
Se valida un único literal IPv4/IPv6 sin DNS, puerto, lista ni zona IPv6 y se
normaliza para impedir contadores distintos por representaciones de una IP.
Ausencia, duplicados o formato inválido producen 503 CLIENT_IP_UNAVAILABLE sólo
en las rutas limitadas: nunca se retorna al contador compartido del proxy.
Webhook y pagos no invocan el resolver.

Fundamento y frontera de confianza (revisión 2026-11-09):
- [Render: DDoS](https://render.com/articles/how-render-handles-ddos-attacks)
  indica que todo ingreso público atraviesa Cloudflare y balanceadores Render.
  Recomienda XFF pero no documenta saneamiento ni cantidad estable de saltos.
- [Render Free](https://render.com/docs/free) no permite escalar más allá de una
  instancia ni recibir conexiones privadas. El usuario confirma Free y ausencia
  de CDN adicional. No se consultó el dashboard.
- [Cloudflare: cabeceras](https://developers.cloudflare.com/fundamentals/reference/http-headers/)
  documenta CF-Connecting-IP como IP del visitante generada hacia el origen;
  XFF puede conservar prefijos aportados por el cliente.
- [Render: variables](https://render.com/docs/environment-variables) documenta
  RENDER y RENDER_EXTERNAL_HOSTNAME, también disponibles con Docker.

La selección de CF-Connecting-IP es una inferencia de esos contratos, no una
validación del despliegue activo: Render no documenta expresamente la conservación
de esta cabecera hasta el contenedor. Si la elimina, las rutas protegidas devolverán
503, sin aceptar una identidad insegura. Antes de desplegar comprobar su entrega
y saneamiento mediante soporte Render o una verificación controlada de staging.
No se añadió endpoint de diagnóstico ni se hicieron solicitudes al servicio público.
Cambiar a plan pagado (con ingreso privado), agregar CDN o cambiar la ruta de
entrada exige revisar esta frontera antes de desplegar. RENDER no autentica por
sí solo una solicitud; la seguridad depende del ingreso público exclusivo descrito.

## Verificación local

Compilación y package con skipTests; suite focalizada ClientIpResolverTest, RateLimitsTest,
AuthControllerTest, JwtAuthenticationFilterTest, SecurityUserStatusWebTest y
StoreSearchQueryTest. Mockito requiere -javaagent con su jar local en este
entorno para evitar el bloqueo de auto-adjunción. Las pruebas nuevas cubren
429, Retry-After, normalización/independencia de cuentas, IP falsificada,
expiración, capacidad, concurrencia, registro compartido y exclusión del webhook.
Resultado de esta revisión: 53 pruebas focalizadas pasaron (12 del resolver, 6 de
rate limiting y 35 existentes), sin fallos ni errores; package y diff --check
pasaron. Se intentó StoreSearchPostgresIntegrationTest con PostgreSQL 18.6 local
y un clúster temporal: initdb funcionó, pero el sandbox impide abrir el socket
local (java.net.SocketException: Operation not permitted). La prueba no alcanzó
las consultas SQL. No se accedió a bases externas.

## Referencias Git

bf21bba se conserva como ancestro. origin/main local apunta a
c6d6528755118232d40bce4b5b40249fd1f0cfde y está contenido en la rama.
Los intentos de git fetch origin/main fallaron por resolución DNS de github.com;
no se puede afirmar sincronización con el main remoto actual ni integrar commits
remotos desconocidos. No hubo rebase, merge, push ni cambios de infraestructura.
