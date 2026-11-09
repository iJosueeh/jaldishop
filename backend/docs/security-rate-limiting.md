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

### Identificación de IP: pendiente de habilitación en Render

**No está lista para producción.** El commit 0998718 activaba automáticamente
CF-Connecting-IP basándose en variables Render. Esa detección no autentica una
cabecera ni demuestra su conservación y saneamiento hasta el contenedor.

| Cabecera | Contrato documentado | Garantía que falta para JaldiShop |
|---|---|---|
| X-Forwarded-For | Render la recomienda. Cloudflare agrega la IP de quien conecta si ya había valores; puede conservar prefijos externos. | Cadena de proxies confiables, tratamiento de duplicados y valores aportados por el cliente; no hay cantidad fija de saltos documentada en las fuentes revisadas. |
| CF-Connecting-IP | Cloudflare entrega un valor al origen; lo recomienda por su formato de una IP. | Render no garantiza expresamente su conservación y saneamiento hasta el contenedor en las fuentes revisadas. No basta que sea un literal válido. |

Fuentes oficiales revisadas el 2026-11-09:
- [Render: DDoS](https://render.com/articles/how-render-handles-ddos-attacks):
  todo ingreso público atraviesa Cloudflare y balanceadores Render; recomienda
  XFF. No describe una política completa de confianza para estas cabeceras.
- [Render Free](https://render.com/docs/free): una instancia, sin ingreso privado.
  Reduce rutas de entrada pero no autentica una cabecera HTTP.
- [Cloudflare: cabeceras](https://developers.cloudflare.com/fundamentals/reference/http-headers/):
  XFF puede conservar valores previos. CF-Connecting-IP tiene excepciones para
  Workers: misma zona puede reflejar x-real-ip modificado por un Worker; entre
  zonas usa una IP de Worker fija. Transformaciones pueden retirar cabeceras y
  Pseudo IPv4 puede sustituir valores. No asumir conocer la configuración de Render.

No se cambia a XFF ni se toma la primera IP. Una futura resolución XFF requerirá
identificar al peer confiable y recorrer la cadena desde la derecha, descartando
sólo proxies explícitamente verificados; no confiar en todas las redes privadas,
ni fijar un número de saltos por observación de una sola solicitud.

### Comportamiento actual

server.forward-headers-strategy=none se mantiene.
- Local (RENDER distinto de true): límites por IP activos usando getRemoteAddr,
  ignorando CF-Connecting-IP, XFF, Forwarded y demás cabeceras de IP.
- Render (RENDER=true): políticas por IP **inactivas por defecto** mientras la
  verificación esté pendiente. No hay fallback a la IP del proxy, ni 503 por
  ausencia de cabecera en este modo. El límite login-account sigue activo.
  Registro y búsqueda quedan sin rate limiting por IP durante esta espera:
  es una protección incompleta, no un modo aprobado para producción.
- RATE_LIMIT_RENDER_IP_VERIFIED=false es el valor inicial, enlazado a
  app.security.rate-limit.render-ip-verified. Sólo después de verificar la frontera
  completa se podrá activar explícitamente. Esta marca es una declaración del
  operador, **no una validación automática**. Una cabecera HTTP no puede activarla.
- El candidato optativo CF-Connecting-IP se restringe al hostname de despliegue
  jaldishop-api.onrender.com en la variable de entorno, nunca al HTTP Host.
  Conserva validación de un único literal y normalización IPv4/IPv6, sin DNS.
  Las pruebas de este parser no demuestran autenticidad en Render.

Con el candidato activado, cabeceras ausentes, duplicadas o inválidas aún dan
503 CLIENT_IP_UNAVAILABLE sólo en rutas limitadas. Sí: si Render omite la cabecera
en todas las solicitudes, inutiliza login, ambos registros, búsqueda y destacados.
No afecta todas las rutas públicas. El modo pendiente evita ese fallo por defecto.
No se cambian los valores de límites ni se modifica el flujo de pagos.

### Verificación requerida antes de habilitar

1. Obtener confirmación de Render de la ruta completa hacia el contenedor Free,
   sin acceso alternativo que permita saltarse el saneamiento, y del contrato
   de sustitución/conservación de CF-Connecting-IP. Confirmar Workers,
   transformaciones, duplicados y comportamiento IPv6/Pseudo IPv4. Si Render sólo
   respalda XFF, obtener su cadena de confianza y implementar esa política antes
   de habilitar nada; esta marca no selecciona XFF.
2. En staging controlado del mismo plan/dominio de ingreso, comprobar solicitudes
   desde dos IP conocidas, sin cabeceras y con CF-Connecting-IP, XFF y Forwarded
   falsificados, duplicados y combinados. Debe observarse la identidad saneada,
   independiente de lo enviado por el cliente, y contadores separados por origen.
   Usar observación temporal sin tokens/contraseñas ni endpoint público permanente;
   retirar la instrumentación. No son pruebas de carga.
3. Verificar presencia consistente de la cabecera en solicitudes normales antes
   de aceptar el comportamiento 503 del modo activado. Si no está garantizada,
   mantener desactivado y resolver el contrato con Render. Unas pruebas puntuales
   no sustituyen la garantía de rutas y saneamiento del proveedor.
4. Registrar evidencia y política elegida; revisar de nuevo antes de cambiar
   plan, dominio, CDN, Workers o ingreso privado. No habilitar por el solo hecho
   de que RENDER=true o de que una cabecera tenga formato correcto.

No se hicieron solicitudes al servicio desplegado ni se modificó infraestructura.

## Verificación local

Revisión exclusiva de IP posterior a 0998718: 25 pruebas pasaron, sin fallos,
errores ni omitidas: ClientIpResolverTest (14), RateLimitsTest (6) y
AuthControllerTest (5). Incluyen modo Render pendiente por defecto, cabeceras HTTP
incapaces de habilitarlo, ausencia de 503 en modo pendiente, límite por cuenta
activo, límites locales activos e impacto 503 del candidato habilitado.
Se compiló mediante Maven test y git diff --check pasó. No se ejecutaron SQL,
bases externas ni pruebas contra Render en esta revisión.


Compilación y package con skipTests; suite focalizada ClientIpResolverTest, RateLimitsTest,
AuthControllerTest, JwtAuthenticationFilterTest, SecurityUserStatusWebTest y
StoreSearchQueryTest. Mockito requiere -javaagent con su jar local en este
entorno para evitar el bloqueo de auto-adjunción. Las pruebas nuevas cubren
429, Retry-After, normalización/independencia de cuentas, IP falsificada,
expiración, capacidad, concurrencia, registro compartido y exclusión del webhook.
Resultado previo (0998718): 53 pruebas focalizadas pasaron (12 del resolver, 6 de
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
